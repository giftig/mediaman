package com.programmingcentre.utils.media

import akka.actor
import java.io.File
import org.scalatest._
import scala.io.Source
import spray.http._
import spray.testkit.ScalatestRouteTest

import com.programmingcentre.utils.config.Config

object ServiceSpec {
  final val TEMP_UPLOAD_FILE_PREFIX = "mediaman-test-episode-upload"
  final val TEMP_DIR = "/tmp"

  private def formDataToBodyParts(fields: Seq[(String, String)]): Seq[BodyPart] = {
    fields map {
      field => BodyPart(HttpEntity(field._2), field._1)
    }
  }
  private def formDataToBodyParts(data: FormData): Seq[BodyPart] = {
    formDataToBodyParts(data.fields)
  }

  /**
   * Create a temporary file to use for PUTing to the service, with the given content and
   * extension.
   */
  private def createUploadFile(content: String, extension: String = ".txt"): File = {
    val tempDir = new File(ServiceSpec.TEMP_DIR)
    assume(tempDir.exists)

    // Unfortunately the API means I have to provide an actual file and upload it
    val fileUpload = File.createTempFile(TEMP_UPLOAD_FILE_PREFIX, extension, tempDir)
    val fileOut = new java.io.PrintWriter(fileUpload)
    fileOut.print(content)
    fileOut.close

    fileUpload
  }
}

class ServiceSpec extends FileWritingSpec with ScalatestRouteTest with ServiceAPI with Matchers {
  def actorRefFactory = system  // Connect the service API to the test ActorSystem

  /**
   * Ensure temporary files created by createUploadFile are deleted when the test suite finishes
   */
  override protected def afterAll(): Unit = {
    super.afterAll

    new File(ServiceSpec.TEMP_DIR).listFiles filter {
      _.getName.startsWith(ServiceSpec.TEMP_UPLOAD_FILE_PREFIX)
    } foreach { _.delete }
  }

  /**
   * Create a temporary file with the given content and return an HttpRequest to upload it as an
   * Episode of the given Programme.
   */
  private def createUploadRequest(
    programme: Programme,
    season: Int,
    episode: Int,
    content: String,
    format: String = ".txt"
  ): HttpRequest = {
    val file = ServiceSpec.createUploadFile(content, format)
    Put(
      "/episode",
      new MultipartFormData(
        ServiceSpec.formDataToBodyParts(Seq(
          ("programme", programme.name),
          ("season", season.toString),
          ("episode", episode.toString)
        )) :+
        BodyPart(file, "file")
      )
    )
  }

  "The programme handler" should "create a new TV programme and conflict if already existing" in {
    val request = Post("/programme", new FormData(Seq(("name", "Jeeves I: Serving Hodor"))))
    request ~> mainRoute ~> check { status.intValue should be (200) }
    request ~> mainRoute ~> check { status.intValue should be (409) }
  }

  "The episode handler" should "retrieve an episode" in {
    // Set up an episode to grab
    val content = "Hodor hodor hodor!!!!"
    val prog = new Programme("Jeeves II: the Aftermath")
    prog.save

    val ep = new Episode(prog, 1, 1, Some("txt"))
    ep.save(HttpData(content))

    // Now try to grab it
    val request = Get("/episode?programme=Jeeves+II:+the+Aftermath&season=1&episode=1")
    request ~> mainRoute ~> check {
      status.intValue should be (200)
      responseAs[String] should be (content)
    }
  }

  it should "accept a PUT file" in {
    // Set up a programme to which to put a file
    val prog = new Programme("Jeeves III: Revenge of the Waistcoat")
    prog.save

    // Now try to PUT our episode file into the programme
    val content = "Jeeves, fetch me my hunting shorts!"
    val request = createUploadRequest(prog, 1, 1, content)

    request ~> mainRoute ~> check { status.intValue should be (200) }
    Source.fromFile(s"${prog.fullpath}/S01 E01.txt").mkString should be (content)
  }

  it should "reject a PUT file with no extension" in {
    val prog = new Programme("Jenkins I: The Prodige")
    prog.save

    val request = createUploadRequest(prog, 1, 1, "Jenkins, it's all up to you now.", "")

    request ~> mainRoute ~> check { status.intValue should be (400) }
    new File(s"${prog.fullpath}").listFiles.isEmpty should be (true)
  }
}
