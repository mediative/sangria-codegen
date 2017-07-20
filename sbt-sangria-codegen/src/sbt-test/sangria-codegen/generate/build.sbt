name := "test"
enablePlugins(SangriaCodegenPlugin)
scalaVersion := "2.11.11"

TaskKey[Unit]("check") := {
  val file = sangriaCodegen.value
  val expected =
    """
      |object SangriaCodegen {
      |  case class HeroNameQuery(hero: HeroNameQuery.Hero)
      |  object HeroNameQuery {
      |    case class HeroNameQueryVariables()
      |    case class Hero(name: Option[String])
      |  }
      |}
    """.stripMargin.trim

  assert(file.exists)
  assert(IO.read(file).trim == expected)
}
