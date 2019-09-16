name := "scala-template"

organization := "com.github.mercurievv"

version := "0.1.0"

//scalaVersion := "2.13.0"

crossScalaVersions := Seq("2.12.9", "2.13.0")

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.28",
  "org.typelevel" %% "cats-core" % "1.6.1"
)