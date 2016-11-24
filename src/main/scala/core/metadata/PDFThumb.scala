package core.metadata

import java.io.File
import java.net.URI
import javax.imageio.ImageIO

import core.ThumbnailJob
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.tools.PDFBox
import org.imgscalr.Scalr


/** Saves a PDF thumbnail at the given location
  *
  */
class PdfThumb extends ThumbActor {
  /** Renders the first page of a PDF document as a jpeg image using Apache PDFBox. Adjusts size and writes to
    * ThumbnailPath.
    *
    * @param thumb [[ThumbnailJob]] containing the required information
    */
  override def createThumbnail(thumb: ThumbnailJob): Unit = {
    val doc = PDDocument.load(thumb.src)
    val renderer = new PDFRenderer(doc)
    val imgBuff = renderer.renderImage(0)
    Scalr.resize(imgBuff, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, thumb.config.thumbResolution.toInt, thumb.config.thumbResolution.toInt,
      Scalr.OP_ANTIALIAS)
    ImageIO.write(imgBuff, "jpeg", new File(new URI(thumb.dest + thumb.config.thumbnail)))


    imgBuff.flush()
    doc.close()
  }
}

