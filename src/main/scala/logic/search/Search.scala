package logic.search

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.RoundRobinPool
import api.{RequestResult, SparqlFilter}
import logic.core._
import logic.core.jobHandling.{Job, ResultArray}
import logic.itemModification.ModificationHandler
import logic.search.SearchMethods.SparqlFilterResult
import sembaGRPC.{FilterResult, LibraryContent}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait Search extends SembaBaseActor with AccessToStorage  {
  val sparqlSearch: ActorRef = context.actorOf(new RoundRobinPool(10).props(SparqlSearch.props(config)))

  abstract override def handleJob(job: Job, master: ActorRef): Unit = {
     job match {
       case sparql: SparqlFilter => {
         sparqlSearch ! forwardJob(sparql, context.sender)
       }
       case x ⇒ super.handleJob(x, master)
     }
  }


  abstract override def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = {
    job match {
      case filter: SparqlFilter => {
        master ! results.extract(classOf[FilterResult])
      }
      case _ => super.finishedJob(job,master,results)
    }
  }
}
