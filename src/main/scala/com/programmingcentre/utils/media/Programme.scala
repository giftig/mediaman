package com.programmingcentre.utils.media

import spray.http.HttpData
import spray.httpx.unmarshalling.{Deserialized, Deserializer}

import com.programmingcentre.utils.config.Config


/**
 * Defines the common API for programmes and episodes: they both have paths and a single
 * way of checking whether or not they already exist.
 */
trait Media {
  def fullpath: String
  def exists: Boolean = new java.io.File(fullpath).exists
}


/**
 * Spray Deserialisers for objects which should be parsed from form data
 */
object Deserialisers {
  /**
   * Straightforward Programme deserialiser for Spray: simply construct a Programme from a String
   * name, ensuring it's been URL decoded (as UTF-8).
   */
  implicit object ProgrammeDeserialiser extends Deserializer[String, Programme] {
    override def apply(s: String): Deserialized[Programme] = {
      Right(new Programme(java.net.URLDecoder.decode(s, "UTF-8")))
    }
  }
}


/**
 * Represents a TV programme. Provides convenience methods for checking if the programme already
 * exists, and creating it if it doesn't.
 */
class Programme(val name: String) extends Media {
  def fullpath: String = s"${Config.mediaPath}/$name"

  def save: Boolean = new java.io.File(fullpath).mkdir
}


/**
 * Represents an episode of a TV programme
 */
class Episode(programme: Programme, season: Int, episode: Int, encoding: String) extends Media {
  if (!Config.allowedMediaEncodings.contains(encoding))
    throw new java.io.UnsupportedEncodingException(s"File type '$encoding' not allowed")

  if (!programme.exists)
    throw new NoSuchProgrammeException(s"Programme '${programme.name}' does not exist")

  def filename: String = f"S$season%02d E$episode%02d.$encoding"
  def fullpath: String = s"${programme.fullpath}/$filename"

  /**
   * Save the given Spray HttpData as this episode
   */
  def save(data: HttpData): Unit = {
    if (data.length > Config.maxEpisodeSize) {
      throw new FileTooLargeException("Size")
    }

    // Convert the data into a Stream of chunks and write each chunk to the episode file
    val f = new java.io.FileOutputStream(fullpath)
    data.toChunkStream(Config.uploadChunkSize) foreach {
      chunk: HttpData => f.write(chunk.toByteArray)
    }
    f.close
  }
}
