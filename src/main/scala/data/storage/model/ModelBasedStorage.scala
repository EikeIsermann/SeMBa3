package data.storage.model

import java.net.URI

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import data.storage.{SembaStorage, SembaStorageComponent}
import logic.core.{JobHandling, JobProtocol, JobReply, JobResult}
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.Model

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class ModelBasedStorage extends SembaStorageComponent {
  override def getModel(uri: String): Model = ???

  override def getOntModel(uri: String): OntModel = ???

  override def getUnionModel(): Model = ???

  override def getBaseModel(): Model = ???

  override def performRead(model: Model): Unit = ???

  override def performWrite(model: Model): Unit = ???

  override def endRead(model: Model): Unit = ???

  override def endWrite(model: Model): Unit = ???

  override def save(): Unit = ???

  override def load(path: URI): Unit = ???

  override def containsModel(uri: String): Boolean = ???
}
