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

import java.io.File
import org.scalatest.{BeforeAndAfter, WordSpec}
import scala.io.Source

class MainSpec extends WordSpec with BeforeAndAfter {
  val queryDirectory = new File("../sangria-codegen/src/test/resources/starwars")
  val testDir        = new File("src/test/resources")
  val outputDir      = new File("target/sangria-codegen-cli")

  before {
    Option(outputDir.listFiles()).foreach(_.foreach(_.delete()))
    outputDir.mkdirs()
  }

  def contentOf(file: File) =
    Source.fromFile(file).mkString

  "sangria-codegen generate" should {
    "generate Scala code" in {
      val schemaPath = new File(testDir, "schema.graphql").getAbsolutePath
      val query      = new File(testDir, "HeroNameQuery.graphql")
      val output     = new File(outputDir, "subdir/HeroNameQuery.scala")
      val command = Generate(
        schema = schemaPath,
        `object` = "HeroNameQuery",
        output = Some(output.getAbsolutePath)
      )
      val Right(_) = command.run(Seq(query.getAbsolutePath))

      val expected = new File(testDir, "HeroNameQuery.scala")
      assert(contentOf(output) == contentOf(expected))
    }

    "generate Scala code using GraphQL JSON schema" in {
      val schemaPath = new File(testDir, "schema.json").getAbsolutePath
      val query      = new File(testDir, "HeroNameQuery.graphql")
      val output     = new File(outputDir, "from/json/HeroNameQuery.scala")
      val command = Generate(
        schema = schemaPath,
        `object` = "HeroNameQuery",
        output = Some(output.getAbsolutePath)
      )
      val Right(_) = command.run(Seq(query.getAbsolutePath))

      val expected = new File(testDir, "HeroNameQuery.scala")
      assert(contentOf(output) == contentOf(expected))
    }

    "fail if --schema file does not exist" in {
      val command     = Generate(schema = "does-not-exist.graphql")
      val Left(error) = command.run(Seq.empty)
      assert(error.getMessage.startsWith("Failed to read does-not-exist.graphql"))
    }
  }

  "sangria-codegen print-schema" should {
    "pretty-print result of instrospection query in the GraphQL schema language format" in {
      val schemaPath = new File(testDir, "schema.json").getAbsolutePath
      val output     = new File(outputDir, "subdir/schema.graphql")
      val command    = PrintSchema(schema = schemaPath, output = Some(output.getAbsolutePath))
      val Right(_)   = command.run(Seq.empty)

      val expected = new File(testDir, "schema.graphql")
      assert(contentOf(output) == contentOf(expected))
    }

    "fail if --schema file does not exist" in {
      val command     = PrintSchema(schema = "does-not-exist.json")
      val Left(error) = command.run(Seq.empty)
      assert(error.getMessage.startsWith("Failed to parse schema in does-not-exist.json"))
    }
  }
}
