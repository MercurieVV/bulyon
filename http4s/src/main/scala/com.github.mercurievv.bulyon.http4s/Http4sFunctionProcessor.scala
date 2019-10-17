package com.github.mercurievv.bulyon.http4s

import org.http4s.{EntityEncoder, Request, Response}

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

  def process[I, O](
          processor: I => PRF[O],
          input: H4SF[I],
          req: Request[H4SF],
  )(implicit ev: EntityEncoder[H4SF, O]): Resp


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
