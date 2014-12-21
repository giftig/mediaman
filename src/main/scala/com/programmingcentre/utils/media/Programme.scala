package com.programmingcentre.utils.media

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
 * Represents a TV programme. Provides convenience methods for checking if the programme already
 * exists, and creating it if it doesn't.
 */
class Programme(name: String) extends Media {
  def fullpath: String = s"${Config.mediaPath}/$name"
  override def toString = fullpath

  def save: Boolean = !exists && new java.io.File(fullpath).mkdir
}


/**
 * Represents an episode of a TV programme
 */
class Episode(programme: Programme, season: Int, episode: Int, encoding: String) extends Media {
  def filename: String = f"S$season%02d E$episode%02d.$encoding"
  def fullpath: String = s"$programme/$filename"
}
