---
layout: docs
title:  "Example Document"
position: 2
---

# Example

We will create simple function, which will be called over APIGateway > AWSLambda
Initially, in AWS you need create your **AWS Lambda** function and setup **API Gateway** to pass proxy request to it.

## Scala code
```scala
libraryDependencies += "com.github.mercurievv" %% "lambdahttp4sfs2zio" % "1.0.10"
```

Create http4s route function
```scala mdoc:silent
import com.github.mercurievv.bulyon.lambdahttp4s.fs2zio.ZIOHttp4sFunctionProcessor
import zio.ZIO

type APPIO[T] = ZIO[Unit, Throwable, T]
val proc = new ZIOHttp4sFunctionProcessor[Unit]
val coolFunction: String => APPIO[String] = (i: String) => ZIO
  .succeed(i.toInt + 1)
  .map(r => s"Here is result: $r")
```

Create handler instance, which will be called by AWS Lambda:
```scala mdoc:silent
import com.github.mercurievv.bulyon.lambdahttp4s.fs2zio.SimpleZioHandler
import zio.ZIO
import fs2._
import org.http4s._
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io.{->, /, POST}
import zio.interop.catz._

object AwsLambdaZioRuntime extends SimpleZioHandler {

  type APPIO[T] = ZIO[Unit, Throwable, T]
  override val routes: fs2.Stream[Pure, HttpRoutes[APPIO]] = Stream(
    HttpRoutes.of[APPIO] {
      case rq@POST -> Root / "test" =>
        proc.process[String, String](coolFunction, rq.as[String], rq)
    })
}
```

Then, you need upload/deploy your function to AWS Lambda

And its ready for launch.
```curl
curl --location --request POST 'https://your.apigateway.domain.com/test' \
   --header 'Content-type: application/json' \
   --header 'Accept: application/json' \
   --data-raw '"5"'
```
Should return:
```Here is result: 6```
