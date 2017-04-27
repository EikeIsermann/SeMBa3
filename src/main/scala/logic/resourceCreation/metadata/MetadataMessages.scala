package logic.resourceCreation.metadata

import java.io.File
import java.net.URI

import logic.core.jobHandling.{Job, ResultContent}
import logic.core.Constants
import logic.resourceCreation.ImportNewItem
import sembaGRPC.ItemDescription


/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object MetadataMessages {
  case class ExtractMetadata(src: File) extends Job
  case class MetadataResult(payload: ItemDescription) extends ResultContent
  case class ExtractThumbnail(src: File, path: URI, config: Constants) extends Job
  case class ThumbnailResult(payload: URI) extends ResultContent
  case class ExtractionJob(importJob: ImportNewItem) extends Job
  case class StorageJob(importJob: ImportNewItem) extends Job


}
