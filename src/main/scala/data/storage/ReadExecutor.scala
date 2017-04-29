package data.storage

import akka.actor.{Actor, ActorRef, Props}
import akka.actor.Actor.Receive
import globalConstants.GlobalMessages.{StorageReadRequest, StorageWriteRequest}
import logic.core._
import logic.core.jobHandling._

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class ReadExecutor(val config: Config) extends LibJobExecutor {
  val storage = SembaStorageComponent.getStorage(config)
  override def performTask(job: Job): JobResult = {
    job match {
      case read: StorageReadRequest => read.operation(storage)
      case _ => JobResult(ErrorResult())
    }
  }
}

object ReadExecutor {
  def props(config: Config): Props = Props(new ReadExecutor(config))
}
