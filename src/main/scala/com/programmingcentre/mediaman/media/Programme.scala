package com.programmingcentre.mediaman.media

import java.io.{File, FileOutputStream}
import java.net.URLDecoder
import scala.collection.mutable.{Map => MutableMap}

import spray.http.HttpData
import spray.httpx.unmarshalling.{Deserialized, Deserializer, MalformedContent}
import spray.json._

import com.programmingcentre.mediaman.Main.logger
import com.programmingcentre.mediaman.config.Config
import com.programmingcentre.mediaman.utils.ChunkedFileHandler


/**
 * Defines the common API for programmes and episodes: they both have paths and a single
 * way of checking whether or not they already exist.
 */
trait Media {
  def file: File
  def exists: Boolean = file.exists
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

object ProgrammeJSONProtocol extends DefaultJsonProtocol {
  /**
   * Serialise a ChunkedEpisodeRequest to/from JSON in the following format:
   *
   * {
   *   "programme": {
   *     "programme": String,
   *     "season": Int,
   *     "episode": Int,
   *     "encoding": String
   *   },
   *   "file": {
   *     "size": Long,
   *     "checksum": String
   *   }
   * }
   */
  implicit object ChunkedEpisodeFormat extends RootJsonFormat[ChunkedEpisodeRequest] {
    def read(value: JsValue) = value.asJsObject.getFields("programme", "file") match {
      case Seq(progData: JsObject, fileData: JsObject) => {
        val episode = progData.getFields("programme", "season", "episode", "encoding") match {
          case Seq(
            JsString(prog),
            JsNumber(seasonNum),
            JsNumber(episodeNum),
            JsString(encoding)
          ) => new Episode(new Programme(prog), seasonNum.toInt, episodeNum.toInt, Some(encoding))
          case _ => throw new DeserializationException("Programme data was malformed")
        }

        val chunker = fileData.getFields("size", "checksum") match {
          case Seq(JsNumber(size), JsString(checksum)) => {
            if ("^[0-9a-f]{40}$".r.findFirstMatchIn(checksum.toString).isEmpty) {
              throw new DeserializationException("Must provide a sha1 checksum")
            }
            new ChunkedFileHandler(checksum.toString, size.toLong)
          }
          case _ => throw new DeserializationException("Must provide filesize and sha1 checksum")
        }

        ChunkedEpisodeRequest(episode, chunker)
      }
      case _ => throw new DeserializationException("Should contain programme and file objects")
    }

    def write(request: ChunkedEpisodeRequest) = JsObject(
      "programme" -> JsObject(
        "programme" -> JsString(request.episode.programme.name),
        "season" -> JsNumber(request.episode.season),
        "episode" -> JsNumber(request.episode.episode),
        "encoding" -> (request.episode.encoding.map { JsString(_) } getOrElse JsNull)
      ),
      "file" -> JsObject(
        "size" -> JsNumber(request.chunkHandler.size),
        "checksum" -> JsString(request.chunkHandler.checksum)
      )
    )
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
  override def file: File = new File(s"${Config.mediaPath}/$name")
  def save: Boolean = file.mkdir
}


/**
 * Represents an episode of a TV programme. An encoding must be provided if this Episode is going
 * to be saved to disk, and file / filename will not be available without one.
 */
class Episode(
  val programme: Programme,
  val season: Int,
  val episode: Int,
  val encoding: Option[String] = None
) extends Media {
  if (!programme.exists) {
    throw new NoSuchProgrammeException(s"Programme '${programme.name}' does not exist")
  }

  val name: String = f"S$season%02d E$episode%02d"
  val filename: Option[String] = if (encoding.isDefined) Some(f"$name.${encoding.get}") else None

  override def file: File = {
    if (filename.isDefined) {
      new File(programme.file, filename.get)
    } else {
      throw new RuntimeException("Can't operate on a file with no encoding")
    }
  }

  /**
   * Find paths to all copies of this episode, with any encoding
   *
   * @return A map of encodings to files. Empty if none exist
   */
  def existingEncodings: Map[String, File] = {
    // Collect files in the programme directory which match our episode
    val files = programme.file.listFiles collect {
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
    val f = new FileOutputStream(file)
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

/**
 * Ties an Episode to a ChunkedFileHandler
 */
case class ChunkedEpisodeRequest(val episode: Episode, val chunkHandler: ChunkedFileHandler)
