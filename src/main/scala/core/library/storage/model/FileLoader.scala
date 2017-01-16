package core.library.storage.model

import akka.actor.Actor
import app.SembaPresets
import core.library.RegisterOntology
import core.{JobHandling, JobProtocol, JobReply}
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.ModelFactory
import utilities.FileFactory

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

    for (source <- FileFactory.filterFileExtension(job.folder, SembaPresets.validOntologyExtensions)) {
      val ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)
      ontology.read(source.toString)
       //if(ontology.hasLoadedImport("http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-main.owl")) {
         val uri = ontology.getNsPrefixURI("resource") + job.libInfo.config.itemName
         job.libInfo.libAccess ! createJob(RegisterOntology(uri, ontology), job)

      // }


    }
  }

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???
}
