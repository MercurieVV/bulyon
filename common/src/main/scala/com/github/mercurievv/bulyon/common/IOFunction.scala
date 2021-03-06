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

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 7/2/2019
  * Time: 1:47 AM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
trait IOFunction[I, O] extends (I => O) {
  type Input  = I
  type Output = O
}
