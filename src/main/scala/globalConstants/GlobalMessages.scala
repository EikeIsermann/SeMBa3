package globalConstants

import data.storage.SembaStorageComponent
import logic.core.{JobProtocol, JobResult}
import sembaGRPC.UpdateMessage

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object GlobalMessages {

  case class UpdateResult(messages: ArrayBuffer[UpdateMessage]) extends JobResult

  abstract class StorageOperation extends JobProtocol

  case class StorageReadRequest() extends StorageOperation

  case class StorageWriteRequest(operation: (SembaStorageComponent => UpdateMessage)) extends StorageOperation

}
