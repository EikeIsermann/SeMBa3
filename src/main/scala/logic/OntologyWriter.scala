package logic

import akka.actor.{Actor, ActorRef, Props}
import data.storage.AccessMethods
import logic.core.{ActorFeatures, Config}
import logic.core.jobHandling.{Job, JobHandling, JobReply, ResultArray}
import org.apache.jena.ontology.OntModel

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
case class SaveOntology(model: OntModel) extends Job
class OntologyWriter(val config: Config)extends Actor with ActorFeatures with JobHandling{

  override def handleJob(job: Job, master: ActorRef): Unit = {
    job match {
      case save: SaveOntology => {
        acceptJob(save, sender)
        writeOntology(save.model)
        self ! JobReply(save, new ResultArray())
      }
    }
  }


  def writeOntology(model:OntModel) = {
      AccessMethods.writeModel(model)
  }

  override def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = ???
}

object OntologyWriter{
  def props(config: Config) = Props(new OntologyWriter (config))
}
