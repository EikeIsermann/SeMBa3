package logic.resourceCreation

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import api.{AddToLibrary, RequestResult}
import app.Application
import logic.core._
import logic.core.jobHandling.{Job, ResultArray}
import sembaGRPC.VoidResult
import utilities.debug.DC

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait ResourceCreation extends SembaBaseActor with AccessToStorage {
  var resourceCreator: ActorRef = _

abstract override def initialization(): Unit = {
    resourceCreator = initializeFeature(ResourceCreator.props(config), "ResourceCreator"+ UUID.randomUUID())
    super.initialization()
  }

 override def receive: Receive = {
    case addItem: AddToLibrary => {
      val notEmpty = addItem.sourceFile.source.isDefined
      sender() ! VoidResult(notEmpty, if (notEmpty) "Trying to import Item." else "No source file set.")
      //acceptJob(addItem, sender)
      resourceCreator ! forwardJob(addItem, sender)
    }
    case x => super.receive(x)
    }

 abstract override def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = {
  job match {
    case addItem: AddToLibrary => {
    results.processUpdates.foreach(update => Application.api ! update)
  }
  case _ => super.finishedJob(job,master,results)
}
}
}
