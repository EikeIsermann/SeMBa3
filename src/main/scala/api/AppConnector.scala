package api

import java.net.URI
import java.util.UUID
import java.util.logging.Logger

import akka.actor.Actor
import akka.actor.Actor.Receive
import app.{Application, Paths, Presets}
import core.ontology.PropertyType
import io.grpc.stub.StreamObserver
import sembaGRPC.{CollectionItem, Library, LibraryConcepts, SimpleQuery, _}
import akka.pattern._
import akka.util.Timeout
import io.grpc.{Server, ServerBuilder}
import sembaGRPC.SembaAPIGrpc.SembaAPI

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, ExecutionException, Future}
import scala.concurrent.duration._

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait SembaApiCall
case class OpenLib() extends SembaApiCall
case class RemoveCollectionItem(collectionItem: CollectionItem) extends SembaApiCall
case class GetMetadata(resource: Resource) extends SembaApiCall
case class RequestContents(library: Library) extends SembaApiCall
case class AddToLibrary(sourceFile: SourceFile) extends SembaApiCall
case class RemoveFromLibrary(resource: Resource) extends SembaApiCall
case class AddToCollectionMsg(addToCollection: AddToCollection) extends SembaApiCall
case class CreateRelation(relationModification: RelationModification) extends SembaApiCall
case class RemoveRelation(relationModification: RelationModification) extends SembaApiCall
case class UpdateMetadata(metadataUpdate: MetadataUpdate) extends SembaApiCall
case class SimpleSearch(simpleQuery: SimpleQuery) extends SembaApiCall
case class SparqlFilter(sparqlQuery: SparqlQuery) extends SembaApiCall

class AppConnector extends Actor {
  val app = Application
  val observers = new scala.collection.mutable.HashMap[String, StreamObserver[UpdateMessage]]()
  val registeredForLibrary= new scala.collection.mutable.HashMap[String, ArrayBuffer[String]]()
  val logger = Logger.getLogger(classOf[ApiServer].getName)

  val server = new ApiServer(context.dispatcher)
  server.start()
  server.blockUntilShutdown()
  println("Ready")

  override def receive: Receive = {
    case update: UpdateMessage => {
      val registered = registeredForLibrary.apply(update.lib)
      for (observer <- registered)
        observers.apply(observer).onNext(update)
    }
  }




   class ApiServer(executionContext: ExecutionContext)  { self =>
    private[this] var server: Server = null

     def start(): Unit = {
      val builder = ServerBuilder.forPort(Presets.grpcPort.toInt)
      builder.addService(SembaAPIGrpc.bindService(new SembaApiImpl, context.dispatcher))
      server = builder.build().start()
      logger.info("Semba Server started, listening on " + server.getPort)
      Runtime.getRuntime.addShutdownHook(new Thread(){
        override def run(): Unit = {
          System.err.println("*** Shutting down Server")
          self.stop()
          System.err.println("***Server shut down")
        }
      })

    }
     def stop(): Unit ={
      if (server != null){
        server.shutdown()
      }
    }

     def blockUntilShutdown(): Unit = {
      if (server != null){
        server.awaitTermination()
      }
    }

    private class SembaApiImpl extends SembaAPIGrpc.SembaAPI {

      implicit val timeout: Timeout = 20 seconds

      override def openLibrary(request: LibraryRequest): Future[LibraryConcepts] = {
        val uri = request.getLib.uri
        val lib = app.loadLibrary(new URI(uri), UUID.fromString(request.sessionID))
        if(!registeredForLibrary.contains(uri)) registeredForLibrary.put(uri, new ArrayBuffer[String]())
        registeredForLibrary(request.getLib.uri).+=(request.sessionID)
        ask(lib, OpenLib()).mapTo[LibraryConcepts]
      }

      override def removeFromCollection(request: CollectionItem): Future[VoidResult] = {
        val lib = app.get(request.lib.get.uri)
        ask(lib, RemoveCollectionItem(request)).mapTo[VoidResult]
      }

      override def subscribeUpdates(request: UpdateRequest, responseObserver: StreamObserver[UpdateMessage]): Unit = {
        observers.put(request.sessionID, responseObserver)
      }

      override def getMetadata(request: Resource): Future[ItemDescription] = {
        val lib = app.get(request.lib.get.uri)
        ask(lib, GetMetadata(request)).mapTo[ItemDescription]
      }

      override def requestContents(request: Library): Future[LibraryContent] = {
        val lib = app.get(request.uri)
        ask(lib, RequestContents(request)).mapTo[LibraryContent]
      }

      override def addToLibrary(request: SourceFile): Future[VoidResult] = {
        val lib = app.get(request.getLibrary.uri)
        ask(lib, AddToLibrary(request)).mapTo[VoidResult]
      }

      override def sparqlFilter(request: SparqlQuery): Future[LibraryContent] = {
        val lib = app.get(request.getLibrary.uri)
        ask(lib, SparqlFilter(request)).mapTo[LibraryContent]
      }

      override def addToCollection(request: AddToCollection): Future[VoidResult] = {
        val lib = app.get(request.getCollection.getLib.uri)
        ask(lib, AddToCollectionMsg(request)).mapTo[VoidResult]
      }

      override def closeLibrary(request: LibraryRequest): Future[VoidResult] = {
        registeredForLibrary(request.getLib.uri).-=(request.sessionID)
        Future.successful(app.closeLibrary(new URI(request.getLib.uri), UUID.fromString(request.sessionID)))
      }

      override def removeRelation(request: RelationModification): Future[VoidResult] = {
        val lib = app.get(request.getLibrary.uri)
        ask(lib, RemoveRelation(request)).mapTo[VoidResult]
      }

      override def removeFromLibrary(request: Resource): Future[VoidResult] = {
        val lib = app.get(request.getLib.uri)
        ask(lib,RemoveFromLibrary(request)).mapTo[VoidResult]
      }

      override def updateMetadata(request: MetadataUpdate): Future[VoidResult] = {
        val lib = app.get(request.getItem.getLib.uri)
        ask(lib, UpdateMetadata(request)).mapTo[VoidResult]
      }

      override def simpleSearch(request: SimpleQuery): Future[LibraryContent] = {
        val lib = app.get(request.getLibrary.uri)
        ask(lib, SimpleSearch(request)).mapTo[LibraryContent]
      }

      override def createRelation(request: RelationModification): Future[VoidResult] = {
        val lib = app.get(request.getLibrary.uri)
        ask(lib, createRelation(request)).mapTo[VoidResult]
      }

      override def registerSession(request: SessionRequest): Future[SessionRequest] = {
        Future.successful(request.withSessionID(UUID.randomUUID().toString))
      }

      override def ping(request: TestMsg): Future[TestMsg] = {
        println(request.test)
        Future.successful(TestMsg("Pong"))
      }
    }
  }


}


