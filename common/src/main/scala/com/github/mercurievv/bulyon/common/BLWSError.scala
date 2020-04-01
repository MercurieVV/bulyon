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
  * Date: 6/5/2018
  * Time: 12:36 PM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
import com.github.mercurievv.bulyon.common.ErrorLevel.minor
import org.slf4j.{Marker, MarkerFactory}

sealed trait ErrorLevel {
  val marker: Marker
}

object ErrorLevel {
  case object minor extends ErrorLevel {
    override val marker: Marker = MarkerFactory.getMarker("minor")
  }
  case object critical extends ErrorLevel {
    override val marker: Marker = MarkerFactory.getMarker("critical")
  } //notify managers
}

@SuppressWarnings(Array("org.wartremover.warts.Null"))
class BLWSError(message: String, cause: Option[Throwable] = None, pcode: Int, pname: String, errorLevell: ErrorLevel = minor)
    extends RuntimeException(message, Option(cause).flatten.orNull) {
  val code: Int              = pcode
  val name: String           = pname
  val errorLevel: ErrorLevel = errorLevell
  def this(message: String, pcode: Int, pname: String) = this(message, None, pcode, pname)
}

case class ContextedException(contextedMessage: String, cause: Throwable) extends RuntimeException(contextedMessage, cause)

trait BusinessError {
  val businessErrorId: Option[String]
}

object BusinessError {
  def unapply(e: BusinessError): Option[Option[String]] = Some(e.businessErrorId)
}

trait ListOfErrors {
  val errors: List[Error]
  def getCodesString: String = errors.map(_.code).distinct.sorted.mkString(",")
}
case class Error(
        code: String, //currently its need only for grouping by
        messaage: String
)

case class InternalServerError_500(message: String, cause: Option[Throwable] = None, override val errorLevel: ErrorLevel = minor)
    extends BLWSError(message, cause, 500, "Internal Server Error") {
  def this(message: String) = this(message, None)
}
case class BadGateway_502(gatewayUri: String, message: String, cause: Option[Throwable] = None, override val errorLevel: ErrorLevel = minor)
    extends BLWSError(s"This server reach enternal resource $gatewayUri, but get unexpected response " + message, cause, 500, "Bad Gateway")

case class NotFound_404(message: String, cause: Option[Throwable]) extends BLWSError(message, cause, 404, "Not Found") {
  def this(message: String) = this(message, None)
  def this() = this("")
}

case class BadRequest_400(message: String, cause: Option[Throwable]) extends BLWSError(message, cause, 400, "Bad Request") {
  def this(message: String) = this(message, None)
  def this() = this("")
}

case class Conflict_409(message: String, cause: Option[Throwable], businessErrorId: Option[String] = None)
    extends BLWSError(message, cause, 409, "Conflict")
    with BusinessError {
  def this(message: String) = this(message, None)
  def this() = this("")
}

case class UnprocessableEntity_422(message: String, cause: Option[Throwable], override val name: String) extends BLWSError(message, cause, 404, "Bad Request") {
  def this(name: String, message: String) = this(message, None, name)
  def this() = this("", "")
}

object ErrorHepler {
  implicit def throwEitherError[T](either: Either[Throwable, T]): T = either match {
    case Left(e)  => throw e
    case Right(t) => t
  }
}
