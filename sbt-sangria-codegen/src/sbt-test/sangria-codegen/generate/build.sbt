name := "test"
enablePlugins(SangriaCodegenPlugin)
scalaVersion := sys.props("scala.version")

TaskKey[Unit]("check") := {
  val file = (sangriaCodegen in Compile).value
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