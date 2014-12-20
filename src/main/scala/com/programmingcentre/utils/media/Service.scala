package com.programmingcentre.utils

import akka.actor._
import akka.io.IO
import ch.qos.logback.classic.{Level => LogLevel, Logger}
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.routing.HttpService


/**
 * Trait to define an API for the media management service
 */
trait ServiceAPI extends HttpService {
  val logger = LoggerFactory.getLogger("mediaman").asInstanceOf[Logger]

  def pingRoute = path("ping") { get(complete {
    logger.info("Ping endpoint hit")
    "pong!"
  })}
  def pongRoute = path("pong") {
    get(complete("pong!?"))
  }

  def rootRoute = pingRoute ~ pongRoute
}


/**
 * Concrete service for media management, which extends an Akka Actor to provide an HTTP interface
 * via Akka and Spray.
 */
class Service extends Actor with ServiceAPI {
  def actorRefFactory = context
  def receive = runRoute(rootRoute)
}
