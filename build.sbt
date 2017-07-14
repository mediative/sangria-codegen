Jvm.`1.8`.required

inThisBuild(
  Def.settings(
    organization := "com.mediative",
    scalaVersion := "2.11.11",
    scalacOptions += "-Ywarn-unused-import",
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false,
    doctestTestFramework := DoctestTestFramework.ScalaTest,
    doctestWithDependencies := false
  ))

lazy val macroAnnotationSettings = Seq(
  resolvers += Resolver.sonatypeRepo("releases"),
  resolvers += Resolver.bintrayRepo("scalameta", "maven"),
  addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M9" cross CrossVersion.full),
  scalacOptions += "-Xplugin-require:macroparadise",
  scalacOptions in (Compile, console) ~= (_ filterNot (_ contains "paradise")) // macroparadise plugin doesn't work in repl yet.
)

lazy val root = Project(id = "sangria-codegen-root", base = file("."))
  .enablePlugins(MediativeGitHubPlugin, MediativeReleasePlugin)
  .aggregate(codegen)
  .settings(noPublishSettings)
  .settings(
    noPublishSettings,
    // Ignore the macro part when generating the API docs with sbt-unidoc
    sources in (ScalaUnidoc, unidoc) ~= { _.filter(_.getName != "GraphQLDomain.scala") }
  )

val codegen = project("sangria-codegen")
  .enablePlugins(MediativeBintrayPlugin)
  .settings(
    macroAnnotationSettings,
    libraryDependencies ++= Seq(
      "org.scalatest"       %% "scalatest"     % "3.0.3" % Test,
      "org.scalameta"       %% "scalameta"     % "1.8.0",
      "org.sangria-graphql" %% "sangria"       % "1.2.1",
      "org.sangria-graphql" %% "sangria-circe" % "1.1.0",
      "io.circe"            %% "circe-jawn"    % "0.8.0"
    )
  )

def project(name: String) = Project(id = name, base = file(name))