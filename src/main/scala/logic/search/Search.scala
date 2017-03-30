package logic.search

import akka.actor.{Actor, ActorRef, Props}
import api.SparqlFilter
import logic.core.{Search, _}
import sembaGRPC.LibraryContent

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait Search extends SembaBaseActor with AccessToStorage  {
abstract  override def initialization(): Unit = ???
abstract  override def receive: Receive = {

     case sparql: SparqlFilter => {

    }
    case x => super.receive(x)
  }
  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray): Unit = ???
}