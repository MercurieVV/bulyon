package com.github.mercurievv.bulyon.lambdahttp4s

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 7/12/2019
  * Time: 2:02 AM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
import java.io.{InputStream, OutputStream}

import cats.{CommutativeMonad, Monad}
import cats.arrow.Arrow
import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
import com.github.mercurievv.bulyon.common.Layer
import com.github.mercurievv.bulyon.lambda.{JStreamToStringLayer, StringToObjectLayer}
import io.circe.{Decoder, Encoder}

import scala.language.higherKinds
import scala.util.Try

object AwsLambdaLayer {

  def apply[F[_], InputAwsCC, OutputAwsCC](processor: InputAwsCC => F[OutputAwsCC])(
          implicit
          d: Decoder[InputAwsCC],
          e: Encoder[OutputAwsCC],
          F: Monad[F]
  ): Kleisli[F, (InputStream, OutputStream), Try[Unit]] = {
    apply(Layer.F(processor))
  }

  def apply[F[_], InputAwsCC, OutputAwsCC](processor: Kleisli[F, InputAwsCC, OutputAwsCC])(
          implicit
          d: Decoder[InputAwsCC],
          e: Encoder[OutputAwsCC],
          F: Monad[F]
  ): Kleisli[F, (InputStream, OutputStream), Try[Unit]] = {
    JStreamToStringLayer(
        StringToObjectLayer[Kleisli[F, ?, ?], InputAwsCC, OutputAwsCC](
            processor
        )
    )
  }
}
