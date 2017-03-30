package logic.resourceCreation.metadata

import java.io.{File, FileInputStream}

import akka.actor.{Actor, Props}
import logic.core._
import logic.resourceCreation.metadata.MetadataMessages.{ExtractMetadata, MetadataResult}
import org.apache.tika.metadata.{Metadata, TikaCoreProperties}
import org.apache.tika.parser.AutoDetectParser
import sembaGRPC.{AnnotationValue, ItemDescription}
import utilities.TextFactory


/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class TikaExtractor extends Actor with JobExecution {
  override def handleJob(job: JobProtocol): JobResult = {
    job match {
      case meta: ExtractMetadata => JobResult(analyzeFile(meta.src))
      case _ => JobResult(ErrorResult())
    }
  }


  def analyzeFile(src: File): ResultContent = {
    val parser = new AutoDetectParser()
    val metadata = new Metadata()
    val stream: FileInputStream = new FileInputStream(src)
    var description = new ItemDescription()
    parser.parse(stream,
      new org.xml.sax.helpers.DefaultHandler(),
      metadata
    )

    metadata.names().foreach( x => description = description.addMetadata(
      (x, new AnnotationValue(metadata.getValues(x)))
    ))
    description = description.update(_.name :=
      Option(metadata.get(TikaCoreProperties.TITLE))
        .getOrElse(TextFactory.omitExtension(src.getAbsolutePath))
    )
    stream.close()
    MetadataResult(description)
  }

}

object TikaExtractor {

  def props(): Props = Props(new TikaExtractor)
}
