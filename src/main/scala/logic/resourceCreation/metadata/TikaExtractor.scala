package logic.resourceCreation.metadata

import java.io.{File, FileInputStream}

import akka.actor.{Actor, ActorRef, Props}

import logic.core.{JobExecution, JobProtocol, JobResult}
import logic.resourceCreation.metadata.MetadataMessages.{ExtractMetadata, MetadataResult}
import org.apache.tika.Tika
import org.apache.tika.metadata.{Metadata, Property, TikaCoreProperties}
import org.apache.tika.parser.AutoDetectParser
import sembaGRPC.{AnnotationValue, ItemDescription}


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
    var description = new ItemDescription()
    parser.parse(stream,
      new org.xml.sax.helpers.DefaultHandler(),
      metadata
    )

    metadata.names().map( x => description = description.addMetadata(
      (x, new AnnotationValue(metadata.getValues(x)))
    ))
    stream.close()
    MetadataResult(description)
  }

}

object TikaExtractor {

  def props(): Props = Props(new TikaExtractor)
}
