/*
 * Copyright 2017 Mediative
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediative.sangria.codegen

import scala.meta.{io => _, _}

import sangria.parser.QueryParser
import sangria.schema.Schema
import scala.util.{Failure, Success}
import scala.io.Source
import sangria.marshalling.circe._
import sangria.validation.TypeInfo

import sangria.ast
import io.circe.Json
import sangria.schema._

object SangriaCodegen {
  case class Generated(stats: Vector[Stat], params: Vector[Term.Param]) {
    def +(that: Generated) =
      Generated(this.stats ++ that.stats, this.params ++ that.params)
  }
  object Generated {
    def apply(param: Term.Param*): Generated = Generated(Vector.empty, Vector(param: _*))
    def apply(stats: Vector[Stat], param: Term.Param*): Generated =
      Generated(stats, Vector(param: _*))
    def apply(stat: Stat, param: Term.Param*): Generated =
      Generated(Vector(stat), Vector(param: _*))
  }

  def apply(schemaPath: String, operationsPath: String): SangriaCodegen = {
    val introspection: Json =
      _root_.io.circe.jawn.parseFile(new java.io.File(schemaPath)) match {
        case Right(json) => json
        case Left(error) => abort(s"@GraphQLDomain failed to parse schema: $error")
      }
    val schema: Schema[Unit, Json] = Schema
      .buildFromIntrospection[Unit, Json](
        introspection,
        new DefaultIntrospectionSchemaBuilder[Unit])
      .asInstanceOf[sangria.schema.Schema[Unit, Json]]

    // Parse GraphQl query
    val document: ast.Document =
      QueryParser.parse(Source.fromFile(operationsPath).mkString) match {
        case Success(api) => api
        case Failure(err) => abort(s"@GraphQLDomain failed to part the API: ${err.getMessage}")
      }

    SangriaCodegen(schema, document)
  }
}

case class SangriaCodegen(schema: Schema[_, _], document: ast.Document) {
  import SangriaCodegen._
  private val typeInfo = new TypeInfo(schema)

  def generate(): Vector[Stat] = {
    val Generated(operations, _) =
      document.operations.values.map(generateSelection("")).reduce(_ + _)
    val fragments = document.fragments.values.toVector.map(generateFragmentTrait(schema))

    val types = schema.outputTypes
      .filter(t => !Schema.isBuiltInType(t._2.name))
      .filter(_._2 != schema.query)
      .map(_._2)
      .flatMap(generateType)

    Vector(operations, fragments, types).flatten
  }

  def termParam(paramName: String, typeName: String) =
    Term.Param(Vector.empty, Term.Name(paramName), Some(Type.Name(typeName)), None)

  def isObjectLike(tpe: sangria.schema.Type): Boolean = tpe match {
    case OptionType(wrapped)       => isObjectLike(wrapped)
    case ListType(wrapped)         => isObjectLike(wrapped)
    case obj: ObjectLikeType[_, _] => true
    case _                         => false
  }

  def getScalaType(outputName: Option[String], tpe: sangria.schema.Type): String = tpe match {
    case OptionType(wrapped) =>
      s"Option[${getScalaType(outputName, wrapped)}]"
    case ot: ObjectType[_, _] =>
      outputName.getOrElse(ot.name)
    case scalar: ScalarType[_] =>
      scalar.name
    case ListType(wrapped) =>
      s"List[${getScalaType(outputName, wrapped)}]"
    case it: InterfaceType[_, _] =>
      it.name
    case et: EnumType[_] =>
      et.name
    case unknown =>
      abort(s"Unknown type $unknown")
  }

  def generateSelection(prefix: String)(node: ast.AstNode): Generated = {
    typeInfo.enter(node)
    val result = node match {
      case field: ast.Field =>
        typeInfo.tpe match {
          case Some(tpe) if !isObjectLike(tpe) =>
            val name = getScalaType(None, tpe)
            Generated(termParam(field.outputName, name))

          case _ =>
            val name = field.outputName.capitalize
            val Generated(stats, params) =
              field.selections.map(generateSelection(prefix + name + ".")).reduce(_ + _)

            val tpeName  = Type.Name(name)
            val termName = Term.Name(name)
            Generated(q"case class $tpeName(..$params)") +
              Generated(
                Option(stats)
                  .filter(_.nonEmpty)
                  .map { _ =>
                    q"object $termName { ..$stats }"
                  }
                  .toVector
              ) +
              Generated(termParam(field.outputName, prefix + name))
        }

      case fragmentSpread: ast.FragmentSpread =>
        val name     = fragmentSpread.name
        val fragment = document.fragments(fragmentSpread.name)
        // FIXME: Add notion of interface
        fragment.selections.map(generateSelection(name + ".")).reduce(_ + _)

      case operation: ast.OperationDefinition =>
        val variables = operation.variables.toVector.map { varDef =>
          val tpe = schema.getOutputType(varDef.tpe, topLevel = true).get
          termParam(varDef.name, getScalaType(None, tpe))
        }

        val name = operation.name.getOrElse(abort("@GraphQLDomain found unnamed operation"))
        val Generated(stats, params) =
          operation.selections.map(generateSelection(name + ".")).reduce(_ + _)

        val tpeName          = Type.Name(name)
        val termName         = Term.Name(name)
        val variableTypeName = Type.Name(name + "Variables")

        val defs = Vector[Stat](
          q"case class $tpeName(..$params)",
          q"""
            object $termName {
              case class $variableTypeName(..$variables)
              ..$stats
            }
          """
        )
        Generated(defs)

      case x => abort(x.toString)
    }
    typeInfo.leave(node)
    result
  }

  def generateFragmentTrait(schema: Schema[_, _])(fragment: ast.FragmentDefinition) = {
    val outputType                      = schema.getOutputType(fragment.typeCondition, topLevel = true)
    val Some(obj: ObjectLikeType[_, _]) = outputType
    val defs = obj.uniqueFields.map { field =>
      val tpe       = getScalaType(Some(field.fieldType.namedType.name), field.fieldType)
      val fieldName = Term.Name(field.name)
      val tpeName   = Type.Name(tpe)
      q"def $fieldName: $tpeName"
    }
    val traitName = Type.Name(fragment.name)
    q"trait $traitName { ..$defs }": Stat

  }

  def generateType(tpe: sangria.schema.Type): Vector[Stat] = tpe match {
    case interface: InterfaceType[_, _] =>
      val defs = interface.uniqueFields.map { field =>
        val tpe       = getScalaType(Some(field.fieldType.namedType.name), field.fieldType)
        val fieldName = Term.Name(field.name)
        val tpeName   = Type.Name(tpe)
        q"def $fieldName: $tpeName"
      }
      val traitName = Type.Name(interface.name)
      Vector(q"trait $traitName { ..$defs }": Stat)

    case obj: ObjectLikeType[_, _] =>
      val params = obj.uniqueFields.map { field =>
        val tpe = getScalaType(Some(field.name), field.fieldType)
        termParam(field.name, tpe)
      }
      val className = Type.Name(obj.name)
      // FIXME: val interfaces = obj.interfaces.
      Vector(q"case class $className(..$params)": Stat)

    case enum: EnumType[_] =>
      val enumName   = Type.Name(enum.name)
      val objectName = Term.Name(enum.name)
      val enumValues = enum.values.map { value =>
        val ctorName  = Ctor.Ref.Name(enum.name)
        val parent    = ctor"$ctorName"
        val template  = template"$parent"
        val valueName = Term.Name(value.name)
        q"case object $valueName extends $template"
      }
      Vector[Stat](
        q"sealed trait $enumName",
        q"object $objectName { ..$enumValues }"
      )

    case x => abort("Unknown output type " + x)
  }

}
