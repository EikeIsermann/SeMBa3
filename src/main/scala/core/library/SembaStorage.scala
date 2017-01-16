package core.library

import akka.actor.{Actor, ActorRef}
import core.JobHandling
import org.apache.jena.rdf.model.Model

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */


abstract class SembaStorage extends Actor with JobHandling {

  val writer: ActorRef
  val readers: ActorRef

  def getModel(uri: String): Model

  def getOntModel(uri: String): Model

  def performRead()

  def performWrite()

  def save()

  def load()


}

object SembaStorage {

}







