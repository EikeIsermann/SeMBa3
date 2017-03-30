package globalConstants

import data.storage.SembaStorageComponent
import logic.core.{JobProtocol, JobResult, ResultContent}
import sembaGRPC.{ItemDescription, UpdateMessage}

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object GlobalMessages {


  case class UpdateResult(payload: ArrayBuffer[UpdateMessage]) extends ResultContent

  abstract class StorageOperation() extends JobProtocol

  abstract class StorageReadRequest(val operation: (SembaStorageComponent => JobResult)) extends StorageOperation

  abstract  class StorageWriteRequest(val operation: (SembaStorageComponent => JobResult)) extends StorageOperation


  }



