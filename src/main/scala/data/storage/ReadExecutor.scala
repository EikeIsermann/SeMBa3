package data.storage

import akka.actor.{Actor, ActorRef, Props}
import akka.actor.Actor.Receive
import globalConstants.GlobalMessages.{StorageReadRequest, StorageWriteRequest}
import logic.core._

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class ReadExecutor(config: LibInfo) extends Actor with JobExecution {
  val storage = SembaStorageComponent.getStorage(config)
  override def handleJob(job: JobProtocol): JobResult = {
    job match {
      case read: StorageReadRequest => read.operation(storage)
      case _ => JobResult(ErrorResult())
    }
  }
}

object ReadExecutor {
  def props(config: LibInfo): Props = Props(new ReadExecutor(config))
}
