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

import java.io.File
import scala.io.Source
import cats.syntax.either._
import io.circe.Json
import sangria.marshalling.circe._
import sangria.parser.QueryParser
import sangria.schema._
import sangria.ast

case class Builder private (
    schema: Result[Schema[_, _]],
    document: Result[ast.Document] = Right(ast.Document.emptyStub)
) {
  private def withQuery(query: Result[ast.Document]): Builder = {
    val merged =
      document.right.flatMap { doc =>
        if (doc == ast.Document.emptyStub) query
        else query.right.map(doc.merge)
      }
    copy(document = merged)
  }

  def withQuery(query: ast.Document): Builder =
    withQuery(Right(query))

  def withQuery(queryFile: File): Builder = {
    val query = for {
      query <- Either.catchNonFatal(Source.fromFile(queryFile).mkString).leftMap { error =>
        Failure(s"Failed to read query from $queryFile: ${error.getMessage}")
      }
      document <- Either.fromTry(QueryParser.parse(query)).leftMap { error =>
        Failure(s"Failed to parse query in $queryFile: ${error.getMessage}")
      }
    } yield document

    withQuery(query)
  }

  def generate[T](generator: Generator[T]): Result[T] =
    for {
      validSchema   <- schema
      validDocument <- document
      api           <- Importer(validSchema, validDocument).parse
      result        <- generator.generate(api)
    } yield result
}

object Builder {
  def apply(schema: Schema[_, _]): Builder = new Builder(Right(schema))

  def apply(schemaFile: File): Builder = {
    val schema = for {
      json <- io.circe.jawn.parseFile(schemaFile).leftMap { error =>
        Failure(s"Failed to parse schema in $schemaFile: $error")
      }

      schema <- Either
        .catchNonFatal {
          val builder = new DefaultIntrospectionSchemaBuilder[Unit]
          Schema.buildFromIntrospection[Unit, Json](json, builder)
        }
        .leftMap { error =>
          Failure(s"Failed to parse schema in $schemaFile: $error")
        }
    } yield schema
    new Builder(schema)
  }
}
