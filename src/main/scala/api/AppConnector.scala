package api
/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

import java.net.URI
import java.util.UUID
import java.util.logging.Logger

import akka.actor.Actor
import akka.pattern._
import akka.util.Timeout
import app.Application
import globalConstants.SembaPresets
import io.grpc.stub.StreamObserver
import io.grpc.{Server, ServerBuilder}
import logic.core.LibraryInstance
import logic.core.jobHandling.Job
import logic.core.jobHandling.benchmark.WriteResults
import sembaGRPC.{CollectionItem, Library, LibraryConcepts, SimpleQuery, _}
import utilities.FileFactory

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


/**
  * Case classes for sending gRPC calls to Semba Actors
  */
abstract class SembaApiCall extends Job

trait RequestResult{val resultClass: Class[_]}
trait LibModification

case class OpenLib(resultClass: Class[_] = classOf[LibraryConcepts])
  extends SembaApiCall with RequestResult

case class GetMetadata(resource: Resource,
                       resultClass: Class[_] = classOf[ItemDescription])
  extends SembaApiCall with RequestResult

case class RequestContents(library: Library,
                           resultClass: Class[_] = classOf[LibraryContent])
  extends SembaApiCall with RequestResult

case class RequestCollectionContents(resource: Resource, resultClass: Class[_] = classOf[CollectionContent])
  extends SembaApiCall with RequestResult

case class AddToLibrary(sourceFile: SourceFile) extends SembaApiCall


case class RemoveCollectionItem(collectionItem: CollectionItem) extends SembaApiCall
with LibModification


case class RemoveFromLibrary(resource: Resource) extends SembaApiCall
with LibModification

case class AddToCollectionMsg(addToCollection: AddToCollection) extends SembaApiCall
  with LibModification

case class CreateRelation(relationModification: RelationModification) extends SembaApiCall
  with LibModification

case class RemoveRelation(relationModification: RelationModification) extends SembaApiCall
  with LibModification

case class UpdateMetadata(metadataUpdate: MetadataUpdate) extends SembaApiCall
  with LibModification

case class SimpleSearch(simpleQuery: SimpleQuery) extends SembaApiCall

case class SparqlFilter(sparqlQuery: SparqlQuery) extends SembaApiCall


/** Actor providing the interface from gRPC to SemBA. Inner classes set up the gRPC Server while AppConnector
  * reacts and distributes [[sembaGRPC.UpdateMessage]]s.
  *
  */
class AppConnector extends Actor {
  val app = Application
  /** [[scala.collection.mutable.HashMap]] mapping SessionIDs to registered gRPC StreamObservers*/
  val observers = new scala.collection.mutable.HashMap[String, StreamObserver[UpdateMessage]]()

  /** [[scala.collection.mutable.HashMap]] mapping Library URIs to subscribing SessionIDs   */
  val registeredForLibrary = new scala.collection.mutable.HashMap[String, ArrayBuffer[String]]()
  val logger = Logger.getLogger(classOf[ApiServer].getName)

  /** ApiServer instance using the Actors Executioncontext */
  val server = new ApiServer(ExecutionContext.global)
  server.start()
  //server.blockUntilShutdown()

  override def receive: Receive = {
    case update: UpdateMessage => {
      val allObservers = registeredForLibrary.get(update.lib)
      allObservers match {
        case None => println("RECEIVED " + update.kindOfUpdate)
        case Some(registered) => {
        for (observer <- registered) {
          try {
            observers.apply(observer).onNext(update)
          }
          catch {
            case e: Exception => {
              observers.remove(observer)
              app.closeConnection(UUID.fromString(observer))
            }
          }
        }
      }
    }
  }
  }

  /** Binds the SembaApiImpl to a [[io.grpc.Server]] instance
    *
    * @param executionContext
    */
  class ApiServer(executionContext: ExecutionContext) {
    self =>
    private[this] var server: Server = null

    def start(): Unit = {
      val builder = ServerBuilder.forPort(SembaPresets.grpcPort.toInt)
      builder.addService(SembaAPIGrpc.bindService(new SembaApiImpl, context.dispatcher))
      server = builder.build().start()
      logger.info("Semba Server started, listening on " + server.getPort)
      Runtime.getRuntime.addShutdownHook(new Thread() {
        override def run(): Unit = {
          System.err.println("*** Shutting down Server")
          self.stop()
          System.err.println("***Server shut down")
        }
      })

    }

    def stop(): Unit = {
      if (server != null) {
        server.shutdown()
      }
    }

    def blockUntilShutdown(): Unit = {
      if (server != null) {
        server.awaitTermination()
      }
    }

    /** Implementation of remote procedure calls defined in SembaAPI.proto
      *
      * [[SembaApiCall]]s are forwarded to the [[LibraryInstance]] actors referenced by the [[Library]] ID.
      * Getter functions are supposed to be synchronous, Setters return a [[VoidResult]] and completion is
      * propagated by [[UpdateMessage]].
      *
      */
     class SembaApiImpl extends SembaAPIGrpc.SembaAPI {

      /** Timeout for all Getters */
      implicit val timeout: Timeout = 12000 seconds

      /** Add the gRPC client to the map of observers
        *
        * @param request clients sessionID
        * @param responseObserver The update stream
        */
      override def subscribeUpdates(request: UpdateRequest, responseObserver: StreamObserver[UpdateMessage]): Unit = {
        observers.put(request.sessionID, responseObserver)
      }

      /** Creates a [[LibraryInstance]] Actor and loads the library if it is not already open. Registers the clients
        * sessionID for updates.
        *
        * @param request
        * @return List of available Relations and Annotations of the Library
        */
      override def openLibrary(request: LibraryRequest): Future[LibraryConcepts] = {

        val uri =  FileFactory.getURI(request.getLib.uri)
        val lib = app.loadLibrary(uri, UUID.fromString(request.sessionID))
        if (!registeredForLibrary.contains(uri)) registeredForLibrary.put(uri, new ArrayBuffer[String]())
        registeredForLibrary(uri).+=(request.sessionID)
        ask(lib, OpenLib()).mapTo[LibraryConcepts]
      }

      /** Asks corresponding [[LibraryInstance]] to import the [[SourceFile]].
        *
        * @param request Either a URI or Byte representation of the Import
        * @return Result is true if SourceFile can be read and Import has been started.
        */
      override def addToLibrary(request: SourceFile): Future[VoidResult] = {
        val lib = app.get(request.getLibrary.uri)
        ask(lib, AddToLibrary(request)).mapTo[VoidResult]
      }

      /**  Asks corresponding [[LibraryInstance]] for the contents of the Library.
        *
        * @param request Library reference
        * @return Map of all Resources (Items and Collections) present in the given Library
        */
      override def requestContents(request: Library): Future[LibraryContent] = {
        val lib = app.get(request.uri)
        ask(lib, RequestContents(request)).mapTo[LibraryContent]
      }

      override def requestCollectionContent(request: Resource): Future[CollectionContent] = {
        val lib = app.get(request.getLib.uri)
        ask(lib, RequestCollectionContents(request)).mapTo[CollectionContent]
      }

      /** Asks corresponding [[LibraryInstance]] for a Resources Metadata.
        *
        * @param request Item reference
        * @return All OWL DatatypeProperties that are a subtype of SembaMetadata
        */
      override def getMetadata(request: Resource): Future[ItemDescription] = {
        val lib = app.get(request.lib.get.uri)
        ask(lib, GetMetadata(request)).mapTo[ItemDescription]
      }

      /** Asks corresponding [[LibraryInstance]] to remove a Collectionitem from it's collection.
        * Removes the Individual and all of its statements from the OWL Model.
        *
        * @param request
        * @return Empty result.
        */
      override def removeFromCollection(request: CollectionItem): Future[VoidResult] = {
        val lib = app.get(request.lib.get.uri)
        ask(lib, RemoveCollectionItem(request)).mapTo[VoidResult]
      }

      /** Executes the [[SparqlQuery]] on the referenced OWL Model and returns all Items that match.
        *
        * @param request Request containing the query as a [[String]]
        * @return List of all matching Resources
        */
      override def sparqlFilter(request: SparqlQuery): Future[FilterResult] = {
        val lib = app.get(request.getLibrary.uri)
        ask(lib, SparqlFilter(request)).mapTo[FilterResult]
      }

      //TODO What about recursion?
      /** Asks corresponding [[LibraryInstance]] to add a Resource to a collection.
        * Creates a referencing CollectionItem inside the OWL model and adds it to the Collection ontology
        *
        * @param request The resource to be added.
        * @return False if resource can not be added du to recursive contents.
        */
      override def addToCollection(request: AddToCollection): Future[VoidResult] = {
        val lib = app.get(request.getCollection.getLib.uri)
        ask(lib, AddToCollectionMsg(request)).mapTo[VoidResult]
      }

      /** Unsubscribes client from updates regarding that library. Terminates the actor no other client is
        * watching it.
        *
        * @param request
        * @return True if library was closed, false if it is still loaded but unsubscribed.
        */
      override def closeLibrary(request: LibraryRequest): Future[VoidResult] = {
        val uri = FileFactory.getURI(request.getLib.uri)
        registeredForLibrary(uri).-=(request.sessionID)
        Future.successful(app.closeLibrary(uri, UUID.fromString(request.sessionID)))
      }

      /** Asks corresponding [[LibraryInstance]] to remove a relation between two [[CollectionItem]]s.
        *
        * @param request
        * @return True if all required fields of the [[RelationModification]] are available.
        */
      override def removeRelation(request: RelationModification): Future[VoidResult] = {
        val lib = app.get(request.getLibrary.uri)
        ask(lib, RemoveRelation(request)).mapTo[VoidResult]
      }

      /** Asks corresponding [[LibraryInstance]] to remove the Resource from its Library.
        * All definitions and the source file are removed from the file system.
        *
        * @param request
        * @return True if all required fields of the [[Resource]] are available.
        */
      override def removeFromLibrary(request: Resource): Future[VoidResult] = {
        val lib = app.get(request.getLib.uri)
        ask(lib, RemoveFromLibrary(request)).mapTo[VoidResult]
      }

      /** Asks corresponding [[LibraryInstance]] to add the given metadata keys/values to the OWL description of the Item.
        *
        * @param request
        * @return True if all required fields of the [[MetadataUpdate]] are available.
        */
      override def updateMetadata(request: MetadataUpdate): Future[VoidResult] = {
        val lib = app.get(request.getItem.getLib.uri)
        ask(lib, UpdateMetadata(request)).mapTo[VoidResult]
      }

      /** Simple but may take a while depending on Jena. Searches all Resources for SembaMetadata values matching the
        * provided String.
        *
        * @param request
        * @return List of all matching Resources
        */
      override def simpleSearch(request: SimpleQuery): Future[LibraryContent] = {
        val lib = app.get(request.getLibrary.uri)
        ask(lib, SimpleSearch(request)).mapTo[LibraryContent]
      }

      /** Asks corresponding [[LibraryInstance]] add the given relation as an ObjectProperty to the underlying OWL model.
        *
        * @param request
        * @return True if all required fields of the [[RelationModification]] are available.
        */
      override def createRelation(request: RelationModification): Future[VoidResult] = {
        val lib = app.get(request.getLibrary.uri)
        ask(lib, CreateRelation(request)).mapTo[VoidResult]
      }

      /** Provides an unique identifier to the client for identification and subscription. Token authentication may be
        * added here.
        *
        * @param request
        * @return [[UUID]] as an identifier during this session.
        */
      override def registerSession(request: SessionRequest): Future[SessionRequest] = {
        Future.successful(request.withSessionID(UUID.randomUUID().toString))
      }

      override def ping(request: TestMsg): Future[TestMsg] = {
        //println(request.test)
        val retVal = "Server running. " +
          app.libraries.size + " opened libraries are accessed by " +
          app.sessions.values.flatten.toList.distinct + " clients."
        Future.successful(TestMsg(request.test))
      }

      override def closeConnection(request: SessionRequest): Future[SessionRequest] = {
        app.closeConnection(UUID.fromString(request.sessionID))
        observers.remove(request.sessionID)
        Future.successful(request)
      }

      override def writeBenchmarkResults(request: BenchmarkWriteRequest): Future[VoidResult] = {
        val lib = app.get(request.lib)
        ask(lib, WriteResults(request.name)).mapTo[VoidResult]
      }


    }

  }


}


