import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtScalariform._
import sbtassembly.Plugin._
import AssemblyKeys._

object GitSubmoduleMoveBuild extends Build {

  lazy val gitSubmoduleMove = Project(
    id = "git-submodule-move",
    base = file("."),
    settings = Project.defaultSettings ++
      scalariformSettings ++
      assemblySettings ++
      Seq(
        name := "git-submodule-move",
        organization := "khernyo.git",
        version := "0.1-SNAPSHOT",
        scalaVersion := "2.10.1",

        scalacOptions ++= Seq(
          "-unchecked",
          "-deprecation",
          "-Dscala.timings=true",
          "-encoding", "UTF-8",
          "-target:jvm-1.6",
          "-feature",
          "-Ywarn-adapted-args"
        ),

        resolvers ++= Seq(),
        libraryDependencies ++= Seq(
          "org.eclipse.jgit" % "org.eclipse.jgit" % "2.3.1.201302201838-r"
        ),

        mainClass in assembly := Some("khernyo.git.GitSubmoduleMove")
      )
  )
}
