package logic.core

import akka.actor.Actor

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait LibActor extends Actor {
    val config: Config
}
