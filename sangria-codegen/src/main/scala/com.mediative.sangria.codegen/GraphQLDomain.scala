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

import scala.annotation.compileTimeOnly
import scala.annotation.StaticAnnotation
import scala.meta._

@compileTimeOnly("@GraphQLDomain not expanded")
class GraphQLDomain(operations: String, schema: String) extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    val (schemaPath, operationsPath) = this match {
      case q"new $_(${Lit.String(schemaPath)}, ${Lit.String(operationsPath)})" =>
        (schemaPath, operationsPath)
      case _ =>
        abort("@GraphQLDomain requires a schema path and operations path")
    }

    defn match {
      case q"object $tname { ..$stats }" =>
        val client = SangriaCodegen(schemaPath, operationsPath).generate()
        q"""
          object $tname {
            ..$client
            ..$stats
          }
        """

      case _ =>
        abort("@GraphQLDomain must annotate an object.")
    }
  }
}
