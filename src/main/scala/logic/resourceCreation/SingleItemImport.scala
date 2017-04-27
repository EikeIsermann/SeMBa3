package logic.resourceCreation

import java.io.File
import java.net.URI

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.RoundRobinPool
import data.storage.SembaStorageComponent
import logic._
import logic.core._
import logic.core.jobHandling.{Job, JobHandling, JobReply, ResultArray}
import logic.resourceCreation.CreationStorageMethods.CreateInStorage
import logic.resourceCreation.metadata.MetadataMessages._
import logic.resourceCreation.metadata.{ThumbActor, TikaExtractor}
import org.apache.jena.ontology.{Individual, OntModel}
import sembaGRPC.{ItemDescription, ItemType}
import utilities.debug.DC

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

case class ImportNewItem(item: File, ontClass: String, libInfo: Config, copyToLib: Boolean) extends Job
case class GenerateDatatypeProperties(keys: scala.collection.Set[String], model: OntModel) extends Job
case class SetDatatypeProperties(propertyMap: mutable.HashMap[String, Array[String]],
                                 item: Individual, model: OntModel) extends Job

// TODO ScalaDoc, Custom Metadata Mappings
class SingleItemImport(val config: Config) extends LibActor {
  var tikaExtractor: ActorRef = _

  override def preStart(): Unit = {
    tikaExtractor = context.actorOf(RoundRobinPool(10).props(TikaExtractor.props(config)))

    super.preStart()
  }

  override def handleJob(job: Job, master: ActorRef): Unit = {
    job match {
      case importItem: ImportNewItem =>
      {
        acceptJob(importItem, context.sender())
        val cluster = createJobCluster(ExtractionJob(importItem), importItem)
        val constants = importItem.libInfo.constants
        ThumbActor.getThumbActor(importItem.item) !
          createJob(ExtractThumbnail(importItem.item, URI.create(constants.dataPath + importItem.jobID + "/" + constants.thumbnail), constants), cluster)
        tikaExtractor ! createJob(ExtractMetadata(importItem.item), cluster)
      }
    }
  }

  override def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = {

    job match {
      case ex: ExtractionJob => {
        finishExtraction(ex, results)
        master ! JobReply(ex, results)
      }

      case sto: StorageJob => {
        master ! JobReply(sto, results)
      }

      case itemImport: ImportNewItem => {
        master ! JobReply(itemImport, results)
      }
    }
  }

  def finishExtraction(ex: ExtractionJob, results: ResultArray) = {
    val queryExecutor = ex.importJob.libInfo.libAccess
    val itemType: ItemType = ItemType.ITEM
    val fileName = ex.importJob.item.getName
    val ontClass = ex.importJob.ontClass
    val desc = results.get(classOf[MetadataResult]).payload
    val thumb = results.get(classOf[ThumbnailResult]).payload.toString
    val job = CreateInStorage(itemType, ontClass, fileName, desc, ex.importJob.libInfo, thumb, ex.importJob.jobID)
    val cluster = createJobCluster(StorageJob(ex.importJob), ex.importJob)
    ex.importJob.libInfo.libAccess  ! createJob(job, cluster)
  }
}

object SingleItemImport {
  def props(config: Config) = Props(new SingleItemImport(config))
}
