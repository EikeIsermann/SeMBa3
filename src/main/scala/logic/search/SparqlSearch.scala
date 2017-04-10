package logic.search

import akka.actor.{Actor, ActorRef, Props}
import api.SparqlFilter
import logic.core._
import logic.search.SearchMethods.ApplySparqlFilter
import org.apache.jena.ontology.OntModel
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.shared.Lock

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */



//TODO SimpleSearch, Sparql Request, JobHandler
class SparqlSearch(config: LibInfo) extends Actor with ActorFeatures with JobHandling {
val storagePipeline = config.libAccess


  def wrappedReceive: Receive = {
    case filter: SparqlFilter => {
      acceptJob(filter, sender())
      val newJob =  ApplySparqlFilter(filter.sparqlQuery.queryString, filter.sparqlQuery.vars)
      storagePipeline ! createJob(newJob, filter)
    }

    case reply: JobReply => handleReply(reply)
  }
      /*
  override def handleJob(jobProtocol: JobProtocol): JobReply = {
    jobProtocol match {
      case sparql: SparqlFilter => performSparqlQuery(sparql.sparqlQuery.key)
    }
     JobReply(jobProtocol)
  }

     */

}


object SparqlSearch {
  def props(config: LibInfo) = Props(new SparqlSearch(config))


}