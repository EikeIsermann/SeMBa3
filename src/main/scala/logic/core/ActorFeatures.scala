package logic.core

import akka.actor.{Actor, ActorRef, Props, Stash}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */


trait ActorFeatures extends Actor {


  def wrappedReceive: Receive
  /*
  def wrappedBecome(r: Receive) = {
    wrappedReceive = r
  }
  */

  override def receive: Receive = {
    case x => if (wrappedReceive.isDefinedAt(x)) wrappedReceive(x) else unhandled(x)
  }

}
