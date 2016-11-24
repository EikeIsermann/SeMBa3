package core.library

import akka.actor.Actor
import api.SparqlFilter
import core.{JobHandling, JobProtocol, JobReply, LibraryAccess}
import org.apache.jena.ontology.OntModel
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory, ResultSetFormatter}
import sembaGRPC.SparqlQuery

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */



//TODO SimpleSearch, Sparql Request, JobHandler
class Search(model: OntModel) extends Actor with JobHandling {
  var searchableModel: OntModel = _
  /*
  override def preStart(): Unit = {
    val time = System.currentTimeMillis()
    searchableModel = LibraryAccess.getDeepCopy(model)
    println("Deepcopy took " + (System.currentTimeMillis() - time))

  }
      */
  override def receive: Receive = {
    case job: JobProtocol => {
      acceptJob(job, sender())
      self ! handleJob(job)
    }

    case reply: JobReply => handleReply(reply, self)
  }

  override def handleJob(jobProtocol: JobProtocol): JobReply = {
    jobProtocol match {
      case sparql: SparqlFilter => performSparqlQuery(sparql.sparqlQuery.key)
    }
     JobReply(jobProtocol)
  }



 def performSparqlQuery(strQuery: String) = {
     val time = System.currentTimeMillis()
   var count = 1
     val query = QueryFactory.create(strQuery)
     val qe = QueryExecutionFactory.create(query, model)
     val res = qe.execSelect()
     while(res.hasNext){
       res.next()
       count += 1
     }
     qe.close()
      println("Query took " + (System.currentTimeMillis() - time) + " model size: " + model.size() + ": number of hits: " + count)

 }
}


object Search {


}