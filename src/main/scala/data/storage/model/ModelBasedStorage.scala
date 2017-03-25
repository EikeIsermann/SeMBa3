package data.storage.model

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import data.storage.SembaStorage
import logic.core.{JobHandling, JobProtocol, JobReply, JobResult}
import org.apache.jena.rdf.model.Model

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class ModelBasedStorage extends Actor with JobHandling with SembaStorage {
  override def receive: Receive = ???


  override def getModel(uri: String): Model = ???

  override def getOntModel(uri: String): Model = ???

  override def performRead(): Unit = ???

  override def performWrite(): Unit = ???

  override def endRead(): Unit = ???

  override def endWrite(): Unit = ???

  override def save(): Unit = ???

  override def load(): Unit = ???

  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray[JobResult]): Unit = ???

  override def getUnionModel(): Model = ???
}
