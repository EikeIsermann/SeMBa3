package logic.resourceCreation

import java.io.File
import java.net.URI

import akka.actor.{Actor, ActorRef}
import logic._
import data.storage.RegisterOntology
import globalConstants.GlobalMessages.UpdateResult
import globalConstants.SembaPaths
import logic.core._
import logic.resourceCreation.CreationMessages.CreateInStorage
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


case class CreateCollection(newColl: NewCollection, ontClass: String, libInfo: LibInfo) extends JobProtocol

class SingleCollectionImport extends Actor with JobHandling {
  override def receive: Receive = {

    case job: CreateCollection => {
      acceptJob(job, context.sender())
      createCollection(job)
    }

    case jobReply: JobReply => {
      handleReply(jobReply)
    }

  }

  def createCollection(job: CreateCollection): Unit = {
    val itemType = ItemType.COLLECTION
    val src = job.newColl.name
    val ontClass = job.ontClass
    val thumb = URI.create(job.newColl.picture)

    job.libInfo.libAccess ! CreateInStorage(itemType, src, ontClass, ItemDescription(), thumb)

  }


  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray[JobResult]): Unit = {
    job match {
      case createCollection: CreateCollection => {
        master ! JobReply(createCollection, UpdateResult(results.processUpdates()))
      }
    }
  }

}
