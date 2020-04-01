package com.github.mercurievv.bulyon.lambda

import cats.arrow.Arrow
import com.github.mercurievv.bulyon.common.ErrorLevel.minor
import com.github.mercurievv.bulyon.common.{InternalServerError_500, Layer}
import com.github.mercurievv.bulyon.lambda.JStreamToStringLayer.JSON
import io.circe.{Decoder, Encoder}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.circe.parser.decode

import scala.language.higherKinds



/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 3/25/2019
  * Time: 6:16 PM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
object StringToObjectLayer {
  val printer: Printer = Printer.spaces2.copy(dropNullValues = true, reuseWriters = true)

  def apply[F[_, _]: Arrow, I, O](f: F[I, O])(implicit d: Decoder[I], e: Encoder[O]): F[JSON, JSON] = {
    Layer[F, JSON, I, O, JSON](
        Arrow[F].lift(jsonS => {
          val json = decode[I](jsonS).left.map(t => throw InternalServerError_500(s"Cant parse JSON: ${jsonS}", Some(t), minor))
          json match {
            case Right(x) => x
            case Left(t)  => throw InternalServerError_500("Cant parse JSON", Some(t), minor)
          }
        }),
        f,
        Arrow[F].lift(v => printer.print(v._2.asJson))
    )
  }
}
