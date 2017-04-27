package logic.core.jobHandling

import globalConstants.GlobalMessages.StorageWriteResult
import sembaGRPC.UpdateMessage

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a Default (Template) Project class
  */
class ResultArray(input: ArrayBuffer[JobResult] = ArrayBuffer[JobResult]()) {
  def this(single: JobResult) = {
    this(ArrayBuffer[JobResult](single))
  }

  var results = input
  def get[T <: ResultContent]( aClass: Class[T]): T  = {
    results.find(x => x.content.getClass.equals(aClass)).get.content.asInstanceOf[T]
  }
  def getAll[T <: ResultContent]( aClass: Class[T]): ArrayBuffer[T] = {
    results.filter( x => x.content.getClass.equals(aClass)).map( x => x.content.asInstanceOf[T])
  }

  def processUpdates(): ArrayBuffer[UpdateMessage] = {
    getAll(classOf[StorageWriteResult]).map(x => x.payload)
  }

  def extract[T]( aClass: Class[T]): T  = {
    results.find(x => x.content.payload.getClass.equals(aClass)).get.content.payload.asInstanceOf[T]
    //this.find(x => x.content.
    //x.content.content.getClass.equals(aClass)).get.content.asInstanceOf[T]
  }
  def add(toAdd: ArrayBuffer[JobResult]) =
  {
    results = results ++ toAdd
  }

}
