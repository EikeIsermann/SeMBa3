package data.storage

import akka.actor.{Actor, Props}
import globalConstants.GlobalMessages.StorageWriteRequest
import logic.core._

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class WriteExecutor(storage: SembaStorageComponent) extends Actor with JobExecution {
  override def handleJob(job: JobProtocol): JobResult = {
    job match{
      case write: StorageWriteRequest => write.operation(storage)
      case _ => JobResult(ErrorResult())
    }

  }
}
object WriteExecutor {
  def props(storage: SembaStorageComponent): Props = Props(new WriteExecutor(storage))
}