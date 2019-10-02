package com.github.mercurievv.bulyon.lambdahttp4s.fs2zio

import java.time.ZonedDateTime

import com.github.mercurievv.bulyon.http4s.Http4sFunctionProcessor
import io.circe.Json
import io.circe.generic.auto._

import scala.compat.Platform.EOL
//import org.http4s.dsl.io._
import _root_.io.circe._
import com.github.mercurievv.bulyon.common._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe.jsonOf
import org.slf4j.{Logger, LoggerFactory}
import zio.{ZIO, _}
import zio.interop.catz._

import scala.language.higherKinds

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 10/2/2017
  * Time: 9:56 PM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
class ZIOHttp4sFunctionProcessor[R, E] extends Http4sFunctionProcessor[RIO[R, ?], ZIO[R, E, ?]] {
  type H4SIO[A] = RIO[R, A]
  type PRIO[A] = ZIO[R, E, A]
  val dsl: Http4sDsl[H4SIO] = Http4sDsl[H4SIO]
  import dsl._
  private val log: Logger = LoggerFactory.getLogger(classOf[ZIOHttp4sFunctionProcessor[_, _]])
  private val printer = Printer.spaces2.copy(dropNullValues = true)
  type ErrData = (Class[_], Request[H4SIO])
  def H4SIO[O](body: => O): H4SIO[O] = RIO(body)

  override def processBody[T](bodyStream: Req)(implicit decoder: Decoder[T]): RIO[R, T] = {
    implicit val entityDecoder: EntityDecoder[H4SIO, T] = jsonOf[H4SIO, T]
    bodyStream.as[T]
  }

  override def process[I, O](processor: I => ZIO[R, E, O], input: RIO[R, I], toJson: O => Json, req: Request[H4SIO]): Resp = {
    try {

      //      log.info(input.toString)
      for {
        i <- input
        o <- processor(i).catchAll {
          case t: Throwable => ZIO.fail(t)
          case e => ZIO.fail(new Throwable("Unknown Error: " + e))
        } //.foldM(e => handleErrors(e, (processor.getClass, req /*, buildInfo )), s => ZIO.succeed(s))//.handleErrorWith(e => handleErrors(e, (processor.getClass, req , buildInfo ))) //.either.map(ee => ee.right.flatMap(s => s))
        response <- asyncIOLToJson(
          toJson,
          o,
          (processor.getClass, req /*, buildInfo*/ )
        )
      } yield response
    } catch {
      case t: Throwable =>
        log.error("processSingle Error processing: " + t.getMessage, t)
        val error: Status.InternalServerError.type = InternalServerError
        error(H4SIO { t.getMessage })
    }
  }


  def asyncIOLToJson[O, I](
    toJson: O => Json,
    result: O,
    errData: ErrData
  ): H4SIO[Response[H4SIO]] = {
    try {
      val jsonString = H4SIO[String] { printer.pretty(toJson(result)) }
      Ok(jsonString)
    } catch {
      case e: Throwable => handleErrors(e, errData)
    }

  }

  private def handleErrors[O, I](
    t: Throwable,
    errData: ErrData
  ): H4SIO[Response[H4SIO]] = {
    t match {
      case BusinessError(Some(beId)) =>
        //        SentryHelper.setTag("error_type", "business_error")
        //        SentryHelper.setTag("business_error_type", beId)
        log.error(t.getMessage, t)
      case _ => log.error(t.getMessage, t)
    }
    t match {
      case blerr: Conflict_409 =>
        Conflict(
          createErrorJson(
            t,
            blerr.code,
            blerr.name,
            errData,
            getBusinessErrorCode(blerr)
          )
        )
      case blerr: InternalServerError_500 =>
        InternalServerError(
          createErrorJson(
            t,
            blerr.code,
            blerr.name,
            errData,
            getBusinessErrorCode(blerr)
          )
        )
      case blerr: BadRequest_400 =>
        BadRequest(
          createErrorJson(
            t,
            blerr.code,
            blerr.name,
            errData,
            getBusinessErrorCode(blerr)
          )
        )
      case blerr: NotFound_404 =>
        NotFound(
          createErrorJson(
            t,
            blerr.code,
            blerr.name,
            errData,
            getBusinessErrorCode(blerr)
          )
        )
      case blerr: BLWSError =>
        InternalServerError(
          createErrorJson(
            t,
            blerr.code,
            blerr.name,
            errData,
            getBusinessErrorCode(blerr)
          )
        )
      case _ =>
        InternalServerError(
          createErrorJson(t, 500, "Internal Server Error", errData, None)
        )
    }
  }

  private def getBusinessErrorCode(blerr: BLWSError): Option[Business] =
    blerr match {
      case be: BusinessError =>
        be.businessErrorId.map(bec => Business(bec, bec))
      case _ => None
    }

  private def createErrorJson[I, O](
                                                                          t: Throwable,
                                                                          code: Int,
                                                                          name: String,
                                                                          errData: (Class[_], Request[H4SIO] /*, BuildInfo*/ ),
                                                                          businessError: Option[Business]
                                                                        ): H4SIO[String] = {
    import _root_.io.circe.generic.auto._
    import _root_.io.circe.syntax._
    UIO {
      printer.pretty(
        ErrorResponse(
          t.getMessage,
          ZonedDateTime.now(),
          t.getStackTrace.mkString("", EOL, EOL),
          code,
          name,
          ErrorRequestInfo(
            errData._2.uri.renderString,
            errData._2.method.name,
            errData._2.pathInfo
          ),
          //            errData._3,
          businessError
        ).asJson
      )
    }
  }
  private case class ErrorRequestInfo(uri: String, method: String, path: String)
  private case class Business(code: String, text: String)
  private case class ErrorResponse(message: String,
                                   time: ZonedDateTime,
                                   stacktrace: String,
                                   http_error_code: Int,
                                   http_error_text: String,
                                   request: ErrorRequestInfo,
                                   //          build: BuildInfo,
                                   businessError: Option[Business])

}
