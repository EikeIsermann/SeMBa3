package core

import akka.actor.{Actor, ActorRef, Props, Stash}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
case class InitializationRequest()
case class InitializationComplete(ref: ActorRef)


trait ActorFeatures extends Actor with Stash {
   var initializingFeatures: Set[ActorRef]

  override def preStart(): Unit = {
    context.become(initialReceive())
    initialization()
    super.preStart()
  }

  def initialization() = {

  }

  def initializeFeature(props: Props): ActorRef = {
    var ref = context.actorOf(props)
    initializingFeatures = initializingFeatures + ref
    ref
  }

  def initialReceive(): Receive = {
    case initMsg: InitializationComplete => {
      completeInit(initMsg)
    }

    //TODO add handling for incoming messages before initialization is complete
    case _ =>  stash()
  }

  def completeInit(initMsg: InitializationComplete) = {
    initializingFeatures = initializingFeatures - initMsg.ref
    if(initializingFeatures.isEmpty){
      unstashAll()
      context.unbecome()
    }
  }


  var wrappedReceive: Receive  = {
    case msg => unhandled(msg)
  }


  def wrappedBecome(r: Receive) = {
    wrappedReceive = r
  }


  override def receive: Receive = {
    case x => if (wrappedReceive.isDefinedAt(x)) wrappedReceive(x) else unhandled(x)
  }

}
