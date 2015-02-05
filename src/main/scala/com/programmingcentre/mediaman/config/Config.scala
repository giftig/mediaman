package com.programmingcentre.mediaman.config

import scala.collection.JavaConversions._

import com.typesafe.config.{ConfigFactory, ConfigObject}

/**
 * Loads in configuration settings from resources (application.conf) using typesafe ConfigFactory
 */
object Config {
  /**
   * Turn a humanised size into an Int number of bytes. Size must be in the format nX, where X is
   * a single letter [B, K, M, G] representing a multiplier. eg. 100K or 1G
   */
  private def parseSize(size: String): Int = {
    // The power of 1024 to which to multiply the value
    val power: Int = "BKMG".indexOf(size.last)

    if (power == -1) {
      throw new IllegalArgumentException(s"Bad size: $size")
    }

    val value = size.init.toInt
    math.pow(1024, power).asInstanceOf[Int] * value
  }

  // Check if mediaman.environment is set, and if it is, use the right config file
  // Otherwise just stick with application.conf
  val environment = Option(System.getProperty("mediaman.environment"))
  private val config = if (environment.isDefined) {
    ConfigFactory.load(environment.get)
  } else {
    ConfigFactory.load
  }

  val bindHost = config.getString("service.host")
  val bindPort = config.getInt("service.port")
  val adminBindHost = config.getString("admin.host")
  val adminBindPort = config.getInt("admin.port")

  val serviceName = config.getString("service.name")
  val uploadChunkSize: Int = parseSize(config.getString("service.upload_chunk_size"))

  val mediaPath = config.getString("media.path")
  val allowedMediaEncodings = config.getStringList("media.allowed_encodings")
  val programmePattern = config.getString("media.filename_patterns.programme").r

  // Max episode size, in bytes
  val maxEpisodeSize: Int = parseSize(config.getString("media.max_sizes.tv"))

  // Mappings of usernames to plaintext passwords, for Spray's BasicAuth.
  // Pretty crap in terms of both security and scaling, but this can be amended if it's ever
  // used in an environment where such things matter
  val authorisedUploaders: ConfigObject = config.getObject("auth.users.upload")
  val authorisedDownloaders: ConfigObject = config.getObject("auth.users.download")

  val corsAllowCredentials = config.getBoolean("service.cors.allow_credentials")

  // Grab origins from a string like "*" or
  // "http://localhost:5000 | http://mydomain.com | http://192.168.3.14:1234"
  val corsAllowOrigins: Array[String] = {
    config.getString("service.cors.allow_origin") match {
      case "*" => Array("*")
      case multiple: String => multiple.split('|') map { _.trim }
    }
  }

  val corsAllowHeaders = config.getStringList("service.cors.allow_headers").toList

  val chunkingDir = new java.io.File(config.getString("service.file_chunking.path"))
}
