package com.programmingcentre.utils

import akka.actor
import akka.io.{IO => AkkaIO}
import ch.qos.logback.classic.{Level => LogLevel, Logger}
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.routing.HttpService

import com.programmingcentre.utils.config.Config
import com.programmingcentre.utils.media.Service


object Main {
  val logger = LoggerFactory.getLogger("mediaman-main").asInstanceOf[Logger]

  def main(args: Array[String]): Unit = {
    val environment = Config.environment getOrElse "production"
    val bindAddress = s"http://${Config.bindHost}:${Config.bindPort}"
    logger.info(s"${Config.serviceName} [$environment] starting; bound to $bindAddress")

    implicit val system = actor.ActorSystem()
    val service = system.actorOf(actor.Props[Service], Config.serviceName)

    AkkaIO(Http) ! Http.Bind(service, interface = Config.bindHost, port = Config.bindPort)
  }
}
