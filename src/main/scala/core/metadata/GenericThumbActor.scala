package core.metadata
import java.io.File
import java.net.URI

import core.ThumbnailJob
import utilities.WriterFactory

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class GenericThumbActor extends ThumbActor {
  override def createThumbnail(thumb: ThumbnailJob): Unit = {
    WriterFactory.writeFile(new File(URI.create(thumb.config.defaultCollectionIcon)), new File(thumb.dest))
  }
}
