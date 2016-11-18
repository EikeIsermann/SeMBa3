package core.library

import java.net.URI

import akka.actor.Actor
import core.{JobHandling, JobProtocol, JobReply, LibraryAccess}
import org.apache.jena.ontology.OntModel
import org.apache.jena.shared.Lock

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
      self ! JobReply(save)
    }
    case reply: JobReply => handleReply(reply)
  }

  def writeOntology(model:OntModel) = {
    LibraryAccess.writeModel(model)
  }

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???
}
