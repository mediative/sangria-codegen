resolvers += Resolver.bintrayIvyRepo("mediative", "sbt-plugins")

addSbtPlugin("com.mediative.sbt"   % "sbt-mediative-core" % "0.5.6")
addSbtPlugin("com.mediative.sbt"   % "sbt-mediative-oss"  % "0.5.6")
addSbtPlugin("com.github.tkawachi" % "sbt-doctest"        % "0.7.0")
addSbtPlugin("com.eed3si9n"        % "sbt-buildinfo"      % "0.7.0")
addSbtPlugin("com.eed3si9n"        % "sbt-doge"           % "0.1.5")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
