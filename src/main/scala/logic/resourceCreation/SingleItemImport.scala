package logic.resourceCreation

import java.io.File

import akka.actor.{Actor, ActorRef}
import akka.routing.RoundRobinPool
import data.storage.SembaStorageComponent
import globalConstants.GlobalMessages.{ReadMetadataRequest, ReadMetadataResult, UpdateResult}
import logic._
import logic.core._
import logic.resourceCreation.CreationStorageMethods.CreateInStorage
import logic.resourceCreation.metadata.MetadataMessages._
import logic.resourceCreation.metadata.{ThumbActor, TikaExtractor}
import org.apache.jena.ontology.{Individual, OntModel}
import sembaGRPC.{ItemDescription, ItemType}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

case class ImportNewItem(item: File, ontClass: String, libInfo: LibInfo, copyToLib: Boolean) extends JobProtocol
case class GenerateDatatypeProperties(keys: scala.collection.Set[String], model: OntModel) extends JobProtocol
case class SetDatatypeProperties(propertyMap: mutable.HashMap[String, Array[String]],
                                 item: Individual, model: OntModel) extends JobProtocol

// TODO ScalaDoc, Custom Metadata Mappings
class SingleItemImport extends Actor with JobHandling {
  var tikaExtractor: ActorRef = _

  override def preStart(): Unit = {
    tikaExtractor = context.actorOf(RoundRobinPool(10).props(TikaExtractor.props()))

    super.preStart()
  }
  override def receive: Receive = {
    case importItem: ImportNewItem =>
      {
        acceptJob(importItem, context.sender())
        val cluster = createJobCluster(ExtractionJob(importItem), importItem)
        ThumbActor.getThumbActor(importItem.item) !
          createJob(ExtractThumbnail(importItem.item, importItem.libInfo.constants), cluster)
        tikaExtractor ! createJob(ExtractMetadata(importItem.item), cluster)
      }

    case reply: JobReply => handleReply(reply)
  }


  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray[JobResult]): Unit = {

    job match {
      case ex: ExtractionJob => {
        finishExtraction(ex, results)
        master ! JobReply(ex, EmptyResult())
      }

      case sto: StorageJob => {

        master ! JobReply(sto, UpdateResult(results.processUpdates()))
      }

      case itemImport: ImportNewItem => {
        master ! JobReply(itemImport, UpdateResult(results.processUpdates()))
      }
    }
  }

  def finishExtraction(ex: ExtractionJob, results: ResultArray[JobResult]) = {
    val queryExecutor = ex.importJob.libInfo.libAccess
    val itemType: ItemType = ItemType.ITEM
    val fileName = ex.importJob.item.getName
    val ontClass = ex.importJob.ontClass
    val desc = results.get(classOf[MetadataResult]).content
    val job = CreateInStorage(itemType, ontClass, fileName, desc, ex.importJob.libInfo)
    val cluster = createJobCluster(StorageJob(ex.importJob), ex.importJob)
    queryExecutor ! createJob(job, cluster)
  }
}
