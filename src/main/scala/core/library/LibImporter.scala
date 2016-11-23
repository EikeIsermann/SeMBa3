package core.library

import java.io.File
import java.net.URI

import akka.actor.{Actor, Props}
import akka.agent.Agent
import akka.routing.RoundRobinPool
import core._
import org.apache.jena.ontology.OntModel

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

//TODO ScalaDoc, JobHandler

case class ImportLib(path: URI, libInfo: LibInfo) extends JobProtocol

case class LoadFolder(folder: File, libInfo: LibInfo) extends JobProtocol


class LibImporter(library: Agent[OntModel]) extends Actor with JobHandling {


  val workers = context.actorOf(new RoundRobinPool(50).props(Props[FileLoader]))
  var availableImports: ArrayBuffer[OntModel] = ArrayBuffer[OntModel]()

  override def receive: Receive = {
    case importLib: ImportLib => {
      acceptJob(importLib, sender())
      importLibrary(importLib)
      self ! JobReply(importLib)
    }
    case reply: JobReply => handleReply(reply, self)
  }

  //TODO add basemodel to see if imported by item
  def importLibrary(importLib: ImportLib): Unit = {
    val lib = new File(importLib.path)
    if (lib.exists) {
      val importJob = lib.listFiles.filter(x => x.isDirectory)
      for (x <- importJob) {
        workers ! createJob(LoadFolder(x, importLib.libInfo), importLib)
      }
    }

    else {
      //TODO error handling
    }


  }

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???
}
