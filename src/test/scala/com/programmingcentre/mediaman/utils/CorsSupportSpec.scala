package com.programmingcentre.mediaman.utils

import org.scalatest._
import spray.http._
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest

import com.programmingcentre.mediaman.config.Config


class CorsSupportSpec extends FlatSpec
  with ScalatestRouteTest
  with HttpService
  with CorsSupport
  with Matchers {
  def actorRefFactory = system  // Connect the service API to the test ActorSystem

  val testRoute = path("test") { cors {
    get { complete((200, "'CORS it works!")) } ~
    post { complete((200, "'CORS I'll update that!")) }
  }}

  "A CORS route" should "work" in {
    Get("/test") ~> testRoute ~> check {
      status.intValue should be (200)
      responseAs[String] should be ("'CORS it works!")
    }
    Post("/test") ~> testRoute ~> check {
      status.intValue should be (200)
      responseAs[String] should be ("'CORS I'll update that!")
    }
  }

  it should "respond to OPTIONS requests properly" in {
    Options("/test") ~> addHeader("Origin", Config.corsAllowOrigins.head) ~> testRoute ~> check {
      status.intValue should be (200)
      header("Access-Control-Allow-Headers").isDefined should be (true)
      header("Access-Control-Max-Age").isDefined should be (true)
      header("Access-Control-Allow-Origin").get.value should be (Config.corsAllowOrigins.head)

      val allowMethods = header("Access-Control-Allow-Methods").get.value.split(", ")
      Array("OPTIONS", "POST", "GET") foreach { allowMethods should contain (_) }
      Array("PUT", "DELETE") foreach { allowMethods should not contain (_) }
    }
  }

  it should "disallow bad origins" in {
    Options("/test") ~> addHeader("Origin", "http://www.ihackugood.rip") ~> testRoute ~> check {
      header("Access-Control-Allow-Origin").isDefined should be (false)
    }
  }

  it should "omit Access-Control-Allow-Origin with no Origin header" in {
    Options("/test") ~> testRoute ~> check {
      header("Access-Control-Allow-Origin").isDefined should be (false)
    }
  }

  it should "respond to all methods with the right Access-Control-Allow-Origin header" in {
    Get("/test") ~> addHeader("Origin", Config.corsAllowOrigins.head) ~> testRoute ~> check {
      header("Access-Control-Allow-Origin").get.value should be (Config.corsAllowOrigins.head)
    }
    Post("/test") ~> addHeader("Origin", Config.corsAllowOrigins.head) ~> testRoute ~> check {
      header("Access-Control-Allow-Origin").get.value should be (Config.corsAllowOrigins.head)
    }
  }
}
