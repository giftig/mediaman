package com.programmingcentre.utils

import akka.actor
import akka.io.{IO => AkkaIO}
import ch.qos.logback.classic.{Level => LogLevel, Logger}
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.routing.HttpService

import com.programmingcentre.utils.admin.Admin
import com.programmingcentre.utils.config.Config
import com.programmingcentre.utils.media.Service


object Main {
  val logger = LoggerFactory.getLogger("mediaman-main").asInstanceOf[Logger]

  private implicit val system = actor.ActorSystem()
  val reaper = system.actorOf(actor.Props[DeadlyReaper])

  def main(args: Array[String]): Unit = {
    val environment = Config.environment getOrElse "production"
    val bindAddress = s"http://${Config.bindHost}:${Config.bindPort}"
    logger.info(s"${Config.serviceName} [$environment] starting; bound to $bindAddress")

    val service = system.actorOf(actor.Props[Service], Config.serviceName)
    val adminService = system.actorOf(actor.Props[Admin], s"${Config.serviceName}-admin")

    // Register the HTTP interfaces for the main service and admin API
    AkkaIO(Http) ! Http.Bind(service, interface = Config.bindHost, port = Config.bindPort)
    AkkaIO(Http) ! Http.Bind(
      adminService, interface = Config.adminBindHost, port = Config.adminBindPort
    )

    // The reaper will terminate the actor system when both services die
    reaper ! Reaper.Watch(service)
    reaper ! Reaper.Watch(adminService)
  }
}
