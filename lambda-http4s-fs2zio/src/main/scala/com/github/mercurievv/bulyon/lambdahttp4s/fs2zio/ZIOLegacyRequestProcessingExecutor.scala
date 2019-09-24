package com.github.mercurievv.bulyon.lambdahttp4s.fs2zio

import java.time.{LocalDate, ZonedDateTime}
import java.time.format.DateTimeFormatter

import io.circe.generic.auto._
import cats.effect._
import fs2._
import _root_.io.circe._
import cats.Id
import cats.data.{EitherT, Kleisli}
import cats.implicits._
import com.github.mercurievv.bulyon.common.{
  BLWSError,
  BadRequest_400,
  BusinessError,
  Conflict_409,
  InternalServerError_500,
  NotFound_404
}
import com.github.mercurievv.bulyon.http4s.Http4sFunctionProcessor
import com.github.mercurievv.bulyon.lambdahttp4s.fs2zio.ZIOLegacyRequestProcessingExecutor.{
  DefaultRequestProcessorExecuterImpl,
  IOL,
  Resp,
  Sequence,
  SequenceResponse,
  Single,
  SingleResponse
}
import org.http4s.dsl.Http4sDsl
//import org.http4s.dsl.io._
import org.http4s.QueryParamDecoder.fromUnsafeCast
import org.http4s._
import org.slf4j.{Logger, LoggerFactory}
import zio.{TaskR, ZIO}

import scala.compat.Platform.EOL
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.higherKinds
import zio.interop.catz.implicits._
import zio.interop.catz._

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 10/2/2017
  * Time: 9:56 PM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  *
  * Its dirty implementation. Was done to start ASAP. Should be changed to better use ZIO (or IO) capabilities. Also I should do something with single/sequence
  */
object ZIOLegacyRequestProcessingExecutor {
//  type IO[-R, +E, +A]  = ZIO[R, E, A]
  type IOL[A] = ZIO[cats.effect.IO[_], Throwable, A] //legacy
  type KleisliIO[A, B] = Kleisli[IOL, A, B]

  object IOL {
    def apply[A](body: => A): IOL[A] = ZIO(body)
  }

  type Single[A] = Either[Throwable, A]
  type Sequence[A] = Stream[IOL, A]

  type SingleResponse[A] = IOL[Single[A]] //=IOL[Either[Throwable,A]]
  type SequenceResponse[A] = SingleResponse[Sequence[A]] //=IOL[Either[Throwable,Stream[IOL,A]]]

  type DefaultRequestProcessorExecuterImpl[F[_]] = Http4sFunctionProcessor[λ[
    A => F[Single[A]]
  ], λ[A => F[Single[_root_.fs2.Stream[F, A]]]], F]

  type Resp = IOL[Response[IOL]]
  type Req = Request[IOL]
}

object DefaultRequestProcessorExecuterImpl
    extends DefaultRequestProcessorExecuterImpl[
      ZIOLegacyRequestProcessingExecutor.IOL
    ] {
  type IOL[A] = ZIO[cats.effect.IO[_], Throwable, A] //legacy
  val dsl: Http4sDsl[IOL] = Http4sDsl[IOL]
  import dsl._
  //  type EntityBody[+F[_]] = Stream[F, Byte]

  val mockUnitProcessor: Object => SingleResponse[Unit] = o => IOL(Right(Unit))

//  override def processBody[T](bodyIo: Req)(implicit decoder: EntityDecoder[IOL, T]): T = ??? //bodyIo.as[T].unsafeRunSync()
//  override def processBody[T](bodyStream: http4s.DefaultRequestProcessorExecuterImpl.Req)(implicit decoder: EntityDecoder[IO, T]): T = ???
  override def processBody[T](bodyStream: ZIOLegacyRequestProcessingExecutor.Req)(
    implicit decoder: EntityDecoder[IOL, T]
  ): T = ???

  private val log: Logger =
    LoggerFactory.getLogger(ZIOLegacyRequestProcessingExecutor.getClass)
  private val printer = Printer.spaces2.copy(dropNullValues = true)

  type ErrData = (Class[_], Request[IOL])

//  implicit def singleResponseToIo[F[_], A](f: F[Single[A]]): SingleResponse[A] = ???
//  implicit def fToIo[F[_]](f: Request[F]): Request[IOL]                      = ???
//  implicit def respToIo[F[_]](f: IOL[Response[IOL]]): F[Response[F]]       = ???

  override def processSingle[I, O](processor: I => SingleResponse[O],
                                   input: SingleResponse[I],
                                   toJson: O => Json,
                                   req: Request[IOL],
  ): Resp = {
    import org.http4s.dsl._
    import zio.interop.catz._
    import zio.interop.catz.implicits._
    try {

//      log.info(input.toString)
      for {
        output <- {
          for {
            i <- EitherT(input)
            o <- {
              EitherT(processor(i)) //.either.map(ee => ee.right.flatMap(s => s))
            }
          } yield o
        }.value
        response <- asyncIOLToJson(
          toJson,
          output,
          (processor.getClass, req /*, buildInfo*/ )
        )
      } yield response
    } catch {
      case t: Throwable =>
        log.error("processSingle Error processing: " + t.getMessage, t)
        val error: Status.InternalServerError.type = InternalServerError
        error(IOL { t.getMessage })
    }
  }

  implicit val decoderZdt: QueryParamDecoder[ZonedDateTime] =
    QueryParamDecoder[String].map(s => ZonedDateTime.parse(s))
  implicit lazy val dateTimeQueryParamDecoder: QueryParamDecoder[LocalDate] =
    QueryParamDecoder[String].map(
      s => LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)
    )

  override def processSequence[I, O](
    processor: Function[I, SequenceResponse[O]],
    input: SingleResponse[I],
    toJson: O => Json,
    req: Request[IOL],
  ): Resp = {
    try {
      for {
        output <- {
          for {
            i <- EitherT(input)
            o <- {
              try {
                val out = processor(i) //.attempt.map(ee => ee.right.flatMap(s => s))
                EitherT(out)
              } catch {
                case e: Throwable =>
                  EitherT.leftT[IOL, Sequence[O]](e)
              }
            }
          } yield o
        }.value
        response <- asyncToJson(
          toJson,
          output,
          (processor.getClass, req /*, buildInfo*/ )
        )
      } yield response
    } catch {
      case t: Throwable =>
        log.error("processSequence Error processing: " + t.getMessage, t)
        InternalServerError(IOL { t.getMessage })
    }
  }

  private def asyncToJson[F <: Function[I, SequenceResponse[O]], O, I](
    toJson: O => Json,
    result: Single[Sequence[O]],
    errData: ErrData
  ): IOL[Response[IOL]] = {
    result match {
      case Left(t) =>
        handleErrors(t, errData)
      case Right(entityStream) =>
        val bodyE = for {
          entity <- entityStream.handleErrorWith(th => {
            log.error(
              "Item processing error. Skip this item: " + th.getMessage,
              th
            )
            Stream.empty
          })
          json <- Stream.emit(toJson(entity))
          stringBody <- Stream.emit(printer.pretty(json))
        } yield stringBody
        val body = Stream("[") ++ bodyE.intersperse(", ") ++ Stream("]")
        Ok(IOL { body })
    }
    /*
        IOL.async((function: Either[Throwable, Response[IOL]] => Unit) => function(Right(LogHelper.log("toJson", () => {
        }
        ))))
//  }
   */
  }

  def asyncIOLToJson[O, I](toJson: O => Json,
                           result: Single[O],
                           errData: ErrData): IOL[Response[IOL]] = {
    result match {
      case Left(t) =>
        handleErrors(t, errData)
      case Right(entity) => {
        try {
          val jsonString = IOL { printer.pretty(toJson(entity)) }
          Ok(jsonString)
        } catch {
          case e: Throwable => handleErrors(e, errData)
        }
      }
    }
  }

  private def getBusinessErrorCode(blerr: BLWSError): Option[Business] =
    blerr match {
      case be: BusinessError =>
        be.businessErrorId.map(bec => Business(bec, bec))
      case _ => None
    }

  private def handleErrors[F <: Function[I, SingleResponse[O]], O, I](
    t: Throwable,
    errData: ErrData
  ) = {
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

  private def createErrorJson[I, O, F <: Function[I, SequenceResponse[O]]](
    t: Throwable,
    code: Int,
    name: String,
    errData: (Class[_], Request[IOL] /*, BuildInfo*/ ),
    businessError: Option[Business]
  ): IOL[String] = {
    import _root_.io.circe.generic.auto._
    import _root_.io.circe.generic.encoding._
    import _root_.io.circe.syntax._
    IOL {
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
