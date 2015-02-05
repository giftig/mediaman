package com.programmingcentre.mediaman.utils

import java.io.File
import java.nio.file.Files

import org.scalatest._

import com.programmingcentre.mediaman.config.Config
import com.programmingcentre.mediaman.media.FileWritingSpec


class ChunkingSpec extends FileWritingSpec with Matchers {
  override val workingDirectory = Config.chunkingDir

  val goodChecksum = "4b94287d001bc19d133233a57b64443504e3f08f"  // matches getFileData
  val badChecksum = "cfecc6538156c6ed944860f29d0c329d8afb9389"   // "hodor hodor hodor"

  def getHandler = new ChunkedFileHandler(goodChecksum, 10000)
  def getHandler(checksum: String) = new ChunkedFileHandler(checksum, 10000)

  // Convenience method to generate some data for a full file to be chunked on demand,
  // to avoid using memory unnecessarily. Just produce a string of 10,000 1s as bytes
  def getFileData = ("1" * 10000).getBytes

  "A chunked file handler" should "create its chunk directory" in {
    getHandler.dir.exists should be (true)
  }

  it should "calculate the number of chunks properly" in {
    val handler = getHandler
    val checkNumChunks: Function[Int, Unit] = ((fileSize: Int) => {
      val handler = new ChunkedFileHandler("", fileSize)
      handler.numChunks should be (math.ceil(fileSize / Config.uploadChunkSize.toDouble).toInt)
    })

    (1 to 10) map { _ * Config.uploadChunkSize / 2 } foreach(checkNumChunks)
  }

  it should "allowing saving chunks" in {
    val chunkData = "Hodor hodor hodor!".getBytes
    val handler = getHandler
    handler.saveChunk(0, chunkData)
    val f = new File(handler.dir, "0000")

    f.exists should be (true)
    Files.readAllBytes(f.toPath) should be (chunkData)
  }

  it should "error when trying to save a bad chunk number" in {
    val chunkData = "Valar Morghulis".getBytes
    val handler = getHandler

    intercept[IllegalArgumentException] { handler.saveChunk(-1, chunkData) }
    intercept[IllegalArgumentException] { handler.saveChunk(handler.numChunks + 1, chunkData) }
  }

  it should "recognise when its chunks exist" in {
    val handler = getHandler
    val chunks = 1 to 10

    // Check savedChunks
    handler.savedChunks should be (Array.empty[Int])

    // Check isChunkSaved
    chunks foreach { n => {
      handler.isChunkSaved(n) should be (false)
      new File(handler.dir, f"$n%04d").createNewFile
      handler.isChunkSaved(n) should be (true)
    }}

    handler.savedChunks.sorted should be (chunks.toArray)
  }

  it should "return the assembled file if it already exists" in {
    val handler = getHandler
    new File(handler.dir, "assembled").createNewFile

    handler.collect.isDefined should be (true)
  }

  it should "collect the file if all chunks are available" in {
    val handler = getHandler
    val fileData = getFileData
    assume(Config.uploadChunkSize < fileData.length)

    // Save all chunks
    fileData.grouped(Config.uploadChunkSize).zipWithIndex foreach {
      case (d: Array[Byte], i: Int) => handler.saveChunk(i, d)
    }

    val result: Option[File] = handler.collect

    result.isDefined should be (true)

    val f: File = result.get
    f.getName should be ("assembled")
    Files.readAllBytes(f.toPath) should be (fileData)
    handler.savedChunks should be (Array.empty[Int])
  }

  it should "fail to collect the file if some chunks are missing" in {
    val handler = getHandler
    assume(handler.numChunks > 1)

    handler.saveChunk(0, "Valar Dohaeris".getBytes)
    handler.collect.isDefined should be (false)
  }

  it should "error if the file's checksum was incorrect" in {
    val handler = getHandler(badChecksum)
    getFileData.grouped(Config.uploadChunkSize).zipWithIndex foreach {
      case (d: Array[Byte], i: Int) => handler.saveChunk(i, d)
    }
    intercept[BadChecksumException] { handler.collect }
  }
}
