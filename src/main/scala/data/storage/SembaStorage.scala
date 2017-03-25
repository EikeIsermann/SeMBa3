package data.storage

import java.net.URI

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, Props}
import akka.routing.RoundRobinPool
import data.storage.dataset.DatasetStorage
import logic.core.{JobHandling, JobProtocol, JobResult, LibInfo}
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.Model
import utilities.SembaConstants.StorageSolution

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

class SembaStorage(config: LibInfo) extends Actor with JobHandling {

  val storage: SembaStorageComponent = SembaStorageComponent.getStorage(config)
  val readExecutor = context.actorOf(new RoundRobinPool(10).props(ReadExecutor.props(storage)))
  val writeExecutor = context.actorOf(WriteExecutor.props(storage))

  override def receive: Receive = ???

  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray[JobResult]): Unit = ???
}


object SembaStorage {
  def props(config: LibInfo): Props= Props(new SembaStorage(config))
}






object SembaStorageComponent {
  def getStorage(config: LibInfo): SembaStorageComponent = {
    config.constants.storageType match {
      case StorageSolution.InMemory =>
      case StorageSolution.TDB => new DatasetStorage(config)
    }
  }

}




abstract class SembaStorageComponent {
  
  
  def getModel(uri: String): Model

  def getOntModel(uri: String): OntModel

  def getUnionModel(): Model

  def getBaseModel(): Model

  def performRead(model: Model)

  def performWrite(model: Model)

  def endRead(model: Model)

  def endWrite(model: Model)

  def save()

  def load(path: URI)

  def containsModel(uri:String): Boolean

}








