import ReleaseKeys._
import ReleaseTransformations._

Jvm.`1.8`.required

inThisBuild(
  Def.settings(
    organization := "com.mediative",
    scalaVersion := "2.12.3",
    crossScalaVersions := Seq("2.11.11", "2.12.3"),
    scalacOptions += "-Ywarn-unused-import",
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false,
    watchSources ++= (((baseDirectory in ThisBuild).value / "samples") ** "*").get,
    doctestTestFramework := DoctestTestFramework.ScalaTest
  ))

lazy val macroAnnotationSettings = Seq(
  resolvers += Resolver.sonatypeRepo("releases"),
  resolvers += Resolver.bintrayRepo("scalameta", "maven"),
  addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M10" cross CrossVersion.full),
  scalacOptions += "-Xplugin-require:macroparadise",
  scalacOptions in (Compile, console) ~= (_ filterNot (_ contains "paradise")) // macroparadise plugin doesn't work in repl yet.
)

lazy val root = Project(id = "sangria-codegen-root", base = file("."))
  .enablePlugins(
    MediativeGitHubPlugin,
    MediativeReleasePlugin,
    CrossPerProjectPlugin,
    SiteScaladocPlugin)
  .aggregate(codegen, cli, sbtPlugin)
  .settings(
    noPublishSettings,
    releaseCrossBuild := false,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies, { st: State =>
        val v = sys.props.get("version").getOrElse {
          st.log.error("No version specified, rerun with `-Dversion=x.y.z`")
          sys.exit(1)
        }
        st.put(versions, (v, v))
      },
      runClean,
      releaseStepCommandAndRemaining("+test"),
      setReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("+publish"),
      pushChanges
    ) ++ postReleaseSteps.value,
    // Ignore the macro part when generating the API docs with sbt-unidoc
    sources in (ScalaUnidoc, unidoc) ~= { _.filter(_.getName != "GraphQLDomain.scala") },
    // Separate API docs for the sbt-plugin
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(sbtPlugin),
    siteSubdirName in (sbtPlugin, SiteScaladoc) := "sbt-plugin",
    addMappingsToSiteDir(
      mappings in (sbtPlugin, Compile, packageDoc),
      siteSubdirName in (sbtPlugin, SiteScaladoc)
    )
  )

val codegen = project("sangria-codegen")
  .enablePlugins(MediativeBintrayPlugin)
  .settings(
    macroAnnotationSettings,
    unmanagedSourceDirectories in Test += (baseDirectory in ThisBuild).value / "samples",
    libraryDependencies ++= Seq(
      "org.scalatest"       %% "scalatest" % "3.0.3" % Test,
      "org.scalameta"       %% "scalameta" % "1.8.0",
      "org.sangria-graphql" %% "sangria"   % "1.3.0",
      "org.typelevel"       %% "cats"      % "0.9.0"
    )
  )

val cli = project("sangria-codegen-cli")
  .enablePlugins(MediativeBintrayPlugin, BuildInfoPlugin)
  .dependsOn(codegen)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest"              %% "scalatest"     % "3.0.3" % Test,
      "com.github.alexarchambault" %% "case-app"      % "1.2.0-M4",
      "io.circe"                   %% "circe-jawn"    % "0.8.0",
      "org.sangria-graphql"        %% "sangria-circe" % "1.1.0",
      "org.scalaj"                 %% "scalaj-http"   % "2.3.0"
    ),
    fork in run := true,
    baseDirectory in run := (baseDirectory in ThisBuild).value,
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "com.mediative.sangria.codegen.cli"
  )

val sbtPlugin = project("sbt-sangria-codegen")
  .enablePlugins(MediativeBintrayPlugin, BuildInfoPlugin)
  .settings(
    scalaVersion := "2.10.6",
    crossScalaVersions := Seq("2.10.6"),
    scalacOptions ~= { _.filterNot(Set("-Ywarn-unused-import")) },
    publishLocal := publishLocal
      .dependsOn(publishLocal in codegen)
      .dependsOn(publishLocal in cli)
      .value,
    sbt.Keys.sbtPlugin := true,
    bintrayRepository := "sbt-plugins",
    scriptedSettings,
    // scriptedBufferLog := false,
    scriptedLaunchOpts += "-Dproject.version=" + version.value,
    scriptedLaunchOpts += "-Dsamples.dir=" + ((baseDirectory in ThisBuild).value / "samples"),
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      scalaVersion in ThisBuild,
      scalaBinaryVersion in ThisBuild),
    buildInfoPackage := "com.mediative.sangria.codegen.sbt"
  )

def project(name: String) = Project(id = name, base = file(name))
