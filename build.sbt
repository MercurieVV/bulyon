name := "bulyon"

organization := "com.github.mercurievv"

version := "0.1.0"

//scalaVersion := "2.13.0"

crossScalaVersions := Seq("2.12.9", "2.13.0")
resolvers += Resolver.sonatypeRepo("releases")


lazy val http4s = (project in file("http4s"))
  .settings(
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "1.7.28",
      "org.http4s" %% "http4s-dsl" % "0.20.9",
      "org.http4s" %% "http4s-circe" % "0.20.9",
      "org.http4s" %% "http4s-core" % "0.20.9",
    )
    , addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)
  ).dependsOn(common)

lazy val lambda = (project in file("lambda"))
  .settings(
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "1.7.28",
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
      "org.typelevel" %% "cats-core" % "1.6.1",
    )
    , addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)
  ).dependsOn(common)

lazy val common = (project in file("common"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.6.1",
    )
    , addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)
  )
lazy val lambdaHttp4sDefault = (project in file("lambda-http4s-default"))
