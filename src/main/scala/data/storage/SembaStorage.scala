package data.storage

import java.net.URI

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, Props}
import akka.routing.RoundRobinPool
import data.storage.dataset.DatasetStorage
import globalConstants.GlobalMessages.{StorageReadRequest, StorageWriteRequest}
import logic.core._
import logic.resourceCreation.CreationStorageMethods.CreateInStorage
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.Model
import utilities.SembaConstants.StorageSolution

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

class SembaStorage(config: LibInfo) extends Actor with ActorFeatures with JobHandling {

  val storage: SembaStorageComponent = SembaStorageComponent.getStorage(config)
  val readExecutors = context.actorOf(new RoundRobinPool(10).props(ReadExecutor.props(storage)))
  val writeExecutor = context.actorOf(WriteExecutor.props(storage))

  override def preStart(): Unit = {
    context.parent ! InitializationComplete(self)
    super.preStart()
  }

  def wrappedReceive: Receive = {
    case read: StorageReadRequest => handleReadRequest(read, sender())

    case write: StorageWriteRequest => handleWriteRequest(write, sender())
  }

  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray): Unit = {
    master ! JobReply(job, results)
  }

  def handleReadRequest(read: StorageReadRequest, master: ActorRef) = {
    readExecutors ! forwardJob(read, master)
  }

  def handleWriteRequest(write: StorageWriteRequest, master: ActorRef) = {
    writeExecutor ! forwardJob(write, master)
  }

}


object SembaStorage {
  def props(config: LibInfo): Props= Props(new SembaStorage(config))
}






object SembaStorageComponent {
  def getStorage(config: LibInfo): SembaStorageComponent = {
    config.constants.storageType match {
      //case StorageSolution.InMemory => new ModelBase
      case StorageSolution.TDB => new DatasetStorage(config)
    }
  }

}




abstract class SembaStorageComponent {
  
  def getABox(): OntModel

  def getTBox(): OntModel

  def saveABox(model: OntModel)

  def saveTBox(model: OntModel)

  def performRead[T](f: => T): T

  def performWrite[T](f: => T): T
  /*
  def save()

  def load(path: URI)
  */

}








