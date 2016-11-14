package core.metadata

import java.io.File
import java.net.URI
import javax.imageio.ImageIO

import core.ThumbnailJob
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.imgscalr.Scalr

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class PdfThumb extends ThumbActor {
  override def createThumbnail(thumb: ThumbnailJob): Unit = {

    val doc =  PDDocument.load(thumb.src)
    val renderer = new PDFRenderer(doc)
    val imgBuff = renderer.renderImage(0)
    Scalr.resize(imgBuff, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC,thumb.config.thumbResolution.toInt, thumb.config.thumbResolution.toInt,
      Scalr.OP_ANTIALIAS)
    ImageIO.write(imgBuff, "jpeg", new File(new URI(thumb.dest + thumb.config.thumbnail)))
    imgBuff.flush()
  }
}

