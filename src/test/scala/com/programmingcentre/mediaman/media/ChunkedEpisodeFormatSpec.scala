package com.programmingcentre.mediaman.media

import org.scalatest._
import spray.json._

import com.programmingcentre.mediaman.config.Config
import com.programmingcentre.mediaman.utils.ChunkedFileHandler
import com.programmingcentre.mediaman.media.ProgrammeJSONProtocol.ChunkedEpisodeFormat

class ChunkedEpisodeFormatSpec extends FileWritingSpec with Matchers {

  "The chunked episode format" should "read well-formed JSON" in {
    val prog = new Programme("The Sky Door")
    prog.save
    val season = 1
    val episode = 3
    val encoding = "mp4"
    val fileSize = 1024000L
    val checksum = "cfecc6538156c6ed944860f29d0c329d8afb9389"

    val data = s"""
      {
        "programme": {
          "programme": "${prog.name}",
          "season": $season,
          "episode": $episode,
          "encoding": "$encoding"
        },
        "file": {
          "size": $fileSize,
          "checksum": "$checksum"
        }
      }
    """.parseJson

    val request = ChunkedEpisodeFormat.read(data)
    request.episode.programme.name should be (prog.name)
    request.episode.season should be (season)
    request.episode.episode should be (episode)
    request.episode.encoding.isDefined should be (true)
    request.episode.encoding.get should be (encoding)
    request.chunkHandler.size should be (fileSize)
    request.chunkHandler.checksum should be (checksum)
  }

  it should "write JSON properly" in {
    val prog = new Programme("The Sky Door II: After the Splat")
    prog.save
    val season = 2
    val episode = 7
    val encoding = "wmv"
    val fileSize = 512000L
    val checksum = "4b94287d001bc19d133233a57b64443504e3f08f"

    val request = ChunkedEpisodeRequest(
      new Episode(prog, season, episode, Some(encoding)),
      new ChunkedFileHandler(checksum, fileSize)
    )

    val expected = s"""
      {
        "programme": {
          "programme": "${prog.name}",
          "season": $season,
          "episode": $episode,
          "encoding": "$encoding"
        },
        "file": {
          "size": $fileSize,
          "checksum": "$checksum"
        }
      }
    """.parseJson
    val actual = ChunkedEpisodeFormat.write(request)

    actual should be (expected)
  }
}
