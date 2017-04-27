package logic.resourceCreation

import java.io.File
import java.net.URI

import akka.actor.{Actor, ActorRef, Props}
import logic._
import globalConstants.SembaPaths
import logic.core._
import logic.core.jobHandling.{Job, JobHandling, JobReply, ResultArray}
import logic.resourceCreation.CreationStorageMethods.CreateInStorage
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shared.Lock
import sembaGRPC.{ItemDescription, ItemType, NewCollection}
import utilities.{FileFactory, WriterFactory}

import scala.collection.mutable

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */


case class CreateCollection(newColl: NewCollection, ontClass: String, libInfo: Config) extends Job

class SingleCollectionImport(val config: Config) extends Actor with ActorFeatures with JobHandling {

  override def handleJob(job: Job, master: ActorRef): Unit = {
     job match {
       case cc: CreateCollection => {
         acceptJob(cc, context.sender())
         createCollection(cc)
       }
     }
  }



  def createCollection(job: CreateCollection): Unit = {
    val itemType = ItemType.COLLECTION
    val name = job.newColl.name
    val ontClass = job.ontClass
    val thumb = job.newColl.picture

    job.libInfo.libAccess ! CreateInStorage(itemType,ontClass,"", ItemDescription().withName(name), job.libInfo, thumb, job.jobID)

  }


  override def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = {
    job match {
      case createCollection: CreateCollection => {
        master ! JobReply(createCollection, results)
      }
    }
  }

}
object  SingleCollectionImport{
  def props(config: Config) = Props(new SingleCollectionImport(config))
}
