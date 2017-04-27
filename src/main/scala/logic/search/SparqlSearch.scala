package logic.search

import akka.actor.{Actor, ActorRef, Props}
import api.SparqlFilter
import logic.core._
import logic.core.jobHandling.{Job, JobHandling, JobReply}
import logic.search.SearchMethods.ApplySparqlFilter
import org.apache.jena.ontology.OntModel
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.shared.Lock

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */



//TODO SimpleSearch, Sparql Request, JobHandler
class SparqlSearch(val config: Config) extends Actor with ActorFeatures with JobHandling {

  override def handleJob(job: Job, master: ActorRef): Unit = {
    job match {
      case filter: SparqlFilter => {
        acceptJob(filter, sender())
        val newJob =  ApplySparqlFilter(filter.sparqlQuery.queryString, filter.sparqlQuery.vars)
        config.libAccess ! createJob(newJob, filter)
      }
    }

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
  def props(config: Config) = Props(new SparqlSearch(config))


}