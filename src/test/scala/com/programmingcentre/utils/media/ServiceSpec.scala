package com.programmingcentre.utils.media

import akka.actor
import org.scalatest._
import spray.http.{FormData, HttpMethods, HttpRequest}
import spray.testkit.ScalatestRouteTest

import com.programmingcentre.utils.config.Config


class ServiceSpec extends FileWritingSpec with ScalatestRouteTest with ServiceAPI with Matchers {
  def actorRefFactory = system  // Connect the service API to the test ActorSystem

  "The service" should "create new TV programmes" in {
    val request = Post("/programme", new FormData(Seq(("name", "Hodor Hodor Hodor"))))
    request ~> mainRoute ~> check { status.intValue should be (200) }
    request ~> mainRoute ~> check { status.intValue should be (409) }
  }
}
