package logic.resourceCreation.metadata

import java.io.File
import java.net.URI

import logic.core.{Config, JobProtocol, ResultContent}
import logic.resourceCreation.ImportNewItem
import sembaGRPC.ItemDescription


/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object MetadataMessages {
  case class ExtractMetadata(src: File) extends JobProtocol
  case class MetadataResult(payload: ItemDescription) extends ResultContent
  case class ExtractThumbnail(src: File, path: URI, config: Config) extends JobProtocol
  case class ThumbnailResult(payload: URI) extends ResultContent
  case class ExtractionJob(importJob: ImportNewItem) extends JobProtocol
  case class StorageJob(importJob: ImportNewItem) extends JobProtocol


}
