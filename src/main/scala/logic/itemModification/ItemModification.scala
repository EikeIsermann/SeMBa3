package logic.itemModification

import akka.actor.{Actor, ActorRef}
import api._
import logic.core._

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait ItemModification extends Actor with ActorFeatures with JobHandling with AccessToStorage{

  override def initialization(): Unit = {
    super.initialization()
  }

  override def receive: Receive = {
    case removeIt: RemoveFromLibrary => {
    //  sender() ! removeItem(removeIt.resource)
    }
    case updateMeta: UpdateMetadata => {
    //  sender() ! updateMetadata(updateMeta)
    }

    case removeCollItem: RemoveCollectionItem => {
    //  sender() ! removeCollectionItem(removeCollItem.collectionItem)
    }

    case createRel: CreateRelation => {
    //  sender() ! createRelation(createRel.relationModification)
    }
    case removeRel: RemoveRelation => {
    //  sender() ! removeRelation(removeRel.relationModification)
    }
    case x => super.receive(x)
  }
  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray[JobResult]): Unit = ???
}
