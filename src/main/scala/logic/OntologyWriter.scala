package logic

import akka.actor.{Actor, ActorRef}
import data.storage.AccessMethods
import logic.core.{ResultArray, _}
import org.apache.jena.ontology.OntModel

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
case class SaveOntology(model: OntModel) extends JobProtocol
class OntologyWriter extends Actor with ActorFeatures with JobHandling{
  def wrappedReceive: Receive = {
    case save: SaveOntology => {
      acceptJob(save, sender)
      writeOntology(save.model)
      self ! JobReply(save, new ResultArray())
    }
    case reply: JobReply => handleReply(reply)
  }

  def writeOntology(model:OntModel) = {
      AccessMethods.writeModel(model)
  }

  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray): Unit = ???
}
