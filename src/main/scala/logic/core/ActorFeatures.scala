package logic.core

import akka.actor.{Actor, ActorRef, Props, Stash}
import akka.agent.Agent

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */


trait ActorFeatures extends Actor {

  def wrappedReceive: Receive = PartialFunction.empty
  /*
  def wrappedBecome(r: Receive) = {
    wrappedReceive = r
  }
  */

  override def receive: Receive = {
    case x => if (wrappedReceive.isDefinedAt(x)) wrappedReceive(x) else unhandled(x)
  }

}
