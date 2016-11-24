package core.library

import java.io.File

import akka.actor.Actor
import app.{Paths, Presets}
import core.{JobHandling, JobProtocol, JobReply, LibInfo}
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.ModelFactory
import utilities.FileFactory

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */


//TODO Scaladoc

class FileLoader extends Actor with JobHandling{

  override def receive: Receive = {
    case load: LoadFolder => {
      acceptJob(load, sender())
      readFolder(load)
      self ! JobReply(load)
    }
    case reply: JobReply => handleReply(reply, self)
  }


  def readFolder(job: LoadFolder) = {

    for (source <- FileFactory.filterFileExtension(job.folder, Presets.validOntologyExtensions)) {
      val ontology = ModelFactory.createOntologyModel()
      ontology.read(source.toString)
       if(ontology.hasLoadedImport("http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-main.owl")) {
         val uri = ontology.listIndividuals(ontology.getOntClass(Paths.resourceDefinitionURI)).next().getURI
         job.libInfo.libAccess ! createJob(RegisterOntology(uri, ontology), job)

       }


    }
  }

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???
}
