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
  override def getABox(): OntModel = ???

  override def getTBox(): OntModel = ???

  override def performRead[T](f: => T): T = ???

  override def performWrite[T](f: => T): T = ???

  override def saveABox(model: OntModel): Unit = ???

  override def saveTBox(model: OntModel): Unit = ???
}
