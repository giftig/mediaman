package com.programmingcentre.utils.admin

import akka.actor.{Actor, ActorContext, PoisonPill}
import spray.routing.HttpService

import com.programmingcentre.utils.Main
import com.programmingcentre.utils.config.Config

trait AdminAPI extends HttpService {
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
      (
        200,
        s"""{"host": "${Config.adminBindHost}", "port": ${Config.adminBindPort}}"""
      )
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
