package com.github.mercurievv.bulyon.common

import cats.arrow.Arrow
import cats.data.Kleisli
import cats.implicits._

import scala.language.higherKinds

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 7/9/2019
  * Time: 3:25 PM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  *    |   Layer   |
  * -->|-->I-->O-->|--\
  *    |           |  |
  * <--|<--O<--I<--|<-/
  */
object Layer {

  object F {
    def apply[F[_], A, B](run: A => F[B]) = Kleisli(run)
  }

  def apply[F[_, _]: Arrow, `I->`, `O->`, `I<-`, `O<-`](
          -> : F[`I->`, `O->`],
          f: F[`O->`, `I<-`],
          -< : F[(`I->`, `I<-`), `O<-`]
  ): F[`I->`, `O<-`] = {
    val inputId   = Arrow[F].id[`I->`]
    val fResponse = inputId &&& inputId >>> -> >>> f
    fResponse >>> -<
  }
}
