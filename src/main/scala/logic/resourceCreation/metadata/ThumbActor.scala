package logic.resourceCreation.metadata

import java.io.File
import java.net.URI
import java.nio.file.{Files, Path}
import javax.imageio.ImageIO

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import data.storage.model.FileLoader
import logic._
import logic.core._
import logic.core.jobHandling._
import logic.resourceCreation.metadata.MetadataMessages.ExtractThumbnail
import org.apache.tika.Tika
import utilities.FileFactory

import scala.collection.mutable.HashMap

/** Abstract class all ThumbnailActors inherit from.
  *
  *
  */

abstract class ThumbActor extends GlobalJobExecutor {
  override def receive: Receive = {
    case thumb: ExtractThumbnail => {
      benchmarkActor = thumb.config.benchmarkActor
      super.receive(thumb)
    }
  }

  override def performTask(job: Job): JobResult = {
    job match {
      case thumb: ExtractThumbnail ⇒ JobResult(createThumbnail(thumb))
      case _ ⇒ JobResult(ErrorResult())
    }
  }



  /** ThumbnailActors only need to implement createThumbnail providing logic to render and write the image.
    *
    * @param thumb
    */

  def createThumbnail(thumb: ExtractThumbnail): ResultContent

  def createPath(thumb: ExtractThumbnail): File  = {
    val dest: Path = new File(thumb.path).toPath
    val folder = dest.getParent
    if(!Files.exists(folder)) Files.createDirectories(dest.getParent)
    dest.toFile
  }
}



/** Returns the correct ThumbnailActorRef for a given Mimetype. Stores all supported MimeTypes for available Actors.
  * ThumbnailActors have to be registered here following the given pattern
  * MIMETYPES_DOCUMENTTYPE = Props[ThumbnailActor] / Set(MimeType)
  *
  */
object ThumbActor {
  /** Supported picture formats */
  val MIMETYPES_IMAGE = (Props[PicThumb],
    ImageIO.getReaderMIMETypes.toSet)

  /** Supported PDF formats*/
  val MIMETYPES_PDF = (Props[PdfThumb],
    Set("application/pdf"))

  /** Supported raw text formats */
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
   val MIMETYPES_GENERIC = (Props[GenericThumbActor], Set("GenericThumbActor"))
  /** All available ThumbnailActor definitions */
  val ALL_GENERATORS = Set[(Props, Set[String])](MIMETYPES_GENERIC, MIMETYPES_IMAGE,  MIMETYPES_TXT, MIMETYPES_PDF)

  private val supportedContentTypes: scala.collection.mutable.HashMap[String, ActorRef] = HashMap[String, ActorRef]()
  /** Returns the correct actor constructor registered for a given MimeType
    *
    * @param file The file to be thumbnailed
    * @return Props for the required ThumbActor
    */
  def getThumbActor(file: File): ActorRef = {
    val mimeType = FileFactory.getMimeTypeOf(file)
    supportedContentTypes.getOrElse(mimeType , supportedContentTypes.apply("GenericThumbActor"))
//    supportedContentTypes.apply("GenericThumbActor")
  }

  def initialize(system: ActorSystem) = {
    for (gen <- ALL_GENERATORS) {
      gen._2.foreach(key => supportedContentTypes.put(key,
        system.actorOf(new RoundRobinPool(10).props(gen._1))))
    }
  }
}
