package com.github.mercurievv.bulyon.lambdahttp4s

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 7/12/2019
  * Time: 2:02 AM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
import java.io.{InputStream, OutputStream}

import cats.effect.Concurrent
import fs2.Stream
import io.circe.{Decoder, Encoder}
import org.http4s.HttpRoutes

import scala.util.Try

class ApiGSimpleHandler[F[_]](httpServices: Stream[F, HttpRoutes[F]])(
        implicit compiler: Stream.Compiler[F, F],
        FF: Concurrent[F],
        d: Decoder[ApiGProxyHttp4sRequestResponseLayer.Input],
        e: Encoder[ApiGProxyHttp4sRequestResponseLayer.Output]
) {
  private val apigProxyLambda: ApiGProxyToHttp4s[F] = new ApiGProxyToHttp4s(httpServices)
  private val awsLambdaLayer = AwsLambdaLayer(apigProxyLambda)

  def apply(ioStreams: (InputStream, OutputStream)): F[Try[Unit]] = {
    awsLambdaLayer(ioStreams)
  }

}
