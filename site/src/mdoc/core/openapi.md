---
layout: docs
title:  "OpenAPI usage Document"
position: 4
---

# OpenAPI usage

OpenAPI is a way to describe your web service API in pretty formal way, so it can be used to generate API and model classes

There exist sbt plugin https://github.com/MercurieVV/sbt-openapi-generator-plugin to run code generation from SBT. Also there exist extension, which allow generate http4s code: https://github.com/MercurieVV/openapi-codegen-http4s

Using provided above openapi libs, you can generate your API as Http4s code, which can be used with this Bulyon library to run your service on AWS Lambda

