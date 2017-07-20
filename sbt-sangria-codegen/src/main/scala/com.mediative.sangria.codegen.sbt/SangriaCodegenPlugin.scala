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

object SangriaCodegenPlugin extends AutoPlugin {

  object autoImport {
    val SangriaCodegen        = config("sangria-codegen")
    val sangriaCodegenSchema  = taskKey[File]("Path to the GraphQL schema")
    val sangriaCodegenQueries = taskKey[Seq[File]]("GraphQL query documents")
    val sangriaCodegen        = taskKey[File]("Generate the Path to the DC/OS CLI")
  }
  import autoImport._

  override def requires = plugins.JvmPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    ivyConfigurations += SangriaCodegen,
    libraryDependencies += "com.mediative" % "sangria-codegen-cli_2.11" % BuildInfo.version % SangriaCodegen,
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
    },
    sangriaCodegenSchema := (resourceDirectory in Compile).value / "schema.graphql",
    resourceDirectories in sangriaCodegen := (resourceDirectories in Compile).value,
    includeFilter in sangriaCodegen := "*.graphql",
    excludeFilter in sangriaCodegen := HiddenFileFilter,
    sangriaCodegenQueries := Defaults
      .collectFiles(
        resourceDirectories in sangriaCodegen,
        includeFilter in sangriaCodegen,
        excludeFilter in sangriaCodegen)
      .value,
    sourceGenerators in Compile += Def.task { Seq(sangriaCodegen.value) },
    sangriaCodegen := {
      val schema  = sangriaCodegenSchema.value.getAbsolutePath
      val output  = sourceManaged.value / "SangriaCodegen.scala"
      val queries = sangriaCodegenQueries.value.map(_.getAbsolutePath)
      val options = Seq("generate", "--schema", schema, "--output", output.getAbsolutePath) ++ queries

      (runner in (SangriaCodegen, run)).value.run(
        "com.mediative.sangria.codegen.cli.Main",
        Attributed.data((fullClasspath in SangriaCodegen).value),
        options,
        streams.value.log
      )

      output
    }
  )
}
