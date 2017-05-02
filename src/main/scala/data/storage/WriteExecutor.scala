package data.storage

import akka.actor.{Actor, Props}
import globalConstants.GlobalMessages.{StorageWriteRequest, StorageWriteResult}
import logic.core._
import logic.core.jobHandling._
import utilities.debug.DC

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class WriteExecutor(val config: Config) extends LibJobExecutor {
  val storage = SembaStorageComponent.getStorage(config)

  override def wrappedReceive: Receive = {
    case i: InitializedStorage => {
      storage.initialize()
      context.sender ! InitializedStorage()
    }
  }

  override def performTask(job: Job): JobResult = {
    job match{

      case write: StorageWriteRequest => {
        val retVal = write.operation(storage)
        JobResult(StorageWriteResult(retVal.withJobID(job.parentCall.toString)))
      }
      case _ => JobResult(ErrorResult())
    }
  }
}
object WriteExecutor {
  def props(config: Config): Props = Props(new WriteExecutor(config))
}