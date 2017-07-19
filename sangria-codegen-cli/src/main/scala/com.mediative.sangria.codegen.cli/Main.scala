package com.mediative.sangria.codegen
package cli

import java.io.{File, PrintStream}
import io.circe.Json
import sangria.schema._
import sangria.marshalling.circe._
import scala.meta.{io => _, _}
import caseapp._
import cats.syntax.either._

sealed trait Command

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
) extends Command

case class PrintSchema(
    @HelpMessage("Path to GraphQL introspection query result")
    @ValueDescription("file")
    schema: String = "schema.json",
    @HelpMessage("Output path for GraphQL schema language file")
    @ValueDescription("path")
    output: Option[String] = None
) extends Command

object Main extends CommandApp[Command] {

  override val appName    = "Sangria Codegen"
  override val appVersion = BuildInfo.version
  override val progName   = "sangria-codegen"

  def run(command: Command, args: RemainingArgs): Unit = {
    val result: Result[Unit] = command match {
      case options: Generate    => generate(options, args.args)
      case options: PrintSchema => printSchema(options)
    }

    result.left.foreach { failure =>
      System.err.println(failure.getMessage)
      System.exit(1)
    }
    ()
  }

  def generate(generate: Generate, args: Seq[String]): Result[Unit] = {
    val output    = outputStream(generate.output)
    val generator = ScalametaGenerator(generate.`object`)
    val files     = args.map(path => new File(path))

    Builder(new File(generate.schema))
      .withQuery(files: _*)
      .generate(generator)
      .map { code =>
        generate.`package`.foreach { packageName =>
          output.println(s"package $packageName\n")
          output.println()
        }
        output.println(code.show[Syntax])
      }
  }

  def printSchema(printSchema: PrintSchema): Result[Unit] = {
    val output = outputStream(printSchema.output)

    parseIntrospectionSchema(new File(printSchema.schema))
      .map { schema =>
        output.println(schema.renderPretty)
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

  def outputStream(path: Option[String]): PrintStream =
    path.map(file => new PrintStream(file)).getOrElse(System.out)
}
