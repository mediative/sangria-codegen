package com.mediative.sangria.codegen
package cli

import java.io.File
import scala.meta._

case class Config(
    command: String = "default",
    schema: Option[File] = None,
    `object`: String = "SangriaCodegen",
    `package`: Option[String] = None,
    output: Option[File] = None,
    files: Seq[File] = Seq.empty
)

object Main {
  val parser = new scopt.OptionParser[Config]("sangria-codegen") {
    head("Sangria Codegen", BuildInfo.version)
    help("help").text("Prints this usage text")
    version("version")

    note("")

    cmd("generate")
      .action((_, c) => c.copy(command = "generate"))
      .text("Generate code from a GraphQL schema and query documents")
      .children(
        opt[File]('s', "schema")
          .required()
          .valueName("<file>")
          .action((file, opts) => opts.copy(schema = Some(file)))
          .text("Output file for the generated code"),
        opt[File]('o', "output")
          .valueName("<file>")
          .action((file, opts) => opts.copy(output = Some(file)))
          .text("Output file for the generated code"),
        opt[String]('p', "package")
          .valueName("<name>")
          .action((name, opts) => opts.copy(`package` = Some(name)))
          .text("Package name of the generated code"),
        opt[String]('O', "object")
          .valueName("<name>")
          .action((name, opts) => opts.copy(`object` = name))
          .text("Name of the enclosing object"),
        arg[File]("<file>...")
          .unbounded()
          .optional()
          .action((x, c) => c.copy(files = c.files :+ x))
          .text("optional unbounded args")
      )
  }

  def main(args: Array[String]): Unit = {
    def terminate(result: Result[Unit]) = {
      result.left.map { failure =>
        parser.reportError(failure.getMessage)
        System.exit(1)
      }
      ()
    }

    parser.parse(args, Config()) match {
      case Some(config) if config.command == "generate" =>
        terminate(generate(config))

      case Some(config) if config.command == "default" =>
        parser.showUsage()

      case None =>
        System.exit(1)
    }
  }

  def generate(config: Config): Result[Unit] = {
    val output    = config.output.map(file => new java.io.PrintStream(file)).getOrElse(System.out)
    val generator = ScalametaGenerator(config.`object`)

    Builder(config.schema.get)
      .withQuery(config.files: _*)
      .generate(generator)
      .right
      .map { code =>
        config.`package`.foreach { packageName =>
          output.println(s"package $packageName\n")
          output.println()
        }
        output.println(code.show[Syntax])
      }
  }
}
