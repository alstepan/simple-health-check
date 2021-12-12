ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.6"

lazy val root = (project in file("."))
  .settings(
    name := "example-service",
    organization := "me.alstepan",
    assembly / mainClass := Some("me.alstepan.healthcheck.simpleservice.Main")
  )

ThisBuild / assemblyMergeStrategy := {
  case "application.conf" => MergeStrategy.concat
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

libraryDependencies ++= Seq(
  // cats
  "org.typelevel" %% "cats-core"            % "2.7.0",
  "org.typelevel" %% "cats-effect"          % "3.3.0",
  // logging
  "ch.qos.logback" % "logback-classic"      % "1.2.7",
  "org.typelevel" %% "log4cats-core"        % "2.1.1",
  "org.typelevel" %% "log4cats-slf4j"       % "2.1.1",
  // http4s
  "org.http4s"    %% "http4s-dsl"           % "0.23.6",
  "org.http4s"    %% "http4s-circe"         % "0.23.6",
  "org.http4s"    %% "http4s-blaze-server"  % "0.23.6",
)