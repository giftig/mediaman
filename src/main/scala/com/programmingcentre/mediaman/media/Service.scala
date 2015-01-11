package com.programmingcentre.mediaman.media

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor._
import akka.io.IO
import ch.qos.logback.classic.{Level => LogLevel, Logger}
import org.slf4j.LoggerFactory
import spray.can.Http
import spray.http.{BodyPart, ContentType, HttpHeaders, HttpResponse, MediaType}
import spray.routing.HttpService
import spray.routing.authentication.{BasicAuth, UserPass}

import com.programmingcentre.mediaman.config.Config
import com.programmingcentre.mediaman.media.Deserialisers._
import com.programmingcentre.mediaman.utils.CorsSupport


/**
 * Trait to define an API for the media management service
 */
trait ServiceAPI extends HttpService with CorsSupport {
  val logger = LoggerFactory.getLogger("mediaman").asInstanceOf[Logger]
  private val downloadAuth = BasicAuth(
    realm = Config.serviceName,
    config = Config.authorisedDownloaders.toConfig,
    createUser = (allowedUser: UserPass) => allowedUser.user
  )
  private val uploadAuth = BasicAuth(
    realm = Config.serviceName,
    config = Config.authorisedUploaders.toConfig,
    createUser = (allowedUser: UserPass) => allowedUser.user
  )


  /**
   * Handle requests dealing with TV Programmes
   */
  def handleProgramme = path("programme") { cors {
    post { formFields("name".as[Programme]) { prog => authenticate(uploadAuth) {
      username => complete {
        // If the programme already exists, give them a 409 (update conflict)
        if (prog.exists) {
          (409, "Conflict")
        } else if (prog.save) {
          (200, "OK")
        } else {
          (500, "Internal Server Error")
        }
      }
    }}}
  }}

  /**
   * Handle requests dealing with Episodes of TV Programmes
   */
  def handleEpisode = path("episode") { cors {
    put {
      formFields(
        "programme".as[Programme], "season".as[Int], "episode".as[Int], "file".as[Option[BodyPart]]
      ) { (programme, seasonNum, episodeNum, body) => authenticate(uploadAuth) {
        username => complete {
          body match {
            case Some(fileInfo: BodyPart) => {
              // Check the file extension
              fileInfo.filename.getOrElse("").split('.').lastOption match {
                case Some(format: String) => {
                  try {
                    new Episode(
                      programme, seasonNum, episodeNum, Some(format)
                    ).save(fileInfo.entity.data)
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
        }
      }}
    } ~
    get { parameters("programme".as[Programme], "season".as[Int], "episode".as[Int]) {
      (programme, seasonNum, episodeNum) => authenticate(downloadAuth) { username => {
        val episode = new Episode(programme, seasonNum, episodeNum)

        episode.existingEncodings.values.headOption match {
          case Some(f) => getFromFile(f)
          case None => complete((404, "Not Found"))
        }
      }}
    }}
  }}

  def mainRoute = handleProgramme ~ handleEpisode
}


/**
 * Concrete service for media management, which extends an Akka Actor to provide an HTTP interface
 * via Akka and Spray.
 */
class Service extends Actor with ServiceAPI {
  def actorRefFactory = context
  def receive = runRoute(mainRoute)
}
