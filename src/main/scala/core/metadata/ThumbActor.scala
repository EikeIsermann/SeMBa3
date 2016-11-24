package core.metadata

import javax.imageio.ImageIO

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import core.library.FileLoader
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
   val MIMETYPES_GENERIC = (Props[GenericThumbActor], Set("GenericThumbActor"))
  /** All available ThumbnailActor definitions */
  val ALL_GENERATORS = Set[(Props, Set[String])](MIMETYPES_GENERIC, MIMETYPES_IMAGE,  MIMETYPES_TXT) //MIMETYPES_PDF,)


  private val supportedContentTypes: scala.collection.mutable.HashMap[String, ActorRef] = HashMap[String, ActorRef]()
  /** Returns the correct actor constructor registered for a given MimeType
    *
    * @param mime MimeType
    * @return Props for the required ThumbActor
    */
  def getThumbActor(mime: String): ActorRef = {
    supportedContentTypes.get(mime).getOrElse(supportedContentTypes.apply("GenericThumbActor"))
  }

  def initialize(system: ActorSystem) = {
    for (gen <- ALL_GENERATORS) {
      gen._2.foreach(key => supportedContentTypes.put(key,
        system.actorOf(new RoundRobinPool(10).props(gen._1))))
    }
  }
}
