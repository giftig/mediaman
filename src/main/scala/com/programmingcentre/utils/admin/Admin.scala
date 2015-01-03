package com.programmingcentre.utils.admin

import akka.actor.{Actor, ActorContext, PoisonPill}
import spray.httpx.SprayJsonSupport
import spray.json._
import spray.routing.HttpService

import com.programmingcentre.utils.Main
import com.programmingcentre.utils.admin.JSONProtocol._
import com.programmingcentre.utils.config.Config

trait AdminAPI extends HttpService with SprayJsonSupport {
  val context: ActorContext

  /**
   * Just to ping the service and check if it's up; best bet is a HEAD request.
   */
  def ping = path("ping") { get {
    complete((200, "pong"))
  }}

  def status = path("status") { get {
    // FIXME: Needless to say, I should turn this response data into a case class and use
    //        either liftjson or spray-json to serialise it
    complete {
      val status = ServiceStatus(
        service_name = Config.serviceName,
        service_port = Config.bindPort,
        admin_port = Config.adminBindPort,
        uptime = context.system.uptime,
        pid = 0
      )
      status.toJson.asInstanceOf[JsObject]
    }
  }}

  def stopService = path("stop") { get {
    complete {
      Main.shutdown
      (202, "Accepted")
    }
  }}

  def mainRoute = ping ~ status ~ stopService
}

class Admin extends Actor with AdminAPI {
  def actorRefFactory = context
  def receive = runRoute(mainRoute)
}
