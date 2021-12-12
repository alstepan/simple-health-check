resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases"

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.6"

lazy val root = (project in file("."))
  .settings(
    name := "simple-health-check",
    organization := "me.alstepan",
    assembly / mainClass := Some("me.alstepan.healthcheck.Server")
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
  // config
  "com.github.pureconfig" %% "pureconfig"   % "0.17.1",
  // circe
  "com.monovore"  %% "decline"              % "2.2.0",
  "io.circe"      %% "circe-core"           % "0.14.1",
  "io.circe"      %% "circe-parser"         % "0.14.1",
  "io.circe"      %% "circe-generic"        % "0.14.1",
  "io.circe"      %% "circe-literal"        % "0.14.1",
  // http4s
  "org.http4s"    %% "http4s-dsl"           % "0.23.6",
  "org.http4s"    %% "http4s-circe"         % "0.23.6",
  "org.http4s"    %% "http4s-blaze-server"  % "0.23.6",
  "org.http4s"    %% "http4s-blaze-client"  % "0.23.6",
  //tests
  "org.scalactic" %% "scalactic"            % "3.2.10",
  "org.scalatest" %% "scalatest"            % "3.2.10" % "test"
)

