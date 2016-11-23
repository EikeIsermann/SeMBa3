package core.library

import java.io.File
import java.net.URI

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import core.{JobHandling, JobProtocol, JobReply, LibraryAccess}
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.jena.ontology.OntModel
import sembaGRPC.Resource

case class RemoveFromOntology(item: Resource, libraryAccess: ActorRef, deleteFiles: Boolean = true) extends JobProtocol
/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

class FileRemover extends Actor with JobHandling {


  override def receive: Receive = {
    case removeItem: RemoveFromOntology => {
      acceptJob(removeItem, sender())
      removeFromOntology(removeItem)
      if(removeItem.deleteFiles) removeFromFileSystem(removeItem.item.uri)
      self ! JobReply(removeItem)
    }
    case reply: JobReply => handleReply(reply, self)
  }

  def removeFromOntology(removeItem: RemoveFromOntology): Unit = {

    removeItem.libraryAccess ! createJob(DeleteItem(removeItem.item), removeItem)
  }

  def removeFromFileSystem(item: String) = {
    val folder = new File(new URI(item.substring(0, item.lastIndexOf("/"))))
    if (folder.isDirectory) {
      FileUtils.deleteDirectory(folder)
    }
  }

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???
}
