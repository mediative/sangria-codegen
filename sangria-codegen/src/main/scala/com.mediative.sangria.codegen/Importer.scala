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
  private val typeInfo = new TypeInfo(schema)
  private val types    = scala.collection.mutable.Set[Type]()

  def parse(): Result[Tree.Api] =
    Right(
      Tree.Api(
        document.operations.values.map(generateOperation).toVector,
        document.fragments.values.toVector.map(generateFragment),
        schema.typeList
          .filter(types)
          .map(generateType)
          .flatten
          .toVector
      ))

  def touchType(tpe: Type): Unit = tpe match {
    case OptionType(wrapped) =>
      touchType(wrapped)
    case OptionInputType(wrapped) =>
      touchType(wrapped)
    case ListType(wrapped) =>
      touchType(wrapped)
    case ListInputType(wrapped) =>
      touchType(wrapped)
    case IDType =>
      types += tpe
      ()
    case scalar: ScalarType[_] =>
      // Nothing
      ()
    case underlying @ (_: OutputType[_] | _: InputObjectType[_]) =>
      types += underlying
      ()
  }

  def isObjectLike(tpe: sangria.schema.Type): Boolean = tpe match {
    case OptionType(wrapped)       => isObjectLike(wrapped)
    case ListType(wrapped)         => isObjectLike(wrapped)
    case obj: ObjectLikeType[_, _] => true
    case _                         => false
  }

  def generateField(
      touch: Boolean,
      name: String,
      tpe: Type,
      selection: Option[Tree.Selection] = None): Tree.Field = {
    if (touch)
      touchType(tpe)
    Tree.Field(name, tpe, selection)
  }

  def generateSelections(selections: Seq[ast.Selection]): Tree.Selection =
    selections.map(generateSelection).foldLeft(Tree.Selection.empty)(_ + _)

  def generateSelection(node: ast.AstNode): Tree.Selection = {
    typeInfo.enter(node)
    val result = node match {
      case field: ast.Field =>
        typeInfo.tpe match {
          case Some(tpe) if !isObjectLike(tpe) =>
            Tree.Selection(Vector(generateField(touch = true, field.outputName, tpe)))

          case Some(tpe) =>
            val gen = generateSelections(field.selections)
            Tree.Selection(Vector(generateField(touch = false, field.outputName, tpe, Some(gen))))

          case _ =>
            sys.error("Field without type: " + field)
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
    typeInfo.enter(operation)
    val variables = operation.variables.toVector.map { varDef =>
      schema.getInputType(varDef.tpe) match {
        case Some(tpe) =>
          generateField(touch = true, varDef.name, tpe)
        case None =>
          sys.error("Unknown input type: " + varDef.tpe)
      }
    }

    val selection = generateSelections(operation.selections)
    typeInfo.leave(operation)
    Tree.Operation(operation.name, variables, selection)
  }

  def generateFragment(fragment: ast.FragmentDefinition): Tree.Interface = {
    typeInfo.enter(fragment)
    val selection = generateSelections(fragment.selections)
    typeInfo.leave(fragment)
    Tree.Interface(fragment.name, selection.fields)
  }

  def generateType(tpe: Type): Option[Tree.Type] = tpe match {
    case interface: InterfaceType[_, _] =>
      val fields = interface.uniqueFields.map { field =>
        generateField(touch = true, field.name, field.fieldType)
      }
      Some(Tree.Interface(interface.name, fields))

    case obj: ObjectLikeType[_, _] =>
      val fields = obj.uniqueFields.map { field =>
        generateField(touch = true, field.name, field.fieldType)
      }
      Some(Tree.Object(obj.name, fields))

    case enum: EnumType[_] =>
      val values = enum.values.map(_.name)
      Some(Tree.Enum(enum.name, values))

    case union: UnionType[_] =>
      None

    case inputObj: InputObjectType[_] =>
      val fields = inputObj.fields.map { field =>
        generateField(touch = true, field.name, field.fieldType)
      }
      Some(Tree.Object(inputObj.name, fields))

    case IDType =>
      Some(Tree.TypeAlias("ID", "_root_.scala.String"))

    case ListInputType(_) | OptionInputType(_) =>
      None

    case ListType(_) | OptionType(_) | ScalarAlias(_, _, _) | ScalarType(_, _, _, _, _, _, _, _) =>
      None
  }
}
