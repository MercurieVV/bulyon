package com.github.mercurievv.bulyon.http4s

import cats.data.ValidatedNel
import io.circe.Json
import org.http4s.{EntityDecoder, ParseFailure, Request, Response}

import scala.language.higherKinds

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 5/22/2018
  * Time: 3:09 AM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
trait Http4sFunctionProcessor[SingleResponseT[_], SequenceResponseT[_], F[_]] {
  type Req  = Request[F]
  type Resp = F[Response[F]]

  def processBody[T](bodyStream: Req)(implicit decoder: EntityDecoder[F, T]): T

  def processSingle[I, O](
          processor: (I, Req) => SingleResponseT[O],
          input: SingleResponseT[I],
          toJson: O => Json,
          req: Request[F],
  ): Resp = {
    val function: I => SingleResponseT[O] = processor(_: I, req)
    processSingle(function, input, toJson, req)
  }

  def processSingle[I, O](
          processor: I => SingleResponseT[O],
          input: SingleResponseT[I],
          toJson: O => Json,
          req: Request[F],
  ): Resp

  def processSequence[I, O](processor: I => SequenceResponseT[O], input: SingleResponseT[I], toJson: O => Json, req: Request[F]): Resp

/*
  def validation[T](o: Option[ValidatedNel[ParseFailure, T]]): Either[BadRequest_400, Option[T]] = o match {
    case None    => Right(None)
    case Some(s) => s.toEither.left.map(e => new BadRequest_400(e.toList.mkString("\n"))).map(Some(_))
  }

  def validation[T](o: List[T]): Either[BadRequest_400, List[T]] = Right(o)
  def validation[T](o: T): Either[BadRequest_400, T]             = Right(o)

  def validation[T](o: ValidatedNel[ParseFailure, T]): Either[BadRequest_400, T] = {
    o.toEither.left.map(e => new BadRequest_400(e.toList.mkString("\n")))
  }
*/
}
