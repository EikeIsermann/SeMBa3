package globalConstants

import data.storage.SembaStorageComponent
import logic.core.jobHandling.{Job, JobResult, ResultContent}
import sembaGRPC.{ItemDescription, UpdateMessage}

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object GlobalMessages {


  case class StorageWriteResult(payload: UpdateMessage) extends ResultContent

  abstract class StorageOperation() extends Job

  abstract class StorageReadRequest(val operation: (SembaStorageComponent => JobResult)) extends StorageOperation

  abstract  class StorageWriteRequest(val operation: (SembaStorageComponent => UpdateMessage)) extends StorageOperation


  }



