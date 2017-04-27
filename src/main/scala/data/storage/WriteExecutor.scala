package data.storage

import akka.actor.{Actor, Props}
import globalConstants.GlobalMessages.StorageWriteRequest
import logic.core._
import logic.core.jobHandling._
import utilities.debug.DC

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class WriteExecutor(val config: Config) extends Actor with SingleJobExecutor {
  val storage = SembaStorageComponent.getStorage(config)
  storage.initialize()
  context.parent ! InitializedStorage()

  override def performTask(job: Job): JobResult = {
    job match{

      case write: StorageWriteRequest => {
        val retVal = write.operation(storage)
        retVal

      }
      case _ => JobResult(ErrorResult())
    }

  }
}
object WriteExecutor {
  def props(config: Config): Props = Props(new WriteExecutor(config))
}