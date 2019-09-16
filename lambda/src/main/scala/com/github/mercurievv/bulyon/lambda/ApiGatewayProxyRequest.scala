package com.github.mercurievv.bulyon.lambda

/**
  * Created with IntelliJ IDEA.
  * User: Victor Mercurievv
  * Date: 4/24/2019
  * Time: 2:57 AM
  * Contacts: email: mercurievvss@gmail.com Skype: 'grobokopytoff' or 'mercurievv'
  */
case class ApiGatewayProxyRequest(
        resource: String,
        path: String,
        httpMethod: String,
        headers: Option[Map[String, String]],
        queryStringParameters: Option[Map[String, String]],
        pathParameters: Option[Map[String, String]],
        stageVariables: Option[Map[String, String]],
//                                   context: Context,
        body: Option[String],
        isBase64Encoded: Option[Boolean]
)
