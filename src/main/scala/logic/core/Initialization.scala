package logic.core

import akka.actor.{Actor, ActorRef, Props, Stash}
import utilities.debug.DC

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
case class InitializationRequest()
case class InitializationComplete(ref: ActorRef)

trait Initialization  extends Actor with Stash {
  var initializingFeatures: Set[ActorRef] = Set[ActorRef]()

  override def preStart(): Unit = {
    context.become(initialReceive())
    initialization()
    super.preStart()
  }

  def initialization() = {

  }

  def initializeFeature(props: Props, name: String): ActorRef = {
    val ref = context.actorOf(props, name)
    initializingFeatures = initializingFeatures + ref
    ref
  }

  def initialReceive(): Receive = {
    case initMsg: InitializationComplete => {
      println(context.sender() + " initialized")
      completeInit(initMsg)
    }

    //TODO add handling for incoming messages before initialization is complete
    case _ => stash()
  }

  def completeInit(initMsg: InitializationComplete) = {
    initializingFeatures = initializingFeatures - initMsg.ref
    if (initializingFeatures.isEmpty) {
      unstashAll()
      context.unbecome()
    }
  }
}