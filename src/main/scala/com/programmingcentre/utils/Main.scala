package com.programmingcentre.utils

import akka.actor
import akka.io.{IO => AkkaIO}
import ch.qos.logback.classic.{Level => LogLevel, Logger}
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.routing.HttpService


object Main {
  val logger = LoggerFactory.getLogger("mediaman-main").asInstanceOf[Logger]

  def main(args: Array[String]): Unit = {
    logger.info("Service starting...")
    implicit val system = actor.ActorSystem()

    // FIXME: Service name "media-management" should be in config
    val service = system.actorOf(actor.Props[Service], "media-management")

    // FIXME: Interface and port should be in config
    AkkaIO(Http) ! Http.Bind(service, interface = "localhost", port = 8000)
  }
}
