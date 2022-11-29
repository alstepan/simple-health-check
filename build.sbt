ThisBuild / scalaVersion     := "3.2.1"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "me.alstepan"
ThisBuild / organizationName := "simplehealthcheck"

lazy val root = (project in file("."))
  .settings(
    name := "simple-health-check-zio",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.2",
      "dev.zio" %% "zio-json" % "0.3.0",
      "dev.zio" %% "zio-streams" % "2.0.2",
      //database
      "io.getquill" %% "quill-jdbc-zio" % "4.6.0",
      "org.postgresql" % "postgresql" % "42.5.0",
      "io.d11"  %% "zhttp" % "2.0.0-RC11",
      // logging
      "org.slf4j" % "slf4j-api" % "2.0.3",
      "ch.qos.logback" % "logback-classic" % "1.4.4",
      //config
      "dev.zio" %% "zio-config" % "3.0.2",
      "dev.zio" %% "zio-config-typesafe" % "3.0.2",
      "dev.zio" %% "zio-config-magnolia" % "3.0.2",
        //test
      "dev.zio" %% "zio-test" % "2.0.2" % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
