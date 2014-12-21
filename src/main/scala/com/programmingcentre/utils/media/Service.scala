package com.programmingcentre.utils

import akka.actor._
import akka.io.IO
import ch.qos.logback.classic.{Level => LogLevel, Logger}
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.http.{ContentType, HttpHeaders, HttpResponse, MediaType}
import spray.routing.HttpService


/**
 * Trait to define an API for the media management service
 */
trait ServiceAPI extends HttpService {
  val logger = LoggerFactory.getLogger("mediaman").asInstanceOf[Logger]

  def createEpisode = {
  }
  def ping = path("ping") {
    get { complete {
      logger.debug("Ping was hit with a GET request")
      "pong!"
    }} ~
    post { complete {
      logger.debug("Ping was hit with a POST request")
      <p><strong>Oi!</strong> y u post meh?</p>
    }}
  }
  def pong = path("pong") {
    get { complete {
      (404, "Pong? Wtf is a pong?")
    }}
  }

  def rootRoute = ping ~ pong
}


/**
 * Concrete service for media management, which extends an Akka Actor to provide an HTTP interface
 * via Akka and Spray.
 */
class Service extends Actor with ServiceAPI {
  def actorRefFactory = context
  def receive = runRoute(rootRoute)
}
