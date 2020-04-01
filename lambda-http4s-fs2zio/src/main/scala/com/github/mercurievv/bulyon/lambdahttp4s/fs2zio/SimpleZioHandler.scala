package com.github.mercurievv.bulyon.lambdahttp4s.fs2zio

import java.io.{InputStream, OutputStream}

import _root_.io.circe.generic.auto._
import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import com.github.mercurievv.bulyon.lambdahttp4s.ApiGSimpleHandler
import fs2.Stream
import org.http4s._
import zio.DefaultRuntime
import zio.interop.catz._

/**
 * Created with IntelliJ IDEA.
 * User: Victor Mercurievv
 * Date: 3/31/2020
 * Time: 7:47 PM
 * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
 */
abstract class SimpleZioHandler extends RequestStreamHandler{
  val routes: Stream[LIO[Unit, *], HttpRoutes[LIO[Unit, *]]]
  val runtime: DefaultRuntime = new DefaultRuntime {}
  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    val apiGatewayHandler = new ApiGSimpleHandler(routes)//this line should run here, not in constructor. As scala do not initiate routes properly
    val process = apiGatewayHandler((input, output))
      .map(_.toEither)
      .absolve

    runtime
      .unsafeRunSync(process.provide(()))
      .getOrElse(cause => {
        println(cause)
      })
  }
}
