ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.2.0"

lazy val root = (project in file("."))
  .settings(
    name := "simple-health-check",
    organization := "me.alstepan",
    assembly / mainClass := Some("me.alstepan.healthcheck.Server")
  )


ThisBuild / assemblyMergeStrategy := {
  case "application.conf" => MergeStrategy.concat
  case "module-info.class" => MergeStrategy.discard
  case PathList("META-INF/versions/9/", xs @ _*) => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

Test / fork := true

libraryDependencies ++= Seq(
  // cats
  "org.typelevel" %% "cats-core"            % "2.8.0",
  "org.typelevel" %% "cats-effect"          % "3.3.14",
  // circe
  "com.monovore"  %% "decline"              % "2.3.0",
  "io.circe"      %% "circe-core"           % "0.14.3",
  "io.circe"      %% "circe-parser"         % "0.14.3",
  "io.circe"      %% "circe-generic"        % "0.14.3",
  "io.circe"      %% "circe-literal"        % "0.14.3",
  "io.circe"      %% "circe-yaml"           % "0.14.1",
  // http4s
  "org.http4s"    %% "http4s-dsl"           % "0.23.16",
  "org.http4s"    %% "http4s-circe"         % "0.23.16",
  "org.http4s"    %% "http4s-ember-server"  % "0.23.16",
  "org.http4s"    %% "http4s-ember-client"  % "0.23.16",
  //postgres
  "org.postgresql" % "postgresql"           % "42.5.0",
  //flyway
  "org.flywaydb"  % "flyway-core"           % "9.3.0",   //db migration
  //doobie
  "org.tpolecat"  %% "doobie-core"          % "1.0.0-RC2",
  "org.tpolecat"  %% "doobie-hikari"        % "1.0.0-RC2",    // HikariCP transactor.
  "org.tpolecat"  %% "doobie-postgres"      % "1.0.0-RC2",    // Postgres driver 42.3.1 + type mappings.
  // logging
  "org.slf4j"      % "slf4j-api"            % "2.0.1",
  "ch.qos.logback" % "logback-classic"      % "1.4.1",
  "org.typelevel" %% "log4cats-core"        % "2.5.0",
  "org.typelevel" %% "log4cats-slf4j"       % "2.5.0",

  //tests
  "org.scalactic" %% "scalactic"            % "3.2.12",
  "org.scalatest" %% "scalatest"            % "3.2.12" % Test,
  "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test,
)

