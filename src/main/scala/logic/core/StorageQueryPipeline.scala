package logic.core

import akka.actor.{Actor, ActorRef, Props}
import globalConstants.GlobalMessages.{StorageOperation}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
case class StorageRegistration(storage: ActorRef)
class StorageQueryPipeline() extends Actor with ActorFeatures with JobHandling {
  var storage: ActorRef = _

  def wrappedReceive: Receive = {
    case job: StorageOperation => {
      acceptJob(job,sender)
      storage ! createJob(job,job)
    }
    case registerStorage: StorageRegistration => {
      storage = registerStorage.storage
    }
  }

   override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray): Unit = {
     master ! JobReply(job, results)
   }
}

object StorageQueryPipeline {

  def props(): Props = Props(new StorageQueryPipeline())
}

