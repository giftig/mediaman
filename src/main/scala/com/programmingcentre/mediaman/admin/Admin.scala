package com.programmingcentre.mediaman.admin

import akka.actor.{Actor, ActorContext, PoisonPill}
import spray.httpx.SprayJsonSupport
import spray.json._
import spray.routing.HttpService

import com.programmingcentre.mediaman.{Main, Reaper}
import com.programmingcentre.mediaman.admin.JSONProtocol._
import com.programmingcentre.mediaman.config.Config

trait AdminAPI extends HttpService with SprayJsonSupport {
  val context: ActorContext

  /**
   * Just to ping the service and check if it's up; best bet is a HEAD request.
   */
  def ping = path("ping") { get {
    complete((200, "pong"))
  }}

  /**
   * Respond with some JSON-encoded service informaiton
   */
  def status = path("status") { get {
    complete {
      ServiceStatus(
        service_name = Config.serviceName,
        service_port = Config.bindPort,
        admin_port = Config.adminBindPort,
        uptime = context.system.uptime,
        pid = Main.pid
      ).toJson.asInstanceOf[JsObject]
    }
  }}

  /**
   * Send the Reaper a KillAll message, instructing it to kill the service safely.
   */
  def stopService = path("stop") { post {
    complete {
      Main.reaper ! Reaper.KillAll
      (202, "Accepted")
    }
  }}

  def mainRoute = ping ~ status ~ stopService
}

class Admin extends Actor with AdminAPI {
  def actorRefFactory = context
  def receive = runRoute(mainRoute)
}
