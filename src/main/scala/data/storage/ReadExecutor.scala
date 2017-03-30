package data.storage

import akka.actor.{Actor, ActorRef, Props}
import akka.actor.Actor.Receive
import globalConstants.GlobalMessages.{StorageReadRequest, StorageWriteRequest}
import logic.core._

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class ReadExecutor(storage: SembaStorageComponent) extends Actor with JobExecution {
  override def handleJob(job: JobProtocol): JobResult = {
    job match {
      case read: StorageReadRequest => read.operation(storage)
      case _ => JobResult(ErrorResult())
    }
  }
}

object ReadExecutor {
  def props(storage: SembaStorageComponent): Props = Props(new ReadExecutor(storage))
}
