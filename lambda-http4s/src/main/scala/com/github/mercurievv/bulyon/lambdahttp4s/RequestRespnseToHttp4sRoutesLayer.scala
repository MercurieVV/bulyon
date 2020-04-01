package com.github.mercurievv.bulyon.lambdahttp4s

import cats.effect.Concurrent
import fs2.Stream
import org.http4s.server.middleware.{RequestLogger, ResponseLogger}
import org.http4s.{HttpRoutes, Request, Response}

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 9/7/2019
  * Time: 1:29 AM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
class RequestRespnseToHttp4sRoutesLayer[F[_]](httpServices: Stream[F, HttpRoutes[F]]) {

  def apply(request: Request[F])(implicit compiler: Stream.Compiler[F, F], FF: Concurrent[F]): F[Option[Response[F]]] = {
    httpServices
      .map(httpRoutes => {
        val httpServiceWithLogging = ResponseLogger.httpRoutes(logHeaders = true, logBody = true)(
            RequestLogger.httpRoutes(logHeaders = true, logBody = true)(httpRoutes)
        )
        httpServiceWithLogging(request).value
      })
      .flatMap(Stream.eval).flatMap {
        case None        => Stream.empty
        case Some(value) => Stream(value)
      }
      .compile
      .last
  }
}
