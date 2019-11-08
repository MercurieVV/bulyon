package com.github.mercurievv.bulyon.lambdahttp4s

import cats.Monad
import cats.effect.{Concurrent, ConcurrentEffect, IO}
import com.amazonaws.services.lambda.runtime.Context
import com.github.mercurievv.bulyon.common.{IOFunction, Layer}
import com.github.mercurievv.bulyon.lambda.ApiGatewayProxyResponse
import com.github.mercurievv.bulyon.lambdahttp4s.ApiGProxyHttp4sRequestResponseLayer.Input
import fs2.Stream
import org.http4s.HttpRoutes
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable
import scala.language.higherKinds

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 7/2/2019
  * Time: 2:27 AM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
class ApiGProxyToHttp4s[F[_]](httpServices: Stream[F, HttpRoutes[F]])(implicit compiler: Stream.Compiler[F, F], FF: Concurrent[F])
    extends IOFunction[Input, F[ApiGProxyHttp4sRequestResponseLayer.Output]] {
  type Response = ApiGProxyHttp4sRequestResponseLayer.Output
  private val reqRespToHttp4s = new RequestRespnseToHttp4sRoutesLayer(httpServices)
  private val http4sProcessor = ApiGProxyHttp4sRequestResponseLayer(Layer.F(reqRespToHttp4s.apply))
  private val log: Logger     = LoggerFactory.getLogger(classOf[ApiGProxyToHttp4s[F]])

  @SuppressWarnings(Array("org.wartremover.warts.Null")) //fixme
  override def apply(input: ApiGatewayProxyHttp4sProcessor.Input): F[ApiGProxyHttp4sRequestResponseLayer.Output] = {
    handleRequest(httpServices, input, null)
  }

  private def handleRequest(httpServices: Stream[F, HttpRoutes[F]], input: Input, context: Context): F[ApiGProxyHttp4sRequestResponseLayer.Output] = {
//    SentryHelper.setTag("call_to", input.httpMethod + " " + input.path + " " + input.resource)
    log.info(s"Request ${input.httpMethod} ${input.path} ${input.resource} ${input.body} ${input.headers}") //we need to log it manually as http4s logger logs request only AFTER it finished. And we need it before to send info to sentry
    if (input.path == "is_alive")
      FF.pure(ApiGatewayProxyResponse(200, immutable.Map.empty, "OK", isBase64Encoded = false))
    else
      http4sProcessor(input)
  }
}
