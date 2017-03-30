package logic.resourceCreation

import akka.actor.{Actor, ActorRef}
import api.AddToLibrary
import logic.core.{AccessToStorage, ActorFeatures, LibInfo, SembaBaseActor}
import sembaGRPC.VoidResult

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait ResourceCreation extends SembaBaseActor with AccessToStorage {
  var resourceCreation: ActorRef = _

abstract override def initialization(): Unit = {
    resourceCreation = initializeFeature(ResourceCreator.props(libInfo), "ResourceCreator")
    super.initialization()
  }

 override def receive: Receive = {
    case addItem: AddToLibrary => {
      val notEmpty = addItem.sourceFile.source.isDefined
      sender() ! VoidResult(notEmpty, if (notEmpty) "Trying to import Item." else "No source file set.")
      resourceCreation ! createMasterJob(addItem)
    }
    case x => super.receive(x)
    }
  }

