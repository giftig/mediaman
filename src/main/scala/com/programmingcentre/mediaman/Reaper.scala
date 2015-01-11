package com.programmingcentre.mediaman

import akka.actor.{Actor, ActorRef, PoisonPill, PossiblyHarmful, Terminated}
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
    }

    // Kill all targets by sending them PoisonPills
    case Reaper.KillAll => targets foreach { _ ! PoisonPill }

    // Received to tell us an actor we were watching died
    case Terminated(actorRef) => {
      targets -= actorRef
      if (targets.isEmpty) allTargetsDead()
    }
  }
}

/**
 * A reaper which shuts down the actor system when its targets are eliminated
 */
class DeadlyReaper extends Reaper {
  def allTargetsDead() = context.system.shutdown
}
