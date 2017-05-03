package data.storage

import java.net.URI

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, Props}
import akka.routing.RoundRobinPool
import data.storage.dataset.DatasetStorage
import globalConstants.GlobalMessages.{StorageReadRequest, StorageWriteRequest}
import logic.core._
import logic.core.jobHandling.{Job, JobHandling, JobReply, ResultArray}
import logic.resourceCreation.CreationStorageMethods.CreateInStorage
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.Model
import utilities.SembaConstants.StorageSolution

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

case class InitializedStorage()
class SembaStorage(val config: Config) extends ActorFeatures with JobHandling {
  val readExecutors = context.actorOf(new RoundRobinPool(20).props(ReadExecutor.props(config)))
  val writeExecutor = context.actorOf(new RoundRobinPool(1).props(WriteExecutor.props(config)))
  writeExecutor ! InitializedStorage()
  override def preStart(): Unit = {
    super.preStart()
  }

  override def wrappedReceive: Receive = {
    case init: InitializedStorage => {
      context.parent ! InitializationComplete(self)
    }
  }

  override def handleJob(job: Job, master: ActorRef): Unit = {
    job match {
      case read: StorageReadRequest => handleReadRequest(read, sender())
      case write: StorageWriteRequest => handleWriteRequest(write, sender())

    }

  }
  override def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = {
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
  def props(config: Config): Props= Props(new SembaStorage(config))
}






object SembaStorageComponent {
  def getStorage(config: Config): SembaStorageComponent = {
    config.constants.storageType match {
      //case StorageSolution.InMemory => new ModelBase
      case StorageSolution.TDB => new DatasetStorage(config)
    }
  }

}




abstract class SembaStorageComponent {

  /** @return an OntModel containing all asserted and deducted statements about the library content. */
  def getABox: OntModel

  /** @return an OntModel containing all asserted and deducted statements about the library content. */
  def getTBox: OntModel
  /** Should only be used for read access, increased query performance.
    * @return an OntModel containing only the asserted statements
    */
  def getABoxNoReasoning: OntModel
  def saveABox(model: OntModel)
  def saveTBox(model: OntModel)

  /**
    * Sets a Read Lock. Executes the query if no write lock is set.
    * @param f A query function without any side effects to the model.
    * @tparam T The return type of the Query function.
    * @return An instance of T
    */
  def performRead[T](f: => T): T

  /**
    * Sets a Write Lock. Executes the query if no lock is set.
    * @param f A query function that changes the state of the model.
    * @tparam T The return type of the Query function.
    * @return An instance of T
    */
  def performWrite[T](f: => T): T

  /** Called on Startup. Initializes the persisted DataModel */
  def initialize()
}








