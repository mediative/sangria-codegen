name := "test"
enablePlugins(SangriaCodegenPlugin)
scalaVersion := "2.12.3"

val StarWarsDir = file(sys.props("samples.dir")) / "starwars"

sangriaCodegenSchema in Compile := StarWarsDir / "schema.graphql"
resourceDirectories in (Compile, sangriaCodegen) += StarWarsDir
includeFilter in (Compile, sangriaCodegen) := "MultiQuery.graphql"
name in (Compile, sangriaCodegen) := "MultiQueryApi"

TaskKey[Unit]("check") := {
  val file     = (sangriaCodegen in Compile).value
  val expected = StarWarsDir / "MultiQuery.scala"

  assert(file.exists)

  // Drop the package line before comparing
  val compare = IO.readLines(file).drop(1).mkString("\n").trim == IO.read(expected).trim
  if (!compare)
    s"diff -u $expected $file".!
  assert(compare, s"$file does not equal $expected")
}
