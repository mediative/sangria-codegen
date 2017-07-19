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
package cli

import java.io.{File, PrintStream}
import io.circe.Json
import sangria.schema._
import sangria.marshalling.circe._
import scala.meta.{io => _, _}
import caseapp._
import cats.syntax.either._

sealed trait Command {
  def run(args: Seq[String]): Result[Unit]
  def outputStream(path: Option[String]): PrintStream =
    path.map(file => new PrintStream(file)).getOrElse(System.out)
}

case class Generate(
    @HelpMessage("Path to GraphQL schema file")
    @ValueDescription("file")
    schema: String = "schema.graphql",
    @HelpMessage("Name of the enclosing object")
    @ValueDescription("name")
    `object`: String = "SangriaCodegen",
    @HelpMessage("Package name of the generated code")
    @ValueDescription("name")
    `package`: Option[String] = None,
    @HelpMessage("Output file for the generated code")
    @ValueDescription("path")
    output: Option[String] = None
) extends Command {
  def run(args: Seq[String]): Result[Unit] = {
    val stdout    = outputStream(output)
    val generator = ScalametaGenerator(`object`)
    val files     = args.map(path => new File(path))

    Builder(new File(schema))
      .withQuery(files: _*)
      .generate(generator)
      .map { code =>
        `package`.foreach { packageName =>
          stdout.println(s"package $packageName\n")
          stdout.println()
        }
        stdout.println(code.show[Syntax])
      }
  }
}

case class PrintSchema(
    @HelpMessage("Path to GraphQL introspection query result")
    @ValueDescription("file")
    schema: String = "schema.json",
    @HelpMessage("Output path for GraphQL schema language file")
    @ValueDescription("path")
    output: Option[String] = None
) extends Command {
  def run(args: Seq[String]): Result[Unit] = {
    val stdout = outputStream(output)

    parseIntrospectionSchema(new File(schema))
      .map { schema =>
        stdout.println(schema.renderPretty)
      }
  }

  def parseIntrospectionSchema(schemaFile: File): Result[Schema[_, _]] =
    for {
      json <- io.circe.jawn.parseFile(schemaFile).leftMap { error: io.circe.ParsingFailure =>
        Failure(s"Failed to parse schema in $schemaFile: $error")
      }

      schema <- Either
        .catchNonFatal {
          val builder = new DefaultIntrospectionSchemaBuilder[Unit]
          Schema.buildFromIntrospection[Unit, Json](json, builder)
        }
        .leftMap { error: Throwable =>
          Failure(s"Failed to parse schema in $schemaFile: ${error.getMessage}")
        }
    } yield schema
}

object Main extends CommandApp[Command] {

  override val appName    = "Sangria Codegen"
  override val appVersion = BuildInfo.version
  override val progName   = "sangria-codegen"

  override def run(command: Command, args: RemainingArgs): Unit = {
    command.run(args.args).left.foreach { failure =>
      System.err.println(failure.getMessage)
      System.exit(1)
    }
    ()
  }

}
