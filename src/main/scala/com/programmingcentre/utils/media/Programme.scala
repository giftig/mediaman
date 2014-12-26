package com.programmingcentre.utils.media

import java.io.{File, FileOutputStream}
import java.net.URLDecoder

import spray.http.HttpData
import spray.httpx.unmarshalling.{Deserialized, Deserializer, MalformedContent}

import com.programmingcentre.utils.config.Config
import com.programmingcentre.utils.Main.logger


/**
 * Defines the common API for programmes and episodes: they both have paths and a single
 * way of checking whether or not they already exist.
 */
trait Media {
  def fullpath: String
  def exists: Boolean = new File(fullpath).exists
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
      try {
        val prog = new Programme(URLDecoder.decode(s, "UTF-8"))
        Right(prog)
      } catch {
        case e: Throwable => Left(new MalformedContent(e.getMessage, Some(e)))
      }
    }
  }
}


/**
 * Represents a TV programme. Provides convenience methods for checking if the programme already
 * exists, and creating it if it doesn't.
 */
class Programme(val name: String) extends Media {
  if (Config.programmePattern.findAllMatchIn(name).isEmpty) {
    logger.error(s"""The programme name "$name" does not match ${Config.programmePattern}""")
    throw new IllegalArgumentException("That programme name contains illegal characters")
  }
  def fullpath: String = s"${Config.mediaPath}/$name"

  def save: Boolean = new File(fullpath).mkdir
}


/**
 * Represents an episode of a TV programme. An encoding must be provided if this Episode is going
 * to be saved to disk, and fullpath / filename will not be available without one.
 */
class Episode(
  programme: Programme,
  season: Int,
  episode: Int,
  encoding: Option[String] = None
) extends Media {
  if (!programme.exists) {
    throw new NoSuchProgrammeException(s"Programme '${programme.name}' does not exist")
  }

  def name: String = f"S$season%02d E$episode%02d"
  def filename: String = f"$name.${encoding.get}"
  def fullpath: String = s"${programme.fullpath}/$filename"

  /**
   * Find paths to all copies of this episode, with any encoding
   *
   * @return A map of encodings to files. Empty if none exist
   */
  def existingEncodings: Map[String, File] = {
    // Collect files in the programme directory which match our episode
    val files = new File(programme.fullpath).listFiles collect {
      case f if f.getName.startsWith(name) => f
    }
    val extensions = files map { _.getName.takeRight(3) }
    (extensions zip files).toMap
  }

  /**
   * Save the given Spray HttpData as this episode
   */
  def save(data: HttpData): Unit = {
    if (encoding.isEmpty) {
      throw new java.io.UnsupportedEncodingException("Can't write an episode with no encoding")
    }
    if (!Config.allowedMediaEncodings.contains(encoding.get)) {
      logger.error(s"Bad encoding in episode: '${encoding.get}'")
      throw new java.io.UnsupportedEncodingException(s"File type '${encoding.get}' not allowed")
    }
    if (data.length > Config.maxEpisodeSize) {
      logger.error(s"Large file upload attempt: (${data.length} bytes)")
      throw new FileTooLargeException("Episode size must be < ${Config.maxEpisodeSize} bytes")
    }

    // Convert the data into a Stream of chunks and write each chunk to the episode file
    val f = new FileOutputStream(fullpath)
    data.toChunkStream(Config.uploadChunkSize) foreach {
      chunk: HttpData => f.write(chunk.toByteArray)
    }
    f.close

    // Make sure duplicate files are gone
    existingEncodings filterKeys { _ != encoding.get } foreach {
      item => {
        logger.info(s"Deleting duplicate encoding ${item._1} for episode $name")
        item._2.delete
      }
    }
  }
}
