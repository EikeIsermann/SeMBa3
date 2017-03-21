package core.metadata

import java.net.URI

import core.JobResult
import core.metadata.MetadataMessages.{ExtractThumbnail, ThumbnailResult}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
//TODO implement VLCJ Library

class MultiMediaThumb extends ThumbActor {
  override def createThumbnail(thumb: ExtractThumbnail): JobResult = {
   ThumbnailResult(URI.create("TODO"))
  }
}
