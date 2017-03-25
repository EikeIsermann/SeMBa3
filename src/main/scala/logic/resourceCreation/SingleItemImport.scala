package logic.resourceCreation

import java.io.File

import akka.actor.{Actor, ActorRef}
import akka.routing.RoundRobinPool
import globalConstants.GlobalMessages.UpdateResult
import logic._
import logic.core._
import logic.resourceCreation.CreationMessages.CreateInStorage
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
    case job: ImportNewItem =>
      {
        acceptJob(job, context.sender())
        val cluster = createJobCluster(ExtractionJob(job), job)
        ThumbActor.getThumbActor(job.item) ! ExtractThumbnail(job.item, job.libInfo.constants)
        tikaExtractor ! ExtractMetadata(job.item)
      }

    case reply: JobReply => handleReply(reply)
  }

  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray[JobResult]): Unit = {

    job match {
      case ex: ExtractionJob => {
        finishExtraction(ex, results)
        master ! JobReply(job, EmptyResult())
      }

      case itemImport: ImportNewItem => {
        master ! JobReply(itemImport, UpdateResult(results.processUpdates()))
      }
    }
  }

  def finishExtraction(ex: ExtractionJob, results: ResultArray[JobResult]) = {
    val queryExecutor = ex.importJob.libInfo.libAccess
    val itemType: ItemType = ItemType.ITEM
    val src = ex.importJob.item.getAbsolutePath
    val ontClass = ex.importJob.ontClass
    val desc = results.get(classOf[MetadataResult]).content
    val thumb = results.get(classOf[ThumbnailResult]).content
    val job = CreateInStorage(itemType, src, ontClass, desc, thumb)
    queryExecutor ! createJob(job, ex.importJob)

  }
}
