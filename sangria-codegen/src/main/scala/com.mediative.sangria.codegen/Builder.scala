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
import sangria.parser.QueryParser
import sangria.schema._
import sangria.ast.Document

case class Builder private (
    schema: Result[Schema[_, _]],
    document: Result[Document] = Right(Document.emptyStub)
) {
  private def withQuery(query: Result[Document]): Builder = {
    val merged =
      document.right.flatMap { doc =>
        if (doc == Document.emptyStub) query
        else query.right.map(doc.merge)
      }
    copy(document = merged)
  }

  def withQuery(query: Document): Builder =
    withQuery(Right(query))

  def withQuery(queryFile: File): Builder =
    withQuery(Builder.parseDocument(queryFile))

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
    val schema = parseDocument(schemaFile).map(Schema.buildFromAst)
    new Builder(schema)
  }

  private def parseDocument(file: File): Result[Document] =
    for {
      input <- Either.catchNonFatal(Source.fromFile(file).mkString).leftMap { error =>
        Failure(s"Failed to read $file: ${error.getMessage}")
      }
      document <- Either.fromTry(QueryParser.parse(input)).leftMap { error =>
        Failure(s"Failed to parse $file: ${error.getMessage}")
      }
    } yield document
}
