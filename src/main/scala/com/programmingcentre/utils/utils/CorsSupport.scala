package com.programmingcentre.utils.utils

import spray.http.{AllOrigins, HttpMethods, HttpMethod, HttpResponse}
import spray.http.HttpHeaders._
import spray.http.HttpMethods._
import spray.routing._

import com.programmingcentre.utils.config.Config

/**
 * A mixin to provide support for providing CORS headers as appropriate
 */
trait CorsSupport {
  this: HttpService =>

  private val allowOriginHeader = `Access-Control-Allow-Origin`(Config.corsAllowOrigins)
  private val optionsCorsHeaders = List(
    `Access-Control-Allow-Headers`(Config.corsAllowHeaders.mkString(", ")),
    `Access-Control-Max-Age`(60 * 60 * 24 * 20),  // cache pre-flight response for 20 days
    `Access-Control-Allow-Credentials`(Config.corsAllowCredentials)
  )

  def cors[T]: Directive0 = mapRequestContext {
    context => context.withRouteResponseHandling {
      // If an OPTIONS request was rejected as 405, complete the request by responding with the
      // defined CORS details and the allowed options grabbed from the rejection
      case Rejected(reasons) if (
        context.request.method == HttpMethods.OPTIONS &&
        reasons.exists(_.isInstanceOf[MethodRejection])
      ) => {
        val allowedMethods = reasons.collect { case r: MethodRejection => r.supported }

        context.complete(HttpResponse().withHeaders(
          `Access-Control-Allow-Methods`(OPTIONS, allowedMethods :_*) ::
          allowOriginHeader ::
          optionsCorsHeaders
        ))
      }
    } withHttpResponseHeadersMapped { headers => allowOriginHeader :: headers }
  }
}
