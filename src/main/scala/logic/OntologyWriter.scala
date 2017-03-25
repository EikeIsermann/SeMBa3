package logic

import akka.actor.Actor
import data.storage.AccessMethods
import logic.core.{EmptyResult, JobHandling, JobProtocol, JobReply}
import org.apache.jena.ontology.OntModel

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
case class SaveOntology(model: OntModel) extends JobProtocol
class OntologyWriter extends Actor with JobHandling{
  override def receive: Receive = {
    case save: SaveOntology => {
      acceptJob(save, sender)
      writeOntology(save.model)
      self ! JobReply(save, EmptyResult())
    }
    case reply: JobReply => handleReply(reply)
  }

  def writeOntology(model:OntModel) = {
      AccessMethods.writeModel(model)
  }

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???
}
