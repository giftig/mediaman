package com.programmingcentre.mediaman.admin

import org.scalatest._
import spray.http._
import spray.json._
import spray.testkit.ScalatestRouteTest

import com.programmingcentre.mediaman.admin.JSONProtocol._
import com.programmingcentre.mediaman.config.Config


class AdminSpec extends FlatSpec with ScalatestRouteTest with AdminAPI with Matchers {
  def actorRefFactory = system

  "The admin interface" should "respond to a ping" in {
    val request = Get("/ping")
    request ~> mainRoute ~> check { status.intValue should be (200) }
  }

  // Skipped pending fix for spray being incompatible with its own fucking library
  ignore should "provide status information" in {
    Get("/status") ~> mainRoute ~> check {
      status.intValue should be (200)
      val statusInfo = responseAs[ServiceStatus]
      statusInfo.service_name should be (Config.serviceName)
      statusInfo.service_port should be (Config.bindPort)
      statusInfo.admin_port should be (Config.adminBindPort)
    }
  }
}
