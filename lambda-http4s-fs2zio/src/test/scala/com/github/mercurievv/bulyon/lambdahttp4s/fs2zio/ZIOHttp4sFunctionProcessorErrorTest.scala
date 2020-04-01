package com.github.mercurievv.bulyon.lambdahttp4s.fs2zio

import com.github.mercurievv.bulyon.lambda.ApiGatewayProxyRequest
import com.github.mercurievv.bulyon.lambdahttp4s.ApiGProxyToHttp4s
import fs2._
import org.http4s.HttpRoutes
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io.{->, /, POST}
import org.scalatest.flatspec.AnyFlatSpec
import zio.interop.catz._
import zio.{DefaultRuntime, Exit, ZIO}

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 11/6/2019
  * Time: 4:24 PM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
class ZIOHttp4sFunctionProcessorErrorTest extends AnyFlatSpec {
  type AIO[T] = ZIO[Unit, Throwable, T]
  val proc = new ZIOHttp4sFunctionProcessor[Unit]
  "Stream with error" should "return correct error after processing" in {

    val function: Int => AIO[Stream[AIO, String]] = (i: Int) =>
      ZIO.succeed {
        val value = ZIO.succeed(i)

        Stream
          .eval(value).map(_ => {
            throw new RuntimeException("e1")
          })
      }

    val apigProxyLambda: ApiGProxyToHttp4s[AIO] = new ApiGProxyToHttp4s(
      fs2.Stream(
        HttpRoutes.of[AIO] {
          case POST -> Root / "test" =>
            proc.process[Int, Stream[AIO, String]](
              function,
              ZIO.succeed(5),
              null
            )
        }
      )
    )
    val runtime             = new DefaultRuntime {}

    runtime.unsafeRunSync(apigProxyLambda(ApiGatewayProxyRequest("/test", "/test", "POST", None,None,None,None,None,None)).provide(())) match {
      case Exit.Failure(_) =>
        println("ok")
        succeed
      case Exit.Success(value) =>
        println(value)
        fail()
    }
  }
}
