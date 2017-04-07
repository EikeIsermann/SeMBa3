package logic.resourceCreation

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import api.{AddToLibrary, RequestResult}
import app.Application
import logic.core._
import sembaGRPC.VoidResult
import utilities.debug.DC

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait ResourceCreation extends SembaBaseActor with AccessToStorage {
  var resourceCreation: ActorRef = _

abstract override def initialization(): Unit = {
    resourceCreation = initializeFeature(ResourceCreator.props(libInfo), "ResourceCreator"+ UUID.randomUUID())
    super.initialization()
  }

 override def receive: Receive = {
    case addItem: AddToLibrary => {
      val notEmpty = addItem.sourceFile.source.isDefined
      sender() ! VoidResult(notEmpty, if (notEmpty) "Trying to import Item." else "No source file set.")
      //acceptJob(addItem, sender)
      executionTime.+=((addItem.jobID, System.currentTimeMillis()))
      resourceCreation ! forwardJob(addItem, sender)
    }
    case x => super.receive(x)
    }

 abstract override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray): Unit = {
  job match {
    case addItem: AddToLibrary => {
    DC.log("Import of file took" + (System.currentTimeMillis() - executionTime.apply(addItem.jobID) ))
    results.processUpdates.foreach(update => Application.api ! update)
  }
  case _ => super.finishedJob(job,master,results)
}
}
}
