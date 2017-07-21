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

import sangria.validation.TypeInfo
import sangria.schema._
import sangria.ast

case class Importer(schema: Schema[_, _], document: ast.Document) {
  private val typeInfo    = new TypeInfo(schema)
  private val outputTypes = scala.collection.mutable.Set[OutputType[_]]()

  def parse(): Result[Tree.Api] =
    Right(
      Tree.Api(
        document.operations.values.map(generateOperation).toVector,
        document.fragments.values.toVector.map(generateFragment),
        schema.outputTypes.values
          .filter(outputTypes)
          .map(generateType)
          .flatten
          .toVector
      ))

  def isObjectLike(tpe: sangria.schema.Type): Boolean = tpe match {
    case OptionType(wrapped)       => isObjectLike(wrapped)
    case ListType(wrapped)         => isObjectLike(wrapped)
    case obj: ObjectLikeType[_, _] => true
    case _                         => false
  }

  def getScalaType(outputName: Option[String], tpe: Type): String = tpe match {
    case OptionType(wrapped) =>
      s"Option[${getScalaType(outputName, wrapped)}]"
    case scalar: ScalarType[_] =>
      scalar.name
    case ListType(wrapped) =>
      s"List[${getScalaType(outputName, wrapped)}]"
    case outputType: OutputType[_] =>
      outputTypes += outputType
      outputName.getOrElse(outputType.namedType.name)
    case t: Type =>
      outputName.getOrElse(t.namedType.name)
  }

  def generateSelections(selections: Seq[ast.Selection]): Tree.Selection =
    selections.map(generateSelection).foldLeft(Tree.Selection.empty)(_ + _)

  def generateSelection(node: ast.AstNode): Tree.Selection = {
    typeInfo.enter(node)
    val result = node match {
      case field: ast.Field =>
        typeInfo.tpe match {
          case Some(tpe) if !isObjectLike(tpe) =>
            val name = getScalaType(None, tpe)
            Tree.Selection(Vector(Tree.Field(field.outputName, Tree.Ref(name))))

          case _ =>
            val name = field.outputName
            val gen  = generateSelections(field.selections)
            Tree.Selection(Vector(Tree.Field(name, gen)))
        }

      case fragmentSpread: ast.FragmentSpread =>
        val name     = fragmentSpread.name
        val fragment = document.fragments(fragmentSpread.name)
        generateSelections(fragment.selections).copy(interfaces = Vector(name))

      case inlineFragment: ast.InlineFragment =>
        generateSelections(inlineFragment.selections)

      case unknown =>
        sys.error("Unknown selection: " + unknown.toString)
    }
    typeInfo.leave(node)
    result
  }

  def generateOperation(operation: ast.OperationDefinition): Tree.Operation = {
    val variables = operation.variables.toVector.map { varDef =>
      val tpe = schema.getOutputType(varDef.tpe, topLevel = true).get
      Tree.Field(varDef.name, Tree.Ref(getScalaType(None, tpe)))
    }

    typeInfo.enter(operation)
    val selection = generateSelections(operation.selections)
    typeInfo.leave(operation)
    Tree.Operation(operation.name, variables, selection)
  }

  def generateFragment(fragment: ast.FragmentDefinition): Tree.Interface = {
    typeInfo.enter(fragment)
    val selection = generateSelections(fragment.selections)
    typeInfo.leave(fragment)
    val fields = selection.fields.collect {
      case tree @ Tree.Field(name, Tree.Ref(tpe)) => Tree.Field(name, Tree.Ref(tpe))
      case tree @ Tree.Field(name, _)             => Tree.Field(name, Tree.Ref(name.capitalize))
    }
    Tree.Interface(fragment.name, fields)
  }

  def generateType[T](tpe: OutputType[T]): Option[Tree.Type] = tpe match {
    case interface: InterfaceType[_, _] =>
      val fields = interface.uniqueFields.map { field =>
        val tpe = getScalaType(Some(field.fieldType.namedType.name), field.fieldType)
        Tree.Field(field.name, Tree.Ref(tpe))
      }
      Some(Tree.Interface(interface.name, fields))

    case obj: ObjectLikeType[_, _] =>
      val fields = obj.uniqueFields.map { field =>
        val tpe = getScalaType(None, field.fieldType)
        Tree.Field(field.name, Tree.Ref(tpe))
      }
      Some(Tree.Object(obj.name, fields))

    case enum: EnumType[_] =>
      val values = enum.values.map(_.name)
      Some(Tree.Enum(enum.name, values))

    case union: UnionType[_] =>
      None

    case ListType(_) | OptionType(_) | ScalarAlias(_, _, _) | ScalarType(_, _, _, _, _, _, _, _) =>
      None
  }
}
