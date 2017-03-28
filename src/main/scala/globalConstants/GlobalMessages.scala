package globalConstants

import data.storage.SembaStorageComponent
import logic.core.{JobProtocol, JobResult}
import sembaGRPC.{ItemDescription, UpdateMessage}

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object GlobalMessages {

  case class UpdateResult(messages: ArrayBuffer[UpdateMessage]) extends JobResult

  abstract class StorageOperation() extends JobProtocol

  abstract case class StorageReadRequest[T](operation: (SembaStorageComponent => T)) extends StorageOperation

  case class StorageWriteRequest(operation: (SembaStorageComponent => UpdateResult)) extends StorageOperation

  case class ReadMetadataResult(itemDescription: ItemDescription) extends JobResult
  case class ReadMetadataRequest() extends StorageReadRequest(test(_ : SembaStorageComponent))
  def test(sembaStorageComponent: SembaStorageComponent): ReadMetadataResult = {
    ReadMetadataResult(ItemDescription())
  }


}
