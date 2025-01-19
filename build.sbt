lazy val root = (project in file("."))
  .settings(Compile / run / mainClass := Some("kafkaessink.Main"))
  .dependsOn(core)
  .aggregate(core)

lazy val tests = (project in file("modules/it-tests"))
  .settings(
    name                     := "kafka-elasticsearch-sink-it",
    libraryDependencies ++= Seq(
      "com.disneystreaming"   %% "weaver-scalacheck"         % "0.8.4"  % Test,
      "com.disneystreaming"   %% "weaver-cats"               % "0.8.4"  % Test,
      "com.dimafeng"          %% "testcontainers-scala-core" % "0.41.4" % Test
    ),
    Test / fork              := true,
    Test / parallelExecution := false,
    Test / javaOptions += "-Dweaver.test.startupTimeout=240"
  )
  .dependsOn(core)

lazy val core = (project in file("modules/core"))
  .settings(commonSettings*)
  .settings(name := "kafka-elasticsearch-sink")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel"         %% "cats-core"                 % "2.9.0",
      "org.typelevel"         %% "cats-effect"               % "3.5.0",
      "co.fs2"                %% "fs2-core"                  % "3.7.0",
      "com.github.fd4s"       %% "fs2-kafka"                 % "3.6.0",
      "com.github.fd4s"       %% "fs2-kafka-vulcan"          % "3.6.0",
      "nl.gn0s1s"             %% "elastic4s-client-esjava"   % "8.16.0",
      "nl.gn0s1s"             %% "elastic4s-effect-cats"     % "8.16.0",
      "nl.gn0s1s"             %% "elastic4s-core"            % "8.16.0",
      "nl.gn0s1s"             %% "elastic4s-json-circe"      % "8.16.0",
      "org.typelevel"         %% "log4cats-slf4j"            % "2.7.0",
      "org.typelevel"         %% "log4cats-core"             % "2.7.0",
      "ch.qos.logback"         % "logback-classic"           % "1.4.7",
      "com.github.pureconfig" %% "pureconfig-core"           % "0.17.8",
      "com.disneystreaming"   %% "weaver-cats"               % "0.8.4"  % Test
    ),
    resolvers += "confluent" at "https://packages.confluent.io/maven/"
  )

val commonSettings = Def.settings(
  inThisBuild(
    List(
      scalaVersion := "3.6.2",
      version      := "0.1.0-SNAPSHOT",
      javacOptions ++= Seq("-source", "21", "-target", "21")
    )
  )
)

addCommandAlias("itTest", "tests / test")
