package com.programmingcentre.mediaman.utils

import java.io.{File, FileOutputStream}
import java.nio.file.{Files, Paths}
import java.security.MessageDigest
import java.util.UUID
import scala.io.Source

import com.programmingcentre.mediaman.config.Config

class BadChecksumException extends Exception

/**
 * The purpose of this class is to essentially get around the fact that Spray doesn't support
 * proper chunking of requests: the options were essentially either implement it myself, including
 * manual header parsing, etc., or allow files to be uploaded with multiple requests, one file
 * chunk per request. This is an implementation of the latter solution.
 */
trait Chunking {
  // The compression and file encoding used for this file
  val compression: String

  val checksum, checksumAlgorithm: String

  val size: Long
  val chunkSize: Int

  // The key used to access this file
  val key: String
  val dir: File

  lazy val numChunks = math.ceil(size / chunkSize.toDouble).toInt

  /**
   * Get a list of files referring to chunks for this upload
   */
  protected def savedChunkFiles: Array[File] = {
    dir.listFiles filter { f => "^[0-9]+$".r.findFirstMatchIn(f.getName).isDefined }
  }

  /**
   * Figure out which chunks have been saved to the filesystem.
   */
  def savedChunks: Array[Int] = savedChunkFiles.map(_.getName.toInt)

  /**
   * Check if the given chunk is saved to the filesystem yet
   */
  def isChunkSaved(n: Int): Boolean = new File(dir, f"$n%04d").exists

  /**
   * Attempt to save a chunk to the correct file
   */
  def saveChunk(n: Int, data: Array[Byte]): Unit = {
    if (n < 0 || n >= numChunks) {
      throw new IllegalArgumentException(s"$n is an invalid chunk number")
    }

    val f = new File(dir, f"$n%04d")
    val out = new FileOutputStream(f)
    out.write(data)
    out.close
  }

  /**
   * Attempt to collect all the chunks into a file and return the resulting File. Returns None if
   * there are chunks missing.
   */
  def collect: Option[File] = {
    val chunks = savedChunkFiles
    val assembledFile = new File(dir, "assembled")

    (chunks.length == numChunks, assembledFile.exists) match {
      case (true, false) => {
        // Concatenate all the chunks together into the assembledFile
        // Run a checksum while we're at it
        val out = new FileOutputStream(assembledFile)
        val md = MessageDigest.getInstance(checksumAlgorithm)
        chunks.sorted.foreach { chunk: File => {
          val data = Files.readAllBytes(chunk.toPath)
          md.update(data)
          out.write(data)
        }}
        out.close

        // Compare hex checksums
        if (md.digest.map(b => f"$b%02x").mkString != checksum) {
          throw new BadChecksumException
        }

        // Now delete the chunks
        chunks foreach { _.delete }

        Some(assembledFile)
      }
      case (_, true) => Some(assembledFile)
      case _ => None
    }
  }
}

class ChunkedFileHandler(
  val checksum: String,
  val size: Long
) extends Chunking {
  val compression = "zip"
  val checksumAlgorithm = "SHA-1"
  val chunkSize = Config.uploadChunkSize
  val key = UUID.randomUUID.toString
  val dir = new File(Config.chunkingDir, key)
  dir.mkdir
}
