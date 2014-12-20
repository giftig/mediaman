package com.programmingcentre.utils

import akka.actor
import akka.io.{IO => AkkaIO}
import spray.can.Http
import spray.routing.HttpService


object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = actor.ActorSystem()

    // FIXME: Service name "media-management" should be in config
    val service = system.actorOf(actor.Props[Service], "media-management")

    // FIXME: Interface and port should be in config
    AkkaIO(Http) ! Http.Bind(service, interface = "localhost", port = 8000)
  }
}
