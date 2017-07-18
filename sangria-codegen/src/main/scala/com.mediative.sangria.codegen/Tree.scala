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

/**
 * AST representing the extracted GraphQL types.
 */
sealed trait Tree
object Tree {
  sealed trait OutputType extends Tree {
    def name: String
  }
  case class Ref(name: String)                             extends Tree
  case class Field[T <: Tree](name: String, tpe: T)        extends Tree
  case class Object(name: String, fields: Seq[Field[Ref]]) extends OutputType
  case class Selection(fields: Seq[Field[Tree]]) extends Tree {
    def +(that: Selection) =
      Selection(this.fields ++ that.fields)
  }
  case class Interface(name: String, fields: Seq[Field[Ref]]) extends OutputType
  case class Enum(name: String, vaules: Seq[String])          extends OutputType
  case class Operation(name: Option[String], variables: Seq[Field[Ref]], selection: Selection)
      extends Tree
  case class Api(operations: Seq[Operation], interfaces: Seq[Interface], types: Seq[OutputType])
}
