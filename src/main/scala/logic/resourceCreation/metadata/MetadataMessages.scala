package logic.resourceCreation.metadata

import java.io.File
import java.net.URI

import logic.core.{Config, JobProtocol, JobResult}
import logic.resourceCreation.ImportNewItem
import sembaGRPC.ItemDescription

import scala.collection.immutable.HashMap

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object MetadataMessages {
  case class ExtractMetadata(src: File) extends JobProtocol
  case class MetadataResult(content: ItemDescription) extends JobResult
  case class ExtractThumbnail(src: File, config: Config) extends JobProtocol
  case class ThumbnailResult(content: URI) extends JobResult
  case class ExtractionJob(importJob: ImportNewItem) extends JobProtocol
  case class StorageJob(importJob: ImportNewItem) extends JobProtocol


}
