package com.github.mercurievv.bulyon.lambda

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 4/24/2019
  * Time: 2:59 AM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
case class ApiGatewayProxyResponse(
        statusCode: Int,
        headers: Map[String, String],
        body: String,
        isBase64Encoded: Boolean
)
