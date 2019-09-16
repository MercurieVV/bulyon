package com.github.mercurievv.bulyon.lambdahttp4s

import cats.implicits._
import cats.effect.Concurrent
import com.github.mercurievv.bulyon.lambda.{ApiGatewayProxyRequest, ApiGatewayProxyResponse}
import com.github.mercurievv.bulyon.lambdahttp4s.ApiGatewayProxyHttp4sProcessor.{Input, Output}
import fs2._
import org.http4s.server.middleware._

import scala.language.higherKinds
import org.http4s.{HttpRoutes, _}
import uk.org.lidalia.sysoutslf4j.context.{LogLevel, SysOutOverSLF4J}

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 11/9/2017
  * Time: 2:51 PM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
object ApiGatewayProxyHttp4sProcessor {

  type Input  = ApiGatewayProxyRequest
  type Output = ApiGatewayProxyResponse

  SysOutOverSLF4J.sendSystemOutAndErrToSLF4J(LogLevel.DEBUG, LogLevel.ERROR)
}

class ApiGatewayProxyHttp4sProcessor[F[_]](implicit compiler: Stream.Compiler[F, F], FF: Concurrent[F]) {
  def handleRequestScala(
          httpServices: Stream[F, HttpRoutes[F]],
          path: String,
          method: String = "GET",
          headers: Map[String, String] = Map(),
          queryParams: Map[String, String] = Map(),
          pathParams: Map[String, String] = Map(),
          stageVariables: Map[String, String] = Map(),
          body: Option[String] = None,
          resource: String = ""
  ): F[Output] = {
    performRequest(
        ApiGatewayProxyRequest(
            resource,
            path,
            method,
            Option(headers),
            Some(queryParams),
            Option(pathParams),
            Option(stageVariables)
            //      , null
            ,
            body,
            isBase64Encoded = Some(false)
        ),
        httpServices
    )
  }

  def performRequest(input: Input, httpServices: Stream[F, HttpRoutes[F]]): F[Output] = {

    val notFound404         = createLambdaResponse(404, "Page Not Found", Map())
    val request: Request[F] = createRequest(input)
    val processStream: F[Output] = httpServices
      .map(httpRoutes => {
        val httpRoutesLogged =
          ResponseLogger.httpRoutes(logHeaders = true, logBody = true)(RequestLogger.httpRoutes(logHeaders = true, logBody = true)(httpRoutes))
        val respOptt       = httpRoutesLogged(request)
        val responseOption = respOptt.map(http4sResponseToLamda).value
        responseOption.flatMap(_.getOrElse(FF.pure(notFound404)))
      }).compile.last.flatMap(_.getOrElse(FF.pure(notFound404)))
    processStream
  }

  private def createRequest(input: Input) = {
//    import fs2._
    import org.http4s._
    val bodyBytesStream: Stream[Pure, Byte] = input.body.map(_.getBytes).map(Stream.emits(_)).getOrElse(org.http4s.EmptyBody)
    val queryParams                         = input.queryStringParameters.getOrElse(Map())
    val uri                                 = Uri.unsafeFromString(cleanPathFromPrefixes(input.path)).copy(query = Query(queryParams.map(x => (x._1, Option(x._2))).toList: _*))
    val method                              = Method.fromString(input.httpMethod.toUpperCase).getOrElse(Method.GET)
    val headers                             = Headers(input.headers.toList.flatten.map(kv => Header(kv._1, kv._2)))
    Request[F](method = method, uri = uri, body = bodyBytesStream, headers = headers)
  }

  private def cleanPathFromPrefixes(path: String) = {
    path.replaceFirst("^/api", "").replaceFirst("^/v\\d*", "")
  }

  private def http4sResponseToLamda(resp: Response[F]): F[Output] = {
    val body: Stream[F, Byte]   = resp.body
    val vector: F[Vector[Byte]] = body.compile.toVector
    val bodyString: F[String]   = vector.map((bytes: Vector[Byte]) => new String(bytes.toArray))
//    log.info("Response: " + body)
    val headers = resp.headers.toList.map((header: Header) => header.name.value -> header.value).toMap
    bodyString.map(bs => createLambdaResponse(resp.status.code, bs, headers))

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
