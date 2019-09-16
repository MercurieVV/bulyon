package com.github.mercurievv.bulyon.lambdahttp4s

import cats.Monad
import cats.arrow.Arrow
import cats.data.Kleisli
import cats.effect.{Concurrent, ConcurrentEffect}
import cats.implicits._
import com.github.mercurievv.bulyon.common.Layer
import com.github.mercurievv.bulyon.lambda.{ApiGatewayProxyRequest, ApiGatewayProxyResponse}
import fs2.Stream
import org.http4s.{Header, HttpRoutes, Request, Response}

import scala.language.higherKinds

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 9/6/2019
  * Time: 9:15 PM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
object ApiGProxyHttp4sRequestResponseLayer {
  type Input  = ApiGatewayProxyRequest
  type Output = ApiGatewayProxyResponse
//  type K[A, B] = Kleisli[G, A, B]
  val notFound404: Output = createLambdaResponse(404, "Page Not Found", Map())
  import Kleisli._

  def apply[G[_]](
          f: Kleisli[G, Request[G], Option[Response[G]]]
  )(implicit compiler: Stream.Compiler[G, G], FF: Concurrent[G]): Kleisli[G, Input, Output] =
    Layer[Kleisli[G, ?, ?], Input, Request[G], Option[Response[G]], Output](
        Arrow[Kleisli[G, *, *]].lift(createRequest),
        f,
        Layer.F(p => http4sResponseToLamda(p))
    )

  private def createRequest[G[_]](input: Input) = {
    import fs2._
    import org.http4s._
    val bodyBytesStream: Stream[Pure, Byte] = input.body.map(_.getBytes).map(Stream.emits(_)).getOrElse(org.http4s.EmptyBody)
    val queryParams                         = input.queryStringParameters.getOrElse(Map())
    val uri                                 = Uri.unsafeFromString(cleanPathFromPrefixes(input.path)).copy(query = Query(queryParams.map(x => (x._1, Option(x._2))).toList: _*))
    val method                              = Method.fromString(input.httpMethod.toUpperCase).getOrElse(Method.GET)
    val headers                             = Headers(input.headers.toList.flatten.map(kv => Header(kv._1, kv._2)))
    Request[G](method = method, uri = uri, body = bodyBytesStream, headers = headers)
  }

  private def cleanPathFromPrefixes(path: String) = {
    path.replaceFirst("^/api", "").replaceFirst("^/v\\d*", "")
  }

  private def http4sResponseToLamda[F[_]](
          respOpt: (Input, Option[Response[F]])
  )(implicit compiler: Stream.Compiler[F, F], FF: Concurrent[F]): F[Output] = {
    respOpt._2 match {
      case Some(resp) =>
        val body: Stream[F, Byte]   = resp.body
        val vector: F[Vector[Byte]] = body.compile.toVector
        val bodyString: F[String]   = vector.map((bytes: Vector[Byte]) => new String(bytes.toArray))
        //    log.info("Response: " + body)
        val headers = resp.headers.toList.map((header: Header) => header.name.value -> header.value).toMap
        bodyString.map(bs => createLambdaResponse(resp.status.code, bs, headers))
      case None => FF.pure(notFound404)
    }

  }

  def createLambdaResponse(code: Int, bodyString: String, headers: Map[String, String]): Output = {
    ApiGatewayProxyResponse(
        code,
        headers,
        bodyString,
        isBase64Encoded = false
    )
  }
}
