package com.programmingcentre.utils.config

import com.typesafe.config.ConfigFactory

/**
 * Loads in configuration settings from resources (application.conf) using typesafe ConfigFactory
 */
object Config {
  private val config = ConfigFactory.load

  val bindHost = config.getString("service.host")
  val bindPort = config.getInt("service.port")

  val serviceName = config.getString("service.name")

  val mediaPath = config.getString("media.path")
  val allowedMediaEncodings = config.getStringList("media.allowed_encodings")
}
