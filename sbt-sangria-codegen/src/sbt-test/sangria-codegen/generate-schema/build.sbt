name := "test"
enablePlugins(SangriaSchemagenPlugin)
scalaVersion := "2.12.3"

libraryDependencies += "org.sangria-graphql" %% "sangria" % "1.2.2"

val StarWarsDir = file(sys.props("samples.dir")) / "starwars"

sangriaSchemagenSnippet in Compile := Some(
  "com.mediative.sangria.codegen.starwars.TestSchema.StarWarsSchema")

TaskKey[Unit]("checkBefore") := {
  val schemaFile = (sangriaSchemagenSchema in Compile).value
  assert(!schemaFile.exists, "Schema file exists")
}

TaskKey[Unit]("check") := {
  val schemaFile = (sangriaSchemagen in Compile).value
  val expected   = StarWarsDir / "schema.graphql"

  assert(schemaFile == (sangriaSchemagenSchema in Compile).value)
  assert(schemaFile.exists, "Schema file does not exists")

  val compare = IO.read(schemaFile).trim == IO.read(expected).trim
  if (!compare)
    s"diff -u $expected $schemaFile".!
  assert(compare, s"$schemaFile does not equal $expected")
}
