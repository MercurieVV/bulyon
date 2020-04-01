package com.github.mercurievv.bulyon.lambdahttp4s.fs2zio

import java.time.ZonedDateTime

import com.github.mercurievv.bulyon.common._
import com.github.mercurievv.bulyon.http4s.Http4sFunctionProcessor
import _root_.io.circe._
import org.http4s._
import org.http4s.dsl._
import org.slf4j.{Logger, LoggerFactory}
import zio.interop.catz._
import zio.{ZIO, _}

/**
 * Created with IntelliJ IDEA.
 * User: Victor Mercurievv
 * Date: 10/2/2017
 * Time: 9:56 PM
 * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
 */
class ZIOHttp4sFunctionProcessor[R] extends Http4sFunctionProcessor[ZIO[R, Throwable, ?], ZIO[R, Throwable, ?]] {
  type APPIO[A] = LIO[R, A]
  val dsl: Http4sDsl[APPIO] = Http4sDsl[APPIO]
  import dsl._
  private val log: Logger = LoggerFactory.getLogger(classOf[ZIOHttp4sFunctionProcessor[_]])
  private val printer     = Printer.spaces2.copy(dropNullValues = true)
  type ErrData = (Class[_], Request[APPIO])

  def process[I, O](processor: I => APPIO[O], input: APPIO[I], req: Request[APPIO])(implicit entityEncoder: EntityEncoder[APPIO, O]): Resp = {
    try {

      //      log.info(input.toString)
      for {
        i <- input
        o <- processor(i).catchAll[R, Throwable, O](cacthErrors) //.foldM(e => handleErrors(e, (processor.getClass, req /*, buildInfo )), s => ZIO.succeed(s))//.handleErrorWith(e => handleErrors(e, (processor.getClass, req , buildInfo ))) //.either.map(ee => ee.right.flatMap(s => s))
        response <- asyncIOLToJson(
          o,
          (processor.getClass, req /*, buildInfo*/ )
        )
      } yield response
    } catch {
      case t: Throwable =>
        log.error("processSingle Error processing: " + t.getMessage, t)
        val error: Status.InternalServerError.type = InternalServerError
        error(ZIO { t.getMessage })
    }
  }

  def asyncIOLToJson[O, I](
                            result: O,
                            errData: ErrData
                          )(implicit entityEncoder: EntityEncoder[APPIO, O]): APPIO[Response[APPIO]] = {
    try {
      Ok(result)
    } catch {
      case e: Throwable => handleErrors(e, errData)
    }

  }

  private def handleErrors[O, I](
                                  t: Throwable,
                                  errData: ErrData
                                ): APPIO[Response[APPIO]] = {
    t match {
      case BusinessError(Some(_)) =>
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
                                     errData: (Class[_], Request[APPIO] /*, BuildInfo*/ ),
                                     businessError: Option[Business]
                                   ): APPIO[String] = {
    import _root_.io.circe.generic.auto._
    import _root_.io.circe.syntax._
    UIO {
      printer.print(
        ErrorResponse(
          t.getMessage,
          ZonedDateTime.now(),
          t.getStackTrace.mkString("", System.lineSeparator, System.lineSeparator),
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
  private case class ErrorResponse(
                                    message: String,
                                    time: ZonedDateTime,
                                    stacktrace: String,
                                    http_error_code: Int,
                                    http_error_text: String,
                                    request: ErrorRequestInfo,
                                    //          build: BuildInfo,
                                    businessError: Option[Business]
                                  )

  def cacthErrors[T](e: Throwable): IO[Throwable, Nothing] = e match {
    case t: Throwable => ZIO.fail[Throwable](t)
    case e            => ZIO.fail[Throwable](new Throwable("Unknown Error: " + e))
  }
}
