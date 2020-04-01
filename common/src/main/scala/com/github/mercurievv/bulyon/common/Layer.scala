/*
 * Copyright (c) 2020 the bulyon contributors.
 * See the project homepage at: https://mercurievv.github.io/bulyon/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mercurievv.bulyon.common

import cats.arrow.Arrow
import cats.data.Kleisli
import cats.implicits._

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
