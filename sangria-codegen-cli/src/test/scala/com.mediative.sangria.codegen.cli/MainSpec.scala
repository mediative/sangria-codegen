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

  "sangria-codegen print-schema" should {
    "pretty-print result of instrospection query in the GraphQL schema language format" in {
      val schemaPath = new File(testDir, "schema.json").getAbsolutePath
      val output     = new File(outputDir, "schema.graphql")
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
