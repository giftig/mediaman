package com.programmingcentre.utils.media

import scala.collection.mutable.{Set => MSet}
import scala.collection.JavaConversions.asScalaSet

import com.typesafe.config.ConfigValue
import spray.http.{BasicHttpCredentials, HttpRequest}
import spray.httpx.RequestBuilding

import com.programmingcentre.utils.config.Config

/**
 * A mixin for Specs which require the ability to add BasicAuth credentials to requests
 */
trait BasicAuth extends RequestBuilding {
  /**
   * Convert a java.util.Map.Entry[String, ConfigValue] to a (String, String) tuple
   */
  private def configEntryToTuple(e: java.util.Map.Entry[String, ConfigValue]): (String, String) = {
    (e.getKey, e.getValue.unwrapped.asInstanceOf[String])
  }

  private val authorisedUploaders: MSet[Tuple2[String, String]] = {
    Config.authorisedUploaders.entrySet.map(configEntryToTuple _)
  }
  private val authorisedDownloaders: MSet[Tuple2[String, String]] = {
    Config.authorisedDownloaders.entrySet.map(configEntryToTuple _)
  }
  private val uploaderDetails = authorisedUploaders.head
  private val downloaderDetails = authorisedDownloaders.find(!authorisedUploaders.contains(_)).get

  private val uploaderAuth = BasicHttpCredentials(uploaderDetails._1, uploaderDetails._2)
  private val downloaderAuth = BasicHttpCredentials(downloaderDetails._1, downloaderDetails._2)
  private val invalidAuth = BasicHttpCredentials("jonsnow", "youknownothing")

  /**
   * Add BasicHttpCredentials to the given request; use valid = false to test invalid credentials.
   * This is curried so that it returns a function (HttpRequest) => HttpRequest, which
   * means the standard ~> operator can be applied to it.
   */
  protected def authorise(credType: String = "UPLOADER")(request: HttpRequest): HttpRequest = {
    request ~> addCredentials(
      credType match {
        case "UPLOADER" => uploaderAuth
        case "DOWNLOADER" => downloaderAuth
        case "INVALID" => invalidAuth
      }
    )
  }
}
