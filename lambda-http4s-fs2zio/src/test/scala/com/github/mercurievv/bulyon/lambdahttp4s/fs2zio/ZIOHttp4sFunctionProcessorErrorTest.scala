package com.github.mercurievv.bulyon.lambdahttp4s.fs2zio

import com.github.mercurievv.bulyon.lambda.ApiGatewayProxyRequest
import io.circe.generic.auto._
import com.github.mercurievv.bulyon.lambdahttp4s.{ApiGProxyHttp4sRequestResponseLayer, ApiGProxyToHttp4s, ApiGSimpleHandler}
import fs2._
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io.{->, /, POST}
import org.http4s.{EntityBody, HttpRoutes, HttpService, Response}
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import zio.{DefaultRuntime, Exit, ZIO}
import zio.interop.catz._
import zio.interop.catz.implicits._

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
          .eval(value).map(t => {
            throw new RuntimeException("e1")
            t.toString
          })
      }

    val apigProxyLambda: ApiGProxyToHttp4s[AIO] = new ApiGProxyToHttp4s(
      fs2.Stream(
        HttpRoutes.of[AIO] {
          case req@POST -> Root / "test" =>
            proc.process[Int, Stream[AIO, String]](
              function,
              ZIO.succeed(5),
              null
            )
        }
      )
    )
    val runtime             = new DefaultRuntime {}

    val respBody = runtime.unsafeRunSync(apigProxyLambda(ApiGatewayProxyRequest("/test", "/test", "POST", None,None,None,None,None,None)).provide(())) match {
      case Exit.Failure(cause) =>
        println("ok")
        succeed
      case Exit.Success(value) =>
        println(value)
        fail()
    }
  }
}
