package core.metadata

import java.io.{File, FileInputStream}

import akka.actor.{Actor, ActorRef, Props}
import akka.actor.Actor.Receive
import app.SembaPaths
import core.library.{GenerateDatatypeProperties, SetDatatypeProperties}
import core._
import core.metadata.MetadataMessages.ExtractMetadata
import org.apache.tika.Tika
import org.apache.tika.metadata.{Metadata, TikaCoreProperties}
import org.apache.tika.parser.AutoDetectParser
import utilities.TextFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class TikaExtractor extends Actor with JobExecution {


  override def handleJob(job: JobProtocol): JobResult = {
    job match {
      case extract: ExtractMetadata => analyzeFile(extract.src)
    }
  }

  def analyzeFile(src: File): JobResult = {
    val tika = new Tika()
    val parser = new AutoDetectParser()
    val metadata = new Metadata()
    val stream: FileInputStream = new FileInputStream(src)

    parser.parse(stream,
      new org.xml.sax.helpers.DefaultHandler(),
      metadata
    )

    val readMetadataProperties = mutable.HashMap[String, Array[String]]()
    for (metadataProperty <- metadata.names) {
      val metaURI = job.libInfo.config.baseOntologyURI + "#" + TextFactory.cleanString(metadataProperty)
      readMetadataProperties.put(metaURI, metadata.getValues(metadataProperty))
    }
    job.libInfo.libAccess !
      createJob(GenerateDatatypeProperties(readMetadataProperties.keySet, job.libInfo.basemodel()),job)
    title = Option(metadata.get(TikaCoreProperties.TITLE)).getOrElse(TextFactory.omitExtension(job.item.getAbsolutePath))

    readMetadataProperties.put(SembaPaths.sembaTitle, Array(title))

    job.libInfo.libAccess !
      createJob(SetDatatypeProperties(readMetadataProperties, itemIndividual, itemOntology), job)

    stream.close()
  }

}

object TikaExtractor{

  def props(): Props = Props(new TikaExtractor)
}
