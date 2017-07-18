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

import org.scalatest.WordSpec
import java.io.File

class SangriaCodegenSpec extends WordSpec {
  import starwars.TestSchema.StarWarsSchema

  val generator = ScalametaGenerator("CodegenResult")

  "SangriaCodegen.Builder" should {
    "fail with non-existent schema" in {
      val result = Builder(new File("schema-file-does-not-exist"))
        .generate(TreeGenerator)

      assert(
        result == Left(Failure("Failed to parse schema in schema-file-does-not-exist: " +
          "io.circe.ParsingFailure: schema-file-does-not-exist (No such file or directory)")))
    }

    "fail with non-existent query" in {
      val result = Builder(StarWarsSchema)
        .withQuery(new File("query-file-does-not-exist"))
        .generate(TreeGenerator)

      assert(
        result == Left(Failure("Failed to read query from query-file-does-not-exist: " +
          "query-file-does-not-exist (No such file or directory)")))
    }

    "merge query documents" in {
      val Right(tree) = Builder(StarWarsSchema)
        .withQuery(new File("src/test/resources/starwars/HeroAndFriends.graphql"))
        .withQuery(new File("src/test/resources/starwars/HeroNameQuery.graphql"))
        .generate(TreeGenerator)

      assert(tree.operations.flatMap(_.name) == Vector("HeroAndFriends", "HeroNameQuery"))
    }
  }
}
