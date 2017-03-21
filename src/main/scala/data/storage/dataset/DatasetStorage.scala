package data.storage.dataset

import akka.actor.Actor
import akka.actor.Actor.Receive
import core.{JobHandling, JobProtocol, JobReply}
import data.storage.SembaStorage
import org.apache.jena.rdf.model.Model

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class DatasetStorage extends Actor with JobHandling with SembaStorage {

  override def receive: Receive = ???

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???

  override def getModel(uri: String): Model = ???

  override def getOntModel(uri: String): Model = ???

  override def performRead(): Unit = ???

  override def performWrite(): Unit = ???

  override def endRead(): Unit = ???

  override def endWrite(): Unit = ???

  override def save(): Unit = ???

  override def load(): Unit = ???
}
