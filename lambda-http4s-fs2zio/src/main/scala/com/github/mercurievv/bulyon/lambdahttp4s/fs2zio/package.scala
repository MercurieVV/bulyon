package com.github.mercurievv.bulyon.lambdahttp4s

import zio.ZIO

/**
 * Created with IntelliJ IDEA.
 * User: Victor Mercurievv
 * Date: 3/31/2020
 * Time: 7:47 PM
 * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
 */
package object fs2zio {
  type LIO[-R, +A] = ZIO[R, Throwable, A]
}
