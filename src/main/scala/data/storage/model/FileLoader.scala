package data.storage.model

import akka.actor.{Actor, ActorRef, Props}
import globalConstants.SembaPresets
import logic.core.{ActorFeatures, Config}
import logic.core.jobHandling.{Job, JobHandling, JobReply, ResultArray}
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.ModelFactory
import utilities.FileFactory

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */


//TODO Scaladoc

class FileLoader(val config: Config) extends Actor with ActorFeatures with JobHandling{

  override def handleJob(job: Job, master: ActorRef): Unit = {
    job match {
      case load: LoadFolder => {
        acceptJob(load, sender())
        readFolder(load)
        // self ! JobReply(load)
      }
    }
  }


  def readFolder(job: LoadFolder) = {

    for (source <- FileFactory.filterFileExtension(job.folder, SembaPresets.validOntologyExtensions)) {
      val ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)
      ontology.read(source.toString)
       //if(ontology.hasLoadedImport("http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-main.owl")) {
         val uri = ontology.getNsPrefixURI("resource") + job.libInfo.constants.itemName
         //job.libInfo.libAccess ! createJob(RegisterOntology(uri, ontology), job)

      // }


    }
  }

  override def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = ???
}
object FileRemover {
  def props(config: Config) = Props(new FileLoader(config))
}

