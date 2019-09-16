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
