package core.metadata

import javax.imageio.ImageIO

import akka.actor.{Actor, Props}
import core.{JobHandling, JobProtocol, JobReply, ThumbnailJob}

import scala.collection.mutable.HashMap

/** Abstract class all ThumbnailActors inherit from.
  *
  *
  */

abstract class ThumbActor extends Actor with JobHandling {


  override def receive: Receive = {
    case thumb: ThumbnailJob => {
      acceptJob(thumb, sender())
      createThumbnail(thumb)
      self ! JobReply(thumb)
    }
    case reply: JobReply => handleReply(reply, self)
  }

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???


  /** ThumbnailActors only need to implement createThumbnail providing logic to render and write the image.
    *
    * @param thumb
    */
  def createThumbnail(thumb: ThumbnailJob)

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

  /** All available ThumbnailActor definitions */
  val ALL_GENERATORS = Set[(Props, Set[String])](MIMETYPES_IMAGE, MIMETYPES_PDF, MIMETYPES_TXT)


  private val supportedContentTypes: HashMap[String, Props] = {
    val map = new HashMap[String, Props]()
    for (gen <- ALL_GENERATORS) {
      gen._2.foreach(key => map.put(key, gen._1))
    }
    map
  }

  /** Returns the correct actor constructor registered for a given MimeType
    *
    * @param mime MimeType
    * @return Props for the required ThumbActor
    */
  def getProps(mime: String): Props = {
    supportedContentTypes.get(mime).getOrElse(Props[GenericThumbActor])
  }
}
