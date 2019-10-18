name := "bulyon"

organization := "com.github.mercurievv"

version := "0.1.0"

//scalaVersion := "2.13.0"

crossScalaVersions := Seq("2.12.9", "2.13.0")
resolvers += Resolver.sonatypeRepo("releases")


lazy val lambdaHttp4s = (project in file("lambda-http4s"))  .settings(
  addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)
).dependsOn(http4s, lambda)

lazy val http4s = (project in file("http4s"))
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-server" % "0.20.9",
      "org.http4s" %% "http4s-dsl" % "0.20.9",
      "org.http4s" %% "http4s-circe" % "0.20.9",
      "org.http4s" %% "http4s-core" % "0.20.9",
    )
    , addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)
  ).dependsOn(common)

lazy val lambda = (project in file("lambda"))
  .settings(
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
      "org.typelevel" %% "cats-core" % "1.6.1",
      "io.circe" %% "circe-core" % "0.11.1",
      "io.circe" %% "circe-generic" % "0.11.1",
      "io.circe" %% "circe-parser" % "0.11.1",
    )
    , addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)
  ).dependsOn(common)

lazy val common = (project in file("common"))
  .settings(
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "1.7.28",
      "uk.org.lidalia" % "sysout-over-slf4j" % "1.0.2" excludeAll(ExclusionRule(organization = "org.slf4j")),
      "org.typelevel" %% "cats-core" % "2.0.0",
    )
    , addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)
  )
lazy val lambdaHttp4sFs2Zio = (project in file("lambda-http4s-fs2zio"))
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "1.0.5",
      "dev.zio" %% "zio" % "1.0.0-RC15",
      "dev.zio" %% "zio-interop-cats" % "2.0.0.0-RC5"
    )
    , addCompilerPlugin("org.typelevel" % "kind-projector" % "0.10.3" cross CrossVersion.binary)
  )
  .dependsOn(lambdaHttp4s)
