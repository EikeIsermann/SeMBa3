package logic.core

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef}
import api.{GetMetadata, OpenLib, RequestContents}
import data.storage.StorageAccess
import utilities.SembaConstants.StorageSolution.StorageSolution

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
case class StorageInitialization(storageType: StorageSolution, storagePath: String)

trait AccessToStorage extends Actor with ActorFeatures with JobHandling {//with Actor with ActorFeatures with JobHandling {

  var storageAccess: ActorRef = _
  var queryExecutor: ActorRef = _

  override def initialization(): Unit = {
    storageAccess = initializeFeature(StorageAccess.props())
    queryExecutor = initializeFeature(StorageQueryExecutor.props(storageAccess))
    super.initialization()
  }
  override def receive: Receive = {
    case openLib: OpenLib => {
       //return Concepts
    }


    case contents: RequestContents => {

    }


    case getMeta: GetMetadata => {

    }


    case x => super.receive(x)
  }

}


