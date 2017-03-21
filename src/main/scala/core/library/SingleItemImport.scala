package core.library

import java.io.{File, FileInputStream, FileNotFoundException}
import java.net.URI
import java.util.UUID

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.routing.RoundRobinPool
import app.{Application, SembaPaths}
import core._
import core.metadata.MetadataMessages.{ExtractMetadata, ExtractThumbnail, ExtractionJob}
import core.metadata.{ThumbActor, TikaExtractior, TikaExtractor}
import data.storage.RegisterOntology
import org.apache.jena.ontology.{Individual, OntModel, OntModelSpec}
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shared.Lock
import org.apache.tika.Tika
import org.apache.tika.metadata.{Metadata, TikaCoreProperties}
import org.apache.tika.parser.AutoDetectParser
import sembaGRPC._
import utilities.debug.DC
import utilities.{Convert, FileFactory, TextFactory, WriterFactory}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

case class ImportNewItem(item: File, libInfo: LibInfo, copyToLib: Boolean) extends JobProtocol
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
        ThumbActor.getThumbActor(job.item) ! ExtractThumbnail(job.item, job.libInfo.config)
        tikaExtractor ! ExtractMetadata(job.item)
      }





    case reply: JobReply => handleReply(reply)
  }

  override def finishedJob(job: JobProtocol, master: ActorRef, results: ArrayBuffer[JobResult]): Unit = {
    job match {
      case ex: ExtractionJob => {
        finishExtraction(ex, results)
        master ! JobReply(job, JobResult())
      }

      case itemImport: ImportNewItem => {
        //TODO define important messages
      }
    }
  }

  def finishExtraction(ex: ExtractionJob, results: ArrayBuffer[JobResult]) = {

  }
}
