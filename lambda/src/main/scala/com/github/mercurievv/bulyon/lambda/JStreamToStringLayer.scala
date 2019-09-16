package com.github.mercurievv.bulyon.lambda

import java.io.{InputStream, OutputStream}

import cats.Monad
import cats.data.Kleisli
import com.github.mercurievv.bulyon.common.Layer
import org.slf4j.{Logger, LoggerFactory}

import scala.io.Source
import scala.language.higherKinds
import scala.util.Try

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 7/11/2019
  * Time: 11:15 PM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
object JStreamToStringLayer {
  private val log: Logger = LoggerFactory.getLogger(JStreamToStringLayer.getClass)
  type IOS  = (InputStream, OutputStream)
  type JSON = String

  def apply[F[_]](f: Kleisli[F, JSON, JSON])(implicit F: Monad[F]): Kleisli[F, (InputStream, OutputStream), Try[Unit]] = {
    val in: IOS => F[JSON] = (inp: IOS) =>
      F.pure[JSON] {
        val string = Source.fromInputStream(inp._1).mkString
        log.info(s"Request" + string)
        inp._1.close()
        string
      }
    val out: ((IOS, String)) => F[Try[Unit]] = inp => {
      F.pure {
        val res = inp._2
        log.info("Response: " + res)
        val result = Try(inp._1._2.write(res.getBytes("UTF-8")))
        inp._1._2.close()
        result
      }
    }

    Layer.apply[Kleisli[F, ?, ?], IOS, JSON, JSON, Try[Unit]](
        Layer.F[F, IOS, JSON](in),
        f,
        Layer.F[F, (IOS, String), Try[Unit]](out)
    )
  }
}
