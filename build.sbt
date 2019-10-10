lazy val root = (project in file("."))
  // .aggregate(todo)
  .dependsOn(todo % "compile->compile;compile->test")
  .enablePlugins(MdocPlugin)
  .settings(
    scalaVersion := "2.12.10",
    libraryDependencies ++=
      "org.scalatest"  %% "scalatest"  % ScalaTestVersion ::
      "org.scalacheck" %% "scalacheck" % ScalaCheckVersion ::
      Nil,
    scalacOptions ++= Seq(
      "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
      "-encoding", "utf-8",                // Specify character encoding used by source files.
      "-explaintypes",                     // Explain type errors in more detail.
      "-feature"
    )
  )

lazy val todo = (project in file("todo"))
  .settings(
    organization := "io.underscore.testing",
    name := "todo",
    scalaVersion := "2.12.10",
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.10"),
    libraryDependencies ++= Seq(
      "com.beachape"    %% "enumeratum"          % EnumeratumVersion,
      "io.circe"        %% "circe-generic"       % CirceVersion,
      "io.circe"        %% "circe-java8"         % CirceVersion,
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "org.scalacheck"  %% "scalacheck"          % ScalaCheckVersion,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion
    ),
    scalacOptions ++= Seq(
      "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
      "-encoding", "utf-8",                // Specify character encoding used by source files.
      "-explaintypes",                     // Explain type errors in more detail.
      "-feature",
      "-language:higherKinds",
      "-Ypartial-unification"
    ),
    testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "1")
  )

val CirceVersion      = "0.9.3"
val EnumeratumVersion = "1.5.13"
val Http4sVersion     = "0.18.11"
val LogbackVersion    = "1.2.3"
val ScalaCheckVersion = "1.14.0"
val ScalaTestVersion  = "3.0.5"