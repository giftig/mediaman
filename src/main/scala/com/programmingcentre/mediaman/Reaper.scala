package com.programmingcentre.mediaman

import akka.actor.{Actor, ActorRef, PoisonPill, PossiblyHarmful, Terminated}
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import scala.collection.mutable.ArrayBuffer

object Reaper {
  // A message used to tell the Reaper to watch something
  case class Watch(actorRef: ActorRef)
  object KillAll extends PossiblyHarmful
}

/**
 * An Akka Actor which watches a collection of ActorRefs and triggers an action when all of the
 * targets have died.
 */
abstract class Reaper extends Actor {
  val logger = LoggerFactory.getLogger("reaper").asInstanceOf[Logger]
  val targets = ArrayBuffer.empty[ActorRef]

  /**
   * Define something to happen when all the targets have been "reaped"
   */
  def allTargetsDead(): Unit

  final def receive = {
    // Register the Reaper as a monitor for ref, as add it to targets
    case Reaper.Watch(actorRef) => {
      context.watch(actorRef)
      targets += actorRef
      logger.info(s"Watching $actorRef. Watching ${targets.length} targets.")
    }

    // Kill all targets by sending them PoisonPills
    case Reaper.KillAll => {
      logger.info("Received kill all instruction; killing targets.")
      targets foreach { _ ! PoisonPill }
    }

    // Received to tell us an actor we were watching died
    case Terminated(actorRef) => {
      targets -= actorRef
      logger.warn(s"Target $actorRef died! ${targets.length} targets left.")
      if (targets.isEmpty) allTargetsDead()
    }
  }
}

/**
 * A reaper which shuts down the actor system when its targets are eliminated
 */
class DeadlyReaper extends Reaper {
  def allTargetsDead() = {
    logger.warn("Terminating service...")
    context.system.shutdown
  }
}
