package com.programmingcentre.utils.media

import java.io.File
import org.scalatest._
import spray.http.HttpData

import com.programmingcentre.utils.config.Config


trait MediaMocker extends Media {
  var _exists = true
  override def exists: Boolean = _exists
}


class ProgrammeSpec extends FlatSpec {
  "A Programme" should "be constructed with a good filename" in {
    new Programme("Awesome TV programme")
  }

  it should "throw an IllegalArgumentException with a bad filename" in {
    intercept[IllegalArgumentException] {
      new Programme("Dodgy TV programme*:@")
    }
  }

  it should "throw an IllegalArgumentException with a DANGEROUS filename" in {
    intercept[IllegalArgumentException] {
      new Programme("../../lolihacku.sh")
    }
  }
}


class EpisodeSpec extends FileWritingSpec with Matchers {

  "An Episode" should "be constructed with an existing Programme" in {
    val prog = new Programme("Hodor: Revenge of the Starks") with MediaMocker
    prog._exists = true

    new Episode(prog, 1, 1)
  }

  it should "error if the programme doesn't exist" in {
    val prog = new Programme("Live Hard, Die Hodor") with MediaMocker
    prog._exists = false

    intercept[NoSuchProgrammeException] {
      new Episode(prog, 1, 1)
    }
  }

  it should "save a file properly" in {
    val data = "Hodor hodor!"
    assume(data.length <= Config.maxEpisodeSize)

    val prog = new Programme("Stark Naked: Eddard's Story")
    prog.save

    val ep = new Episode(prog, 1, 1, Some("avi"))

    assume(!ep.exists)
    ep.save(HttpData(data))

    ep.exists should be (true)
  }

  it should "not save without an encoding" in {
    val prog = new Programme("Arya Sure: An Interview with Sansa") with MediaMocker
    val ep = new Episode(prog, 1, 1, None)

    intercept[java.io.UnsupportedEncodingException] {
      ep.save(HttpData("Hodor hodor!"))
    }
  }

  it should "not save if the data is too long" in {
    val prog = new Programme("Meow meow: A Biography of Lady Stark") with MediaMocker
    val ep = new Episode(prog, 1, 1, Some("avi"))

    intercept[FileTooLargeException] {
      ep.save(HttpData("." * (Config.maxEpisodeSize + 1)))
    }
  }

  it should "not save if the encoding is unsupported" in {
    val encoding = "exe"
    assume(!Config.allowedMediaEncodings.contains(encoding))

    val prog = new Programme("Lord Snow and his Title of Irony") with MediaMocker
    val ep = new Episode(prog, 1, 1, Some(encoding))

    intercept[java.io.UnsupportedEncodingException] {
      ep.save(HttpData("Hodor hodor!"))
    }
  }

  it should "remove duplicate encodings on save" in {
    val prog = new Programme("Robb Plays With His Sword")
    prog.save

    val avi = new Episode(prog, 1, 1, Some("avi"))
    val mkv = new Episode(prog, 1, 1, Some("mkv"))

    avi.save(HttpData("Hodor hodor"))
    avi.exists should be (true)

    mkv.save(HttpData("Hodor hodor hodor"))
    avi.exists should be (false)
    mkv.exists should be (true)
  }

  it should "retrieve alternate encodings properly" in {
    val prog = new Programme("Bran's Final Climb")
    prog.save

    val files = List("S01 E01.avi", "S01 E01.exe", "S01 E01.mkv", "S01 E01.txt") map {
      e => new File(s"${prog.file.getCanonicalPath}/$e")
    }
    files foreach { _.createNewFile }

    val ep = new Episode(prog, 1, 1, None)
    val encodings = ep.existingEncodings

    encodings.size should be(files.length)
    files foreach { f: File => {
      encodings.contains(f.getName.takeRight(3)) should be (true)
    }}
  }
}


class ProgrammeDeserialiserSpec extends FlatSpec with Matchers {
  "ProgrammeDeserialiser" should "decode UTF-8" in {
    List("Sansa+Sucks+Balls", "Sansa%20Sucks%20Balls", "Sansa Sucks Balls") foreach { s => {
      val prog = Deserialisers.ProgrammeDeserialiser(s)

      prog match {
        case Right(p: Programme) => p.name should be ("Sansa Sucks Balls")
        case Left(_) => fail("The programme wasn't constructed")
      }
    }}
  }
}
