name := "test"
enablePlugins(SangriaCodegenPlugin)
scalaVersion := "2.11.11"

TaskKey[Unit]("check") := {
  val file = (sangriaCodegen in Compile).value
  val expected =
    """package sangria.codegen
      |
      |object SangriaCodegen {
      |  case class HeroNameQuery(hero: SangriaCodegen.HeroNameQuery.Hero)
      |  object HeroNameQuery {
      |    case class HeroNameQueryVariables()
      |    case class Hero(name: Option[String])
      |  }
      |}
    """.stripMargin.trim

  assert(file.exists)
  assert(IO.read(file).trim == expected)
}
