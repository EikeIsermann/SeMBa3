package logic.itemModification

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.RoundRobinPool
import api._
import logic.core._
import sembaGRPC.VoidResult

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait ItemModification extends SembaBaseActor with AccessToStorage {
val modificationHandler: ActorRef = context.actorOf(new RoundRobinPool(10).props(ModificationHandler.props(libInfo)))

abstract override def initialization(): Unit = {
    super.initialization()
  }

  override def receive: Receive = {
    case mod: JobProtocol with LibModification => {
      sender() ! VoidResult(true, "Modification received")
      modificationHandler ! createMasterJob(mod)
    }

    case x => super.receive(x)
  }
  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray): Unit = ???
}
