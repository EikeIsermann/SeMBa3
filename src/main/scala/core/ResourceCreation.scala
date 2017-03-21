package core

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import api.AddToLibrary
import core.library.ResourceCreator

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait ResourceCreation extends Actor with ActorFeatures with AccessToStorage {
  var config: LibInfo
  var resourceCreation: ActorRef = _

  override def initialization(): Unit = {
       resourceCreation = initializeFeature(ResourceCreator.props(config))
  }

  override def receive: Receive = {
    case addItem: AddToLibrary => {
    }
  }
}
