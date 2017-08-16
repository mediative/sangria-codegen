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

package com.mediative.sangria.codegen.sbt

import sbt._
import sbt.Keys._

/**
 * Generate GraphQL schema file from a Sangria schema instance.
 *
 * == Usage ==
 *
 * This plugin must be explicitly enabled. To enable it add the following line
 * to your `.sbt` file:
 * {{{
 * enablePlugins(SangriaSchemagenPlugin)
 * }}}
 *
 * In addition, it is required to configure `sangriaSchemagenSnippet` with a snippet
 * of Scala code providing access to your `sangria.schema.Schema[_, _]`.
 *
 * See the [[https://github.com/mediative/sangria-codegen/tree/master/sbt-sangria-codegen/src/sbt-test/sangria-codegen/generate-schema example project]].
 *
 * == Using with [[SangriaCodegenPlugin]] ==
 *
 * To use this plugin to generate a schema for use with [[SangriaCodegenPlugin]]
 * they must be configured in separate projects. For example, [[SangriaSchemagenPlugin]]
 * could be enabled in the server project which defines the Sangria schema, and
 * [[SangriaCodegenPlugin]] will be enabled in the client project which consumes
 * the GraphQL services.
 *
 * See the [[https://github.com/mediative/sangria-codegen/tree/master/sbt-sangria-codegen/src/sbt-test/sangria-codegen/generate-schema-and-code example project]].
 *
 * == Implementation Details ==
 *
 * The plugins introduces a parallel build scope named `sangria-schemagen` and
 * uses it for generating a program for executing the schema introspection code
 * defined in `sangriaSchemagenSnippet`. The build scope depends on the normal
 * `compile` scope so schema classes are available when running the program.
 * Generate code can be found in `target/scala-2.x/src_managed/sangria-schemagen`
 * and compile artifacts can be found in `target/scala-2.x/sangria-schemagen-classes`.
 *
 * == Configuration ==
 *
 * Keys are defined in [[SangriaSchemagenPlugin.autoImport]].
 *
 *  - `sangriaSchemagenSnippet`: Code snippet to access the Sangria Schema.
 *  - `sangriaSchemagenSchema`: GraphQL schema output file. Defaults to
 *    file located inside `resourceManaged`.
 *  - `sangriaSchemagen`: Generate the GraphQL schema file.
 *
 * @example
 * {{{
 * sangriaSchemagenSnippet in Compile := Some("com.example.schema.GraphqlSchema")
 * sangriaSchemagenSchema in Compile := (resourceDirectory in Compile).value / "schema.graphql"
 * }}}
 */
object SangriaSchemagenPlugin extends AutoPlugin {

  object autoImport {
    val SangriaSchemagen        = config("sangria-schemagen").extend(Compile).hide
    val sangriaSchemagenSnippet = taskKey[Option[String]]("Code to access the Sangria schema")
    val sangriaSchemagenSchema  = taskKey[File]("GraphQL schema output file")
    val sangriaSchemagen        = taskKey[File]("Generate GraphQL schema file")
  }
  import autoImport._

  override def requires = plugins.JvmPlugin

  override def projectSettings: Seq[Setting[_]] =
    sangriaSchemagenScopedSettings(SangriaSchemagen, Compile) ++
      sangriaSchemagenDefaultSettings

  def sangriaSchemagenDefaultSettings: Seq[Setting[_]] = Seq(
    ivyConfigurations += SangriaSchemagen
  )

  def sangriaSchemagenScopedSettings(
      introspection: Configuration,
      runtime: Configuration): Seq[Setting[_]] = {
    val mainName    = introspection.name.split("-").map(_.capitalize).mkString
    val packageName = "com.mediative.sangria.codegen"
    val mainClass   = packageName + "." + mainName

    inConfig(runtime)(
      Seq(
        sangriaSchemagenSnippet := None,
        sangriaSchemagenSchema := resourceManaged.value / "sbt-sangria-codegen" / "schema.graphql",
        sangriaSchemagen := {
          val schemaFile = sangriaSchemagenSchema.value

          (runner in (introspection, run)).value.run(
            mainClass,
            Attributed.data((fullClasspath in introspection).value),
            List(schemaFile.getAbsolutePath),
            streams.value.log
          )

          streams.value.log.info("Generating schema in $schemaFile")
          schemaFile
        }
      )
    ) ++
      inConfig(introspection)(
        Defaults.compileSettings ++
          Seq(
            sourceGenerators += Def.task {
              val schemaCode = (sangriaSchemagenSnippet in runtime).value
              val file       = sourceManaged.value / "sbt-sangria-codegen" / s"$mainName.scala"

              if (!schemaCode.isDefined)
                sys.error(
                  "compile:sangriaSchemagenSnippet should contain a code snippet to access the Sangria schema")

              val content = s"""
                package $packageName

                object $mainName {
                  val schema: sangria.schema.Schema[_, _] = {
                    ${schemaCode.get}
                  }

                  def main(args: Array[String]): Unit = {
                    val schemaFile = new java.io.File(args(0))
                    val graphql: String = schema.renderPretty
                    schemaFile.getParentFile.mkdirs()
                    new java.io.PrintWriter(schemaFile) {
                      write(graphql)
                      close
                    }
                  }
                }
              """

              if (!file.exists || IO.read(file) != content)
                IO.write(file, content)

              Seq(file)
            }
          )
      )
  }
}
