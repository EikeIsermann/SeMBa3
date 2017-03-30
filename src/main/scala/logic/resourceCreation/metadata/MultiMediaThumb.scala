package logic.resourceCreation.metadata

import java.net.URI

import logic.core.{JobResult, ResultContent}
import logic.resourceCreation.metadata.MetadataMessages.{ExtractThumbnail, ThumbnailResult}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
//TODO implement VLCJ Library

class MultiMediaThumb extends ThumbActor {
  override def createThumbnail(thumb: ExtractThumbnail): ResultContent = {
   ThumbnailResult(URI.create("TODO"))
  }
}
