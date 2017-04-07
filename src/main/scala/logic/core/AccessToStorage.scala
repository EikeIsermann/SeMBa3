package logic.core

import java.util.UUID

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, Props}
import api._
import data.storage.{AccessMethods, SembaStorage, SembaStorageComponent, StorageAccess}
import globalConstants.GlobalMessages.StorageReadRequest
import logic.core.AccessToStorageMethods._
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
  var queryExecutor: ActorRef = system.actorOf(StorageQueryPipeline.props(), ("StorageQueryPipeline" + UUID.randomUUID()))

  abstract override def initialization(): Unit = {
    sembaStorage = initializeFeature(SembaStorage.props(libInfo), "SembaStorage" + UUID.randomUUID())
    queryExecutor ! StorageRegistration(sembaStorage)
    super.initialization()
  }
  override def receive: Receive = {
    case openLib: OpenLib => {
      acceptJob(openLib, sender)
      queryExecutor ! createJob(ConceptRequest(libInfo), openLib)
    }


    case contents: RequestContents => {
      acceptJob(contents, sender)
      queryExecutor ! createJob(ContentRequest(libInfo), contents)
    }


    case getMeta: GetMetadata => {
      acceptJob(getMeta, sender)
      queryExecutor ! createJob(MetadataRequest(getMeta.resource.uri), getMeta)
    }

    case collectionContents: RequestCollectionContents => {
      acceptJob(collectionContents, sender)
      queryExecutor ! createJob(CollectionContentRequest(collectionContents.resource.uri, libInfo), collectionContents)
    }


    case x => super.receive(x)
  }

  abstract override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray): Unit = {
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
  case class ConceptRequest(config:LibInfo) extends StorageReadRequest(requestConcepts(config, _))


  def requestConcepts(config: LibInfo, storage: SembaStorageComponent): JobResult = {
    storage.performRead(
         JobResult(ConceptResult(AccessMethods.retrieveLibConcepts(storage.getTBox(),config)))
    )
  }

  case class ContentResult(payload: LibraryContent)
    extends ResultContent
  case class ContentRequest(config: LibInfo) extends StorageReadRequest(requestContent(config, _ ))


  def requestContent(config: LibInfo, storage: SembaStorageComponent): JobResult = {
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

  case class CollectionContentRequest(item: String, config: LibInfo) extends StorageReadRequest(requestCollectionContent(item, config, _))
  case class CollectionContentResult(payload: CollectionContent) extends ResultContent
  def requestCollectionContent(item: String, config: LibInfo,  storage: SembaStorageComponent): JobResult = {
    storage.performRead(
      JobResult(CollectionContentResult(AccessMethods.retrieveCollectionContent(storage.getABox, item, config))

    )
    )
  }



}


