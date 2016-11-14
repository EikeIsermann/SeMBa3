package core.library

import java.io.File
import java.net.URI
import akka.routing.{ActorRefRoutee, RoundRobinPool, RoundRobinRoutingLogic, Router}
import akka.actor.{Actor, ActorRef, Props}
import akka.agent.Agent
import core.LibraryAccess
import org.apache.jena.ontology.OntModel

import scala.collection.mutable.ArrayBuffer
/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

sealed trait ImportProtocol
case class ImportLib(path: URI) extends ImportProtocol
case class LoadFolder(folder: File) extends ImportProtocol
case class SuccessfulSingleImport(models: ArrayBuffer[OntModel]) extends ImportProtocol

class LibImporter(library: Agent[OntModel]) extends Actor {


  val workers = context.actorOf(new RoundRobinPool(10).props(Props[FileLoader]))
  var jobs: Int = _
  var completedJobs: Int = 0
  var availableImports: ArrayBuffer[OntModel] = ArrayBuffer[OntModel]()
  override def receive: Receive = {
    case ImportLib(path) => importLibrary(path)
    case SuccessfulSingleImport(result) => {
      completedJobs += 1
      result.foreach(models => availableImports.+=(models))
      if(completedJobs == jobs){
          library.send( base => LibraryAccess.addToLib(base, availableImports))
      }
    }
  }

  //TODO add basemodel to see if imported by item
  def importLibrary(path: URI): Unit = {
    val lib = new File(path)
    if ( lib.exists )
      {
        val importJob = lib.listFiles.filter( x => x.isDirectory)
        jobs = importJob.size
        for (x <- importJob)
        {
          workers ! LoadFolder(x)
        }
      }

    else
      {
        //TODO error handling
      }


  }


}
