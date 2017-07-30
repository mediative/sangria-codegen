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
 * Generate API code based on a GraphQL schema and queries.
 *
 * == Usage ==
 *
 * This plugin must be explicitly enabled. To enable it add the following line
 * to your `.sbt` file:
 * {{{
 * enablePlugins(SangriaCodegenPlugin)
 * }}}
 *
 * By default, the plugin reads `*.graphql` files from the resources directory
 * and generates a single Scala source files in the managed source directory.
 *
 * See the [[https://github.com/mediative/sangria-codegen/tree/master/sbt-sangria-codegen/src/sbt-test/sangria-codegen/generate example project]].
 *
 * == Configuration ==
 *
 * Keys are defined in [[SangriaCodegenPlugin.autoImport]].
 *
 *  - `sangriaCodegenSchema`: The GraphQL schema file. *
 *  - `sangriaCodegenQueries`: GraphQL query documents. Defaults to `*.graphql`
 *   files found inside  which are found inside the resources directory.
 *
 *  - `resourceDirectories in sangriaCodegen`: The resource directories in which
 *    to search for GraphQL query documents.
 *
 *  - `includeFilter in sangriaCodegen`: Filter which query documents to include.
 *    Defaults to `"*.graphql"`.
 *
 *  - `excludeFilter in sangriaCodegen`: Filter out query documents.
 *    Defaults to `HiddenFileFilter`.
 *
 * @example
 * {{{
 * sangriaCodegenSchema := sourceDirectory.value / "graphql" / "schema.graphql"
 * sangriaCodegenQueries += sourceDirectory.value / "graphql" / "api.graphql"
 * }}}
 */
object SangriaCodegenPlugin extends AutoPlugin {

  object autoImport {
    val SangriaCodegen        = config("sangria-codegen")
    val sangriaCodegenSchema  = taskKey[File]("GraphQL schema file")
    val sangriaCodegenQueries = taskKey[Seq[File]]("GraphQL query documents")
    val sangriaCodegen        = taskKey[File]("Generate GraphQL API code")
  }
  import autoImport._

  override def requires = plugins.JvmPlugin

  override def projectSettings: Seq[Setting[_]] =
    sangriaCodegenScopedSettings(Compile) ++ sangriaCodegenDefaultSettings

  def sangriaCodegenDefaultSettings: Seq[Setting[_]] = Seq(
    ivyConfigurations += SangriaCodegen,
    libraryDependencies ++= List(
      // Explicitly set the Scala version dependency so the resolution doesn't pick
      // up the Scala version of the project the plugin is enabled in.
      "org.scala-lang" % "scala-reflect"                                        % BuildInfo.scalaVersion % SangriaCodegen,
      "com.mediative"  % s"sangria-codegen-cli_${BuildInfo.scalaBinaryVersion}" % BuildInfo.version      % SangriaCodegen
    ),
    mainClass in SangriaCodegen := Some("com.mediative.sangria.codegen.cli.Main"),
    fullClasspath in SangriaCodegen := Classpaths
      .managedJars(SangriaCodegen, classpathTypes.value, update.value),
    runner in (SangriaCodegen, run) := {
      val forkOptions = ForkOptions(
        bootJars = Nil,
        javaHome = javaHome.value,
        connectInput = connectInput.value,
        outputStrategy = outputStrategy.value,
        runJVMOptions = javaOptions.value,
        workingDirectory = Some(baseDirectory.value),
        envVars = envVars.value
      )
      new ForkRun(forkOptions)
    }
  )

  def sangriaCodegenScopedSettings(conf: Configuration): Seq[Setting[_]] =
    inConfig(conf)(
      Seq(
        sangriaCodegenSchema := resourceDirectory.value / "schema.graphql",
        resourceDirectories in sangriaCodegen := resourceDirectories.value,
        includeFilter in sangriaCodegen := "*.graphql",
        excludeFilter in sangriaCodegen := HiddenFileFilter,
        sangriaCodegenQueries := Defaults
          .collectFiles(
            resourceDirectories in sangriaCodegen,
            includeFilter in sangriaCodegen,
            excludeFilter in sangriaCodegen)
          .value,
        sourceGenerators += Def.task { Seq(sangriaCodegen.value) },
        sangriaCodegen := {
          val schema  = sangriaCodegenSchema.value.getAbsolutePath
          val output  = sourceManaged.value / "sbt-sangria-codegen" / "SangriaCodegen.scala"
          val queries = sangriaCodegenQueries.value.map(_.getAbsolutePath)
          val options = Seq("generate", "--schema", schema, "--output", output.getAbsolutePath) ++ queries

          (runner in (SangriaCodegen, run)).value.run(
            (mainClass in SangriaCodegen).value.get,
            Attributed.data((fullClasspath in SangriaCodegen).value),
            options,
            streams.value.log
          )

          output
        }
      ))
}
