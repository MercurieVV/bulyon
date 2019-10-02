package com.github.mercurievv.bulyon.http4s

import cats.data.ValidatedNel
import io.circe.{Decoder, Json}
import org.http4s.{EntityDecoder, ParseFailure, Request, Response}

import scala.language.higherKinds

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 5/22/2018
  * Time: 3:09 AM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
trait Http4sFunctionProcessor[H4SF[_], PRF[_]] {
  type Req  = Request[H4SF]
  type Resp = H4SF[Response[H4SF]]

  def processBody[T](bodyStream: Req)(implicit decoder: Decoder[T]): H4SF[T]

/*
  def process[I, O](
          processor: (I, Req) => PRF[O],
          input: H4SF[I],
          toJson: O => Json,
          req: Request[H4SF],
  ): Resp = {
    val function: I => PRF[O] = processor(_: I, req)
    process(function, input, toJson, req)
  }
*/

  def process[I, O](
          processor: I => PRF[O],
          input: H4SF[I],
          toJson: O => Json,
          req: Request[H4SF],
  ): Resp


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
