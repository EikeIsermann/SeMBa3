package logic.search

import data.storage.SembaStorageComponent
import globalConstants.GlobalMessages.StorageReadRequest
import logic.core.{JobResult, ResultContent}
import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.shared.Lock
import sembaGRPC.{FilterResult, ResultEntry}
import utilities.debug.DC

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object SearchMethods {

  case class ApplySparqlFilter(strQuery: String, searchVariables: Seq[String]) extends StorageReadRequest(applySparqlFilter(strQuery, searchVariables, _))

  case class SparqlFilterResult(payload: FilterResult) extends ResultContent

  def applySparqlFilter(strQuery: String, vars: Seq[String], storage: SembaStorageComponent): JobResult = {
    var retVal = FilterResult()
    storage.performRead(
      {
        DC.log("Searching.")

        val model = storage.getABox()
        val query = QueryFactory.create(strQuery)
        val qe = QueryExecutionFactory.create(query, model)
        val res = qe.execSelect()
        while (res.hasNext) {
          val result = res.next()
          retVal = retVal.addResults(
            ResultEntry().addAllResults(
              vars.foldLeft(ArrayBuffer[(String, String)]()) { case (buff, x) =>
                buff.+=((x, result.get(x).toString))
              }
            )
          )
        }
      }
    )
    JobResult(SparqlFilterResult(retVal))
  }
}
