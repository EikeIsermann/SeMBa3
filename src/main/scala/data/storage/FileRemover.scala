package data.storage

import java.io.File
import java.net.URI

import akka.actor.{Actor, ActorRef}
import logic.core._
import org.apache.commons.io.FileUtils
import sembaGRPC.Resource

case class RemoveFromOntology(item: Resource, libraryAccess: ActorRef, deleteFiles: Boolean = true) extends JobProtocol
/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

class FileRemover extends Actor with ActorFeatures with JobHandling {

  override def wrappedReceive: Receive = {
    case removeItem: RemoveFromOntology => {
      acceptJob(removeItem, sender())
      removeFromOntology(removeItem)
      if (removeItem.deleteFiles) removeFromFileSystem(removeItem.item.uri)
      //  self ! JobReply(removeItem)
    }
  }


  def removeFromOntology(removeItem: RemoveFromOntology): Unit = {

//    removeItem.libraryAccess ! createJob(DeleteItem(removeItem.item), removeItem)
  }

  def removeFromFileSystem(item: String) = {
    val folder = new File(new URI(item.substring(0, item.lastIndexOf("/"))))
    if (folder.isDirectory) {
      FileUtils.deleteDirectory(folder)
    }
  }

  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray): Unit = {

    }



}
