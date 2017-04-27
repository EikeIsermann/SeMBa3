package logic.core

import akka.actor.{Actor, ActorRef, Props}
import globalConstants.GlobalMessages.StorageOperation
import logic.core.jobHandling.{Job, JobHandling, JobReply, ResultArray}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
case class StorageRegistration(storage: ActorRef)
class StorageQueryPipeline(val config: Config) extends LibActor {
  var storage: ActorRef = _

  override def handleJob(job: Job, master: ActorRef): Unit = {
    job match {
      case job: StorageOperation => {
        acceptJob(job, sender)
        storage ! createJob(job, job)
      }
    }
  }

  override def receive: Receive = {
    case registerStorage: StorageRegistration => {
      storage = registerStorage.storage
    }
    case x ⇒ super.receive(x)
  }


   override def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = {
     master ! JobReply(job, results)
   }


}

object StorageQueryPipeline {

  def props(config: Config): Props = Props(new StorageQueryPipeline(config: Config))
}

