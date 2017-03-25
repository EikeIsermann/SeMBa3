package logic.resourceCreation.metadata

import java.io.File
import java.net.URI

import logic.core.JobResult
import logic.resourceCreation.metadata.MetadataMessages.{ExtractThumbnail, ThumbnailResult}
import utilities.WriterFactory


/** ThumbActor for all files that are not covered by another ThumbActor
  *
  */
//TODO copies the default thumbnail. Just a reference would be smarter.
class GenericThumbActor extends ThumbActor {
  override def createThumbnail(thumb: ExtractThumbnail): JobResult = {
    ThumbnailResult(URI.create(thumb.config.defaultCollectionIcon))
  }
}
