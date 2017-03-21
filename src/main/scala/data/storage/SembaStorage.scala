package data.storage

import akka.actor.{Actor, ActorRef}
import core.JobHandling
import org.apache.jena.rdf.model.Model

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */


trait SembaStorage {

  def getModel(uri: String): Model

  def getOntModel(uri: String): Model

  def getUnionModel(): Model

  def performRead()

  def performWrite()

  def endRead()

  def endWrite()

  def save()

  def load()

}








