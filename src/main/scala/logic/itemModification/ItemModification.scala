package logic.itemModification

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.RoundRobinPool
import api._
import app.Application
import logic.core._
import logic.core.jobHandling.{Job, ResultArray}
import sembaGRPC.VoidResult

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait ItemModification extends SembaBaseActor with AccessToStorage {
val modificationHandler: ActorRef = context.actorOf(new RoundRobinPool(10).props(ModificationHandler.props(config)))

abstract override def initialization(): Unit = {
    super.initialization()
  }

  abstract override def handleJob(job: Job, master: ActorRef): Unit = {
    job match {
      case mod: LibModification => {
        sender() ! VoidResult(true, "Modification received")
        modificationHandler ! forwardJob(mod, sender)
      }

      case x => super.handleJob(x, master)
    }

  }
  abstract override def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = {
    job match {
      case mod: LibModification => {
        results.processUpdates.foreach(update => Application.api ! update)
      }
      case _ => super.finishedJob(job, master, results)
    }
  }
  //override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray): Unit = {
}

