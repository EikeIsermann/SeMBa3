package core.metadata

import javax.imageio.ImageIO

import akka.actor.{Actor, Props}
import core.{JobHandling, JobProtocol, JobReply, ThumbnailJob}

import scala.collection.mutable.HashMap
import scala.collection.mutable

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
abstract class ThumbActor extends Actor with JobHandling {

  override def receive: Receive = {
    case job: JobProtocol => sender ! handleJob(job)
  }


  override def handleJob(job: JobProtocol): JobReply = {
    job match {
      case thumb: ThumbnailJob => createThumbnail(thumb)
    }
    new JobReply(job)
  }

  def createThumbnail(thumb: ThumbnailJob)

}

object ThumbActor  {


  val MIMETYPES_IMAGE = (Props[PicThumb],
      ImageIO.getReaderMIMETypes.toSet)


  val MIMETYPES_PDF = (Props[PdfThumb],
    Set("application/pdf"))

  val MIMETYPES_TXT = (Props[TxtThumb],
      Set("text/plain",
      "text/cmd",
      "text/css",
      "text/csv",
      "text/html",
      "text/javascript",
      "text/vcard",
      "text/xml",
      "application/xml",
      "application/rdf+xml",
      "application/x-tex"))

  val ALL_GENERATORS = Set[(Props,Set[String])](MIMETYPES_IMAGE, MIMETYPES_PDF, MIMETYPES_TXT)

    private val supportedContentTypes: HashMap[String, Props] = {
      val map = new HashMap[String, Props]()
      for( gen <- ALL_GENERATORS){
        gen._2.foreach(key => map.put(key, gen._1))
      }
      map
    }


  def getProps(mime: String): Props = {
    supportedContentTypes.get(mime).getOrElse(Props[GenericThumbActor])
  }
}
