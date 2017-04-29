package logic.resourceCreation.metadata

import java.awt.font.FontRenderContext
import java.awt.image.BufferedImage
import java.awt.{Color, Font, RenderingHints}
import java.io.File
import java.net.URI
import java.util
import java.util.UUID
import javax.imageio.ImageIO

import logic.core.jobHandling.ResultContent
import logic.resourceCreation.metadata.MetadataMessages.{ExtractThumbnail, ThumbnailResult}
import org.apache.commons.io.IOUtils
import org.imgscalr.Scalr.pad

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class TxtThumb extends ThumbActor {

  override def createThumbnail(thumb: ExtractThumbnail): ResultContent = {
    val src = IOUtils.toString(thumb.src.toURI)
    val destination = createPath(thumb)

    val res = thumb.config.constants.thumbResolution.toInt - TxtThumb.padding
    val thumbnail = new BufferedImage(res, res, BufferedImage.TYPE_INT_RGB)
    val bounds = TxtThumb.font.getStringBounds(src, new FontRenderContext(null, true, true))
    val pic = thumbnail.createGraphics()
    pic.setColor(TxtThumb.background)
    pic.fillRect(0, 0, res, res)
    pic.setColor(TxtThumb.color)
    pic.setFont(TxtThumb.font)
    pic.setRenderingHints(TxtThumb.RenderingProperties)

    val metrics = pic.getFontMetrics(TxtThumb.font)
    val height = metrics.getHeight
    val xOrigin = (bounds.getX.toInt + res * 0.2).toInt
    var xPos = xOrigin
    var yPos = (bounds.getY.toInt + res * 0.2).toInt

    def breakLine {
      yPos += height; xPos = xOrigin
    }

    breakLine
    src.split("\n|\r\n").foreach { line =>
      line.split(" ").foreach { word =>
        val wordWidth = metrics.stringWidth(word + " ")

        pic.drawString(word, xPos, yPos)

        if (xPos + wordWidth >= xOrigin + (res * 0.8).toInt) breakLine
        else xPos += wordWidth
      }

      breakLine
    }

    pic.dispose

    ImageIO.write(pad(thumbnail, TxtThumb.padding / 2, TxtThumb.background), "JPEG", destination)
    thumbnail.flush()
    ThumbnailResult(thumb.path)
  }


}

object TxtThumb {
  val RenderingProperties = new util.HashMap[RenderingHints.Key, Object]()
  RenderingProperties.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
  RenderingProperties.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
  RenderingProperties.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)

  val font = new Font("Tahoma", Font.PLAIN, 5)
  val color = Color.black
  val background = Color.white
  val padding = 4
}
