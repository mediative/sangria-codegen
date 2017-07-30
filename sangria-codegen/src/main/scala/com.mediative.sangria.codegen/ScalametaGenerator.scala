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

import scala.collection.immutable.Seq
import scala.meta._
import sangria.schema
import cats.implicits._

/**
 * Generate code using Scalameta.
 */
case class ScalametaGenerator(moduleName: Term.Name, stats: Seq[Stat] = Vector.empty)
    extends Generator[Defn.Object] {

  override def generate(api: Tree.Api): Result[Defn.Object] = {
    val operations = api.operations.flatMap(generateOperation)
    val fragments  = api.interfaces.map(generateInterface)
    for {
      types <- api.types.map(generateType).toList.sequenceU
    } yield {
      q"""
        object $moduleName {
          ..$operations
          ..$fragments
          ..${types.flatten}
          ..$stats
        }
      """
    }
  }

  def termParam(paramName: String, tpe: Type) =
    Term.Param(Vector.empty, Term.Name(paramName), Some(tpe), None)

  def generateTemplate(traits: Seq[String]): Template = {
    val ctorNames = traits.map(Ctor.Name.apply)
    val emptySelf = Term.Param(Vector.empty, Name.Anonymous(), None, None)
    Template(Nil, ctorNames, emptySelf, None)
  }

  def generateFieldType(field: Tree.Field)(genType: schema.Type => Type): Type = {
    def typeOf(tpe: schema.Type): Type = tpe match {
      case schema.OptionType(wrapped) =>
        t"Option[${typeOf(wrapped)}]"
      case schema.OptionInputType(wrapped) =>
        t"Option[${typeOf(wrapped)}]"
      case schema.ListType(wrapped) =>
        t"List[${typeOf(wrapped)}]"
      case schema.ListInputType(wrapped) =>
        t"List[${typeOf(wrapped)}]"
      case tpe: schema.Type =>
        genType(tpe)
    }
    typeOf(field.tpe)
  }

  def generateOperation(operation: Tree.Operation): Seq[Stat] = {
    def fieldType(field: Tree.Field, prefix: String = ""): Type =
      generateFieldType(field) { tpe =>
        if (field.isObjectLike)
          Type.Name(prefix + field.name.capitalize)
        else if (tpe == sangria.schema.IDType)
          Type.Name(moduleName.value + ".ID")
        else
          Type.Name(tpe.namedType.name)
      }

    def generateSelectionParams(prefix: String)(selection: Tree.Selection): Seq[Term.Param] =
      selection.fields.map { field =>
        val tpe = fieldType(field, prefix)
        termParam(field.name, tpe)
      }

    def generateSelectionStats(prefix: String)(selection: Tree.Selection): Seq[Stat] =
      selection.fields.flatMap {
        case Tree.Field(_, _, None) =>
          Vector.empty

        case Tree.Field(name, tpe, Some(selection)) =>
          val stats  = generateSelectionStats(prefix + name.capitalize + ".")(selection)
          val params = generateSelectionParams(prefix + name.capitalize + ".")(selection)

          val tpeName  = Type.Name(name.capitalize)
          val termName = Term.Name(name.capitalize)
          val template = generateTemplate(selection.interfaces)

          Vector(q"case class $tpeName(..$params) extends $template") ++
            Option(stats)
              .filter(_.nonEmpty)
              .map { stats =>
                q"object $termName { ..$stats }"
              }
              .toVector
      }

    val variables = operation.variables.map { varDef =>
      termParam(varDef.name, fieldType(varDef))
    }

    val name   = operation.name.getOrElse(sys.error("found unnamed operation"))
    val prefix = moduleName.value + "." + name + "."
    val stats  = generateSelectionStats(prefix)(operation.selection)
    val params = generateSelectionParams(prefix)(operation.selection)

    val tpeName          = Type.Name(name)
    val termName         = Term.Name(name)
    val variableTypeName = Type.Name(name + "Variables")

    Vector[Stat](
      q"case class $tpeName(..${params})",
      q"""
        object $termName {
          case class $variableTypeName(..$variables)
          ..${stats}
        }
      """
    )
  }

  def generateInterface(interface: Tree.Interface): Stat = {
    val defs = interface.fields.map { field =>
      val fieldName = Term.Name(field.name)
      val tpe = generateFieldType(field) { tpe =>
        field.selection.map(_.interfaces).filter(_.nonEmpty) match {
          case Some(interfaces) =>
            interfaces.map(x => Type.Name(x): Type).reduce(Type.With(_, _))
          case None =>
            Type.Name(tpe.namedType.name)
        }
      }
      q"def $fieldName: $tpe"
    }
    val traitName = Type.Name(interface.name)
    q"trait $traitName { ..$defs }"
  }

  def generateType(tree: Tree.Type): Result[Seq[Stat]] = tree match {
    case interface: Tree.Interface =>
      Right(Vector(generateInterface(interface)))

    case Tree.Object(name, fields) =>
      val params = fields.map { field =>
        val tpe = generateFieldType(field)(t => Type.Name(t.namedType.name))
        termParam(field.name, tpe)
      }
      val className = Type.Name(name)
      // FIXME: val interfaces = obj.interfaces.
      Right(Vector(q"case class $className(..$params)": Stat))

    case Tree.Enum(name, values) =>
      val enumValues = values.map { value =>
        val template  = generateTemplate(Seq(name))
        val valueName = Term.Name(value)
        q"case object $valueName extends $template"
      }

      val enumName   = Type.Name(name)
      val objectName = Term.Name(name)
      Right(
        Vector[Stat](
          q"sealed trait $enumName",
          q"object $objectName { ..$enumValues }"
        ))

    case Tree.TypeAlias(from, to) =>
      val alias      = Type.Name(from)
      val underlying = Type.Name(to)
      Right(Vector(q"type $alias = $underlying": Stat))

    case unknown =>
      Left(Failure("Unknown output type " + unknown))
  }

}

object ScalametaGenerator {
  def apply(moduleName: String): ScalametaGenerator =
    ScalametaGenerator(Term.Name(moduleName))
}
