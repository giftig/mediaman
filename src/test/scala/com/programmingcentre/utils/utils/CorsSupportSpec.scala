package com.programmingcentre.utils.utils

import org.scalatest._
import spray.http._
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest


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
    Options("/test") ~> testRoute ~> check {
      status.intValue should be (200)
      header("Access-Control-Allow-Headers").isDefined should be (true)
      header("Access-Control-Max-Age").isDefined should be (true)

      val allowMethods = header("Access-Control-Allow-Methods").get.value.split(", ")
      Array("OPTIONS", "POST", "GET") foreach { allowMethods should contain (_) }
      Array("PUT", "DELETE") foreach { allowMethods should not contain (_) }
    }
  }

  it should "respond to all requests with the Access-Control-Allow-Origin header" in {
    Get("/test") ~> testRoute ~> check {
      header("Access-Control-Allow-Origin").isDefined should be (true)
    }
    Post("/test") ~> testRoute ~> check {
      header("Access-Control-Allow-Origin").isDefined should be (true)
    }
  }
}
