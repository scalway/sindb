lazy val core = project.in(file("sindb-core"))
  .settings(
    name := "sindb",
    version := "0.0.1",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    organization := "com.scalway",
    homepage := Some(url("https://github.com/scalway/sindb")),
    description := "very simple scala IndexedDB wrapper",

    scalaVersion := "2.11.12",
    crossScalaVersions := Seq("2.11.12", "2.12.6"),

    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.6",
      "com.lihaoyi" %%% "sourcecode" % "0.1.3",
      "com.lihaoyi" %%% "upickle" % "0.6.5"
    ),

    scalacOptions ++= Seq(
      "-encoding", "UTF-8",
      "-feature",
      "-deprecation",
      "-unchecked",
//      "–Xcheck-null",
      "-Xfatal-warnings",
      /* "-Xlint", */
      "-Ywarn-adapted-args",
      /* "-Ywarn-dead-code", */
      "-Ywarn-inaccessible",
      "-Ywarn-nullary-override",
      "-Ywarn-numeric-widen"
    ),
    publishMavenStyle := false,
    bintrayRepository := "sindb",
    bintrayPackageLabels := Seq("scala", "scala.js", "IndexedDB", "database"),
    bintrayRepository := "maven",
    bintrayVcsUrl := Some("git@github.com:scalway/sindb.git"),

    publishMavenStyle := true,
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    libraryDependencies ++= Seq("org.scala-js" %%% "scalajs-dom" % "0.9.2"),
    scalaJSStage in Global := FastOptStage
  )
  .enablePlugins(ScalaJSPlugin)
