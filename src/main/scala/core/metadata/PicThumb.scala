package core.metadata

import java.io.File
import java.net.URI
import javax.imageio.ImageIO

import akka.actor.{Actor, Props}
import akka.actor.Actor.Receive
import core.{JobHandling, JobProtocol, JobReply, ThumbnailJob}
import org.imgscalr.Scalr

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class PicThumb extends ThumbActor
{

  override def createThumbnail(job: ThumbnailJob): Unit = {
    val imgBuff = ImageIO.read(job.src)
    val pic = Scalr.resize(imgBuff, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC,job.config.thumbResolution.toInt, job.config.thumbResolution.toInt,
      Scalr.OP_ANTIALIAS)
    ImageIO.write(pic, "jpeg", new File(new URI(job.dest + job.config.thumbnail)))
    pic.flush()
  }

}
