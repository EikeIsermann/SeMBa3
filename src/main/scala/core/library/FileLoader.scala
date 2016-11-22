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


//TODO implement JobHandler, Scaladoc

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
      val uri = ontology.listIndividuals(ontology.getOntClass(Paths.itemClassURI)).next().getURI
      job.libInfo.libAccess ! RegisterOntology(uri, ontology)
    }
  }

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???
}
