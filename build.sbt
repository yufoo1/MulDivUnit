ThisBuild / version := "alpha"

ThisBuild / scalaVersion := "2.12.15"

lazy val root = (project in file("."))
  .settings(
      name := "eula-core"
  )

addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % "3.5.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
    "edu.berkeley.cs" %% "chisel3" % "3.5.2",
    "edu.berkeley.cs" %% "chiseltest" % "0.5.2" % "test",
)

