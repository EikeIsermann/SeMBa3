package logic.resourceCreation

import akka.actor.{Actor, ActorRef}
import api.AddToLibrary
import logic.core.{AccessToStorage, ActorFeatures, LibInfo}
import sembaGRPC.VoidResult

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait ResourceCreation extends Actor with ActorFeatures with AccessToStorage {
  var config: LibInfo
  var resourceCreation: ActorRef = _

  override def initialization(): Unit = {
    resourceCreation = initializeFeature(ResourceCreator.props(config))
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

