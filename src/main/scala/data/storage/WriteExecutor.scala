package data.storage

import akka.actor.{Actor, Props}
import globalConstants.GlobalMessages.StorageWriteRequest
import logic.core._
import utilities.debug.DC

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class WriteExecutor(config: LibInfo) extends Actor with JobExecution {
  val storage = SembaStorageComponent.getStorage(config)
  storage.initialize()
  context.parent ! InitializedStorage()
  override def handleJob(job: JobProtocol): JobResult = {
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
  def props(config: LibInfo): Props = Props(new WriteExecutor(config))
}