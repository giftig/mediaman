package com.programmingcentre.mediaman.media

import java.io.File
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import com.programmingcentre.mediaman.config.Config

/**
 * This spec superclass should be used by any suite which needs to write directories / files using
 * Programme and Episode instances. It ensures thatt he root media path is created before running,
 * and cleans out all the created programmes / episodes when complete.
 *
 * Each test should ensure that it uses non-conflicting programmes / episodes as needed, though,
 * as this is only done after the whole suite executes, so that tests may be parallelised
 */
abstract class FileWritingSpec extends FlatSpec with BeforeAndAfterAll {
  val workingDirectory = new File(Config.mediaPath)

  /**
   * Ensure the media path exists before running tests
   */
  override protected def beforeAll(): Unit = workingDirectory.mkdir

  /**
   * Delete all programmes and episodes after tests have run, to clean up for the next run
   *
   * It's worth noting that this method isn't really specific to the programme/episode directory
   * structure, and will clear the workingDirectory two levels deep, so it can be used for
   * things like the ChunkedFileHandler as well.
   */
  override protected def afterAll(): Unit = {
    val programmes = workingDirectory.listFiles
    val episodes = programmes flatMap { p: File => p.listFiles }

    episodes foreach { _.delete }
    programmes foreach { _.delete }
  }
}
