package com.programmingcentre.utils.config

import com.typesafe.config.ConfigFactory

/**
 * Loads in configuration settings from resources (application.conf) using typesafe ConfigFactory
 */
object Config {
  private val config = ConfigFactory.load

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

  val bindHost = config.getString("service.host")
  val bindPort = config.getInt("service.port")

  val serviceName = config.getString("service.name")
  val uploadChunkSize: Int = parseSize(config.getString("service.upload_chunk_size"))

  val mediaPath = config.getString("media.path")
  val allowedMediaEncodings = config.getStringList("media.allowed_encodings")

  // Max episode size, in bytes
  val maxEpisodeSize: Int = parseSize(config.getString("media.max_sizes.tv"))
}
