package com.programmingcentre.utils

import akka.actor
import akka.io.{IO => AkkaIO}
import ch.qos.logback.classic.{Level => LogLevel, Logger}
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.routing.HttpService

import com.programmingcentre.utils.config.Config


object Main {
  val logger = LoggerFactory.getLogger("mediaman-main").asInstanceOf[Logger]

  def main(args: Array[String]): Unit = {
    logger.info(s"${Config.serviceName} starting...")

    implicit val system = actor.ActorSystem()
    val service = system.actorOf(actor.Props[Service], Config.serviceName)

    AkkaIO(Http) ! Http.Bind(service, interface = Config.bindHost, port = Config.bindPort)
  }
}
