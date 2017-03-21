package core.metadata

import java.io.File
import java.net.URI

import core.library.ImportNewItem
import core.{Config, JobProtocol, JobResult}

import scala.collection.immutable.HashMap

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object MetadataMessages {
  case class ExtractMetadata(src: File) extends JobProtocol
  case class MetadataResult(data: HashMap[String, Array[String]]) extends JobResult
  case class ExtractThumbnail(src: File, config: Config) extends JobProtocol
  case class ThumbnailResult(path: URI) extends JobResult

  case class ExtractionJob(importJob: ImportNewItem) extends JobProtocol



}
