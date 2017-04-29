package logic.core

import java.util.UUID

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, Props}
import api._
import data.storage.{AccessMethods, SembaStorage, SembaStorageComponent}
import globalConstants.GlobalMessages.StorageReadRequest
import logic.core.AccessToStorageMethods._
import logic.core.jobHandling.{Job, JobResult, ResultArray, ResultContent}
import sembaGRPC.{CollectionContent, ItemDescription, LibraryConcepts, LibraryContent}
import utilities.SembaConstants.StorageSolution.StorageSolution
import utilities.debug.DC

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
case class StorageInitialization(storageType: StorageSolution, storagePath: String)

trait AccessToStorage extends SembaBaseActor {//with Actor with ActorFeatures with JobHandling {

  var sembaStorage: ActorRef = _
  var queryExecutor: ActorRef = system.actorOf(StorageQueryPipeline.props(config), ("StorageQueryPipeline" + UUID.randomUUID()))

  abstract override def initialization(): Unit = {
    sembaStorage = initializeFeature(SembaStorage.props(config), "SembaStorage" + UUID.randomUUID())
    queryExecutor ! StorageRegistration(sembaStorage)
    super.initialization()
  }
  abstract override def handleJob(job: Job, master: ActorRef): Unit = {
    job match {
      case openLib: OpenLib => {
        acceptJob(openLib, sender)
        queryExecutor ! createJob(ConceptRequest(config), openLib)
      }


      case contents: RequestContents => {
        acceptJob(contents, sender)
        queryExecutor ! createJob(ContentRequest(config), contents)
      }


      case getMeta: GetMetadata => {
        acceptJob(getMeta, sender)
        queryExecutor ! createJob(MetadataRequest(getMeta.resource.uri), getMeta)
      }

      case collectionContents: RequestCollectionContents => {
        acceptJob(collectionContents, sender)
        queryExecutor ! createJob(CollectionContentRequest(collectionContents.resource.uri, config), collectionContents)
      }
      case x => super.handleJob(job, master)

    }

  }

  abstract override def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = {
     job match {
       case request: RequestResult => {
         master ! results.extract(request.resultClass)
       }
      case _ => super.finishedJob(job,master,results)
    }
  }
}

object AccessToStorageMethods
{

  case class ConceptResult(payload: LibraryConcepts)
    extends ResultContent
  case class ConceptRequest(config:Config) extends StorageReadRequest(requestConcepts(config, _))


  def requestConcepts(config: Config, storage: SembaStorageComponent): JobResult = {
    storage.performRead(
         JobResult(ConceptResult(AccessMethods.retrieveLibConcepts(storage.getTBox(),config)))
    )
  }

  case class ContentResult(payload: LibraryContent)
    extends ResultContent
  case class ContentRequest(config: Config) extends StorageReadRequest(requestContent(config, _ ))


  def requestContent(config: Config, storage: SembaStorageComponent): JobResult = {
    storage.performRead(
    JobResult(ContentResult(AccessMethods.retrieveLibContent(storage.getABox, config)))
    )
  }

  case class MetadataRequest(item: String) extends StorageReadRequest(requestMetadata(item, _))
  case class MetadataResult(payload: ItemDescription)
    extends ResultContent
  def requestMetadata(uri: String, storage: SembaStorageComponent): JobResult = {
    storage.performRead(
      JobResult(MetadataResult(AccessMethods.retrieveMetadata(uri, storage.getABox())))
    )
  }

  case class CollectionContentRequest(item: String, config: Config) extends StorageReadRequest(requestCollectionContent(item, config, _))
  case class CollectionContentResult(payload: CollectionContent) extends ResultContent
  def requestCollectionContent(item: String, config: Config, storage: SembaStorageComponent): JobResult = {
    storage.performRead(
      JobResult(CollectionContentResult(AccessMethods.retrieveCollectionContent(storage.getABox, item, config))

    )
    )
  }



}


