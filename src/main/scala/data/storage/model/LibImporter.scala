package data.storage.model

import java.io.File
import java.net.URI

import akka.actor.{Actor, ActorRef, Props}
import akka.agent.Agent
import akka.routing.RoundRobinPool
import logic._
import logic.core._
import logic.core.jobHandling.{Job, JobHandling, JobReply, ResultArray}
import org.apache.jena.ontology.OntModel

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

//TODO ScalaDoc, JobHandler

case class ImportLib(path: URI, libInfo: Config) extends Job

case class LoadFolder(folder: File, libInfo: Config) extends Job


class LibImporter(library: Agent[OntModel], val config: Config) extends Actor with ActorFeatures with JobHandling {


  val workers = context.actorOf(new RoundRobinPool(10).props(Props[FileLoader]))
  var availableImports: ArrayBuffer[OntModel] = ArrayBuffer[OntModel]()

  override def handleJob(job: Job, master: ActorRef): Unit = {
    job match {
      case importLib: ImportLib => {
        acceptJob(importLib, sender())
        importLibrary(importLib)
        //      self ! JobReply(importLib)
      }
    }

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

  override def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = ???
}

object LibImporter  {
  def props(library: Agent[OntModel], config: Config) = Props(new LibImporter(library, config))
}