package com.programmingcentre.utils.media

import akka.actor._
import akka.io.IO
import ch.qos.logback.classic.{Level => LogLevel, Logger}
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.http.{BodyPart, ContentType, HttpHeaders, HttpResponse, MediaType}
import spray.routing.HttpService

import com.programmingcentre.utils.media.Deserialisers._


/**
 * Trait to define an API for the media management service
 */
trait ServiceAPI extends HttpService {
  val logger = LoggerFactory.getLogger("mediaman").asInstanceOf[Logger]

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

  /**
   * Handle requests dealing with TV Programmes
   */
  def handleProgramme = path("programme") {
    post { formFields("name".as[Programme]) { prog =>
      complete {
        // TODO: Do an auth check of some description here.
        // If the programme already exists, give them a 409 (update conflict)
        if (prog.exists) {
          (409, "Conflict")
        } else if (prog.save) {
          (200, "OK")
        } else {
          (500, "Internal Server Error")
        }
      }
    }}
  }

  /**
   * Handle requests dealing with Episodes of TV Programmes
   */
  def handleEpisode = path("episode") {
    put {
      formFields(
        "programme".as[Programme], "season".as[Int], "episode".as[Int], "file".as[Option[BodyPart]]
      ) { (programme, seasonNum, episodeNum, body) => complete {
        body match {
          case Some(fileInfo: BodyPart) => {
            // Check the file extension
            fileInfo.filename.get.split('.').lastOption match {
              case Some(format: String) => {
                try {
                  new Episode(programme, seasonNum, episodeNum, format).save(fileInfo.entity.data)
                  (200, "OK")
                } catch {
                  case e: java.io.UnsupportedEncodingException => (400, "Unsupported file type")
                  case e: FileTooLargeException => (400, "That file is too large")
                  case e: NoSuchProgrammeException => (404, "That programme does not exist")
                }
              }
              case None => (400, "Couldn't determine file type")
            }
          }
          case None => (400, "Missing file")
        }
      }}
    }
  }

  def mainRoute = ping ~ pong ~ handleProgramme ~ handleEpisode
}


/**
 * Concrete service for media management, which extends an Akka Actor to provide an HTTP interface
 * via Akka and Spray.
 */
class Service extends Actor with ServiceAPI {
  def actorRefFactory = context
  def receive = runRoute(mainRoute)
}
