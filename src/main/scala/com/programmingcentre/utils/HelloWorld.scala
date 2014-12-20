package com.programmingcentre.utils


object HelloWorld {
  def main(args: Array[String]): Unit = {
    println("Hello, world!")

    val out = new java.io.PrintWriter(new java.io.File("/tmp/successkid.txt"))
    out.println((1 to 10) map { _ * 10 } mkString(", "))
    out.close
  }
}
