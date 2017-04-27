package logic.resourceCreation.metadata

import java.io.File
import java.net.URI
import java.util.UUID
import javax.imageio.ImageIO

import logic.core.jobHandling.ResultContent
import logic.resourceCreation.metadata.MetadataMessages.{ExtractThumbnail, ThumbnailResult}
import org.imgscalr.Scalr

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class PicThumb extends ThumbActor {

  override def createThumbnail(job: ExtractThumbnail): ResultContent = {
    val destination = createPath(job)
    val imgBuff = ImageIO.read(job.src)
    val pic = Scalr.resize(imgBuff, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, job.config.thumbResolution.toInt, job.config.thumbResolution.toInt,
      Scalr.OP_ANTIALIAS)
    ImageIO.write(pic, "jpeg", destination)
    pic.flush()
    ThumbnailResult(job.path)
  }

}
