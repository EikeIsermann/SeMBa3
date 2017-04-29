package logic.resourceCreation.metadata

import java.io.File
import java.net.URI
import java.util.UUID
import javax.imageio.ImageIO

import logic.core.jobHandling.ResultContent
import logic.resourceCreation.metadata.MetadataMessages.{ExtractThumbnail, ThumbnailResult}
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
    * @param thumb [[ExtractThumbnail]] containing the required information
    */
  override def createThumbnail(thumb: ExtractThumbnail): ResultContent = {
    val destination = createPath(thumb)
    val doc = PDDocument.load(thumb.src)
    val renderer = new PDFRenderer(doc)
    val imgBuff = renderer.renderImage(0)
    Scalr.resize(imgBuff, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, thumb.config.constants.thumbResolution.toInt, thumb.config.constants.thumbResolution.toInt,
      Scalr.OP_ANTIALIAS)
    ImageIO.write(imgBuff, "jpeg", destination)
    imgBuff.flush()
    doc.close()
    ThumbnailResult(thumb.path)
  }
}

