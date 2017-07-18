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
package starwars

import org.scalatest.WordSpec
import java.io.File
import scala.io.Source
import scala.meta._

class StarWarsCodegenSpec extends WordSpec {
  import TestSchema.StarWarsSchema

  val inputDirectory = new File("src/test/resources/starwars")
  val generator      = ScalametaGenerator("CodegenResult")

  def contentOf(file: File) =
    Source.fromFile(file).mkString

  "SangriaCodegen" should {
    for {
      input <- inputDirectory.listFiles()
      if input.getName.endsWith(".graphql")
      expected = new File(inputDirectory, input.getName.replace(".graphql", ".scala"))
      if expected.exists
    } {
      s"generate code for ${input.getName}" in {
        val Right(out) = Builder(StarWarsSchema)
          .withQuery(input)
          .generate(generator)

        assert(out.show[Syntax].trim == contentOf(expected).trim)
      }
    }
  }
}
