package logic.resourceCreation.metadata

import java.io.{File, FileInputStream}

import akka.actor.{Actor, ActorRef, Props}
import logic.core._
import logic.core.jobHandling._
import logic.resourceCreation.metadata.MetadataMessages.{ExtractMetadata, MetadataResult}
import org.apache.tika.metadata.{Metadata, TikaCoreProperties}
import org.apache.tika.parser.AutoDetectParser
import sembaGRPC.{AnnotationValue, ItemDescription}
import utilities.TextFactory


/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class TikaExtractor extends Actor with SingleJobExecutor {

  override def performTask(job: Job): JobResult =  {
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
    //Parse file and add extracted information to metadata object
    parser.parse(stream,
      new org.xml.sax.helpers.DefaultHandler(),
      metadata
    )
    //Map metadata to Annotationvalue and add K-V pair to return value.
    metadata.names().foreach(x => description = description.addMetadata(
      (x, new AnnotationValue(metadata.getValues(x)))
    ))
    //If no title was extracted use filename without extension as name
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
