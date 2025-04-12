import Dependencies._
import sbtassembly.AssemblyPlugin.defaultShellScript

lazy val buildScalaVersion = "3.3.5"
lazy val pluginVersion = "0.3.0"

ThisBuild / scalaVersion     := buildScalaVersion
ThisBuild / version          := s"$pluginVersion-SNAPSHOT"
ThisBuild / organization     := "org.winlogon"
ThisBuild / organizationName := "winlogon"
Compile / mainClass := Some("org.winlogon.MineCord")

// GitHub CI
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("21"))
ThisBuild / publishTo := None
publish / skip := true

crossScalaVersions := Seq(buildScalaVersion)

lazy val root = (project in file("."))
  .settings(
    name := "minecord",
    assembly / assemblyOption := (assembly / assemblyOption).value.withIncludeScala(false),
  )

// Merge strategy for avoiding conflicts in dependencies
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

assembly / mainClass := Some("org.winlogon.MineCord")

libraryDependencies ++= Seq(
  "io.papermc.paper" % "paper-api" % "1.21.4-R0.1-SNAPSHOT" % Provided,
  "net.dv8tion" % "JDA" % "5.2.3",
  "dev.vankka" % "mcdiscordreserializer" % "4.3.0",
)

resolvers += "papermc-repo" at "https://repo.papermc.io/repository/maven-public/"
