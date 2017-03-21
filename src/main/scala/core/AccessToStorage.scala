package core

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.RoundRobinPool
import core.library.StorageQueryExecutor
import data.storage.StorageAccess
import utilities.SembaConstants.StorageSolution.StorageSolution

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
case class StorageInitialization(storageType: StorageSolution, storagePath: String)

trait AccessToStorage extends Actor with ActorFeatures{

  var storageAccess: ActorRef = _
  var queryExecutor: ActorRef = _

  override def initialization(): Unit = {
    storageAccess = initializeFeature(StorageAccess.props())
    queryExecutor = initializeFeature(StorageQueryExecutor.props(storageAccess))
    super.initialization()
  }
  override def receive: Receive = {
    case _ => {}

  }
}


