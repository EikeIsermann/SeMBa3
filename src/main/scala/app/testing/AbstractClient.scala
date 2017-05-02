package app.testing

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import globalConstants.SembaPaths
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, StatusRuntimeException}
import sembaGRPC.SembaAPIGrpc.{SembaAPIBlockingStub, SembaAPIStub}
import sembaGRPC._

import scala.collection.immutable.HashMap

/**
  * Created by Eike on 01.05.2017.
  */
abstract class AbstractClient (
                                       val channel: ManagedChannel,
                                       val blockingStub: SembaAPIBlockingStub,
                                       val asyncStub: SembaAPIStub
                                     ) {
  val logger = Logger.getLogger(classOf[AbstractClient].getName)


  val updateFunction: PartialFunction[UpdateMessage, Any]
  var session: String = ""

  def shutdown(session: SessionRequest): Unit = {
    blockingStub.closeConnection(session)
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
  }

  def registerSession(): String = {
    logger.info("Trying to register a session")
    var session = SessionRequest()
    try {
      session = blockingStub.registerSession(session)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)

    }
    session.sessionID

  }

  def openLib(lib: Library): LibraryConcepts = {
    logger.info("Trying to open library")
    val request = LibraryRequest(Some(lib), session)
    var concepts = LibraryConcepts()
    try {
      concepts = blockingStub.openLibrary(request)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    concepts
  }

  def closeLib(lib: Library, instance: ClientLib) = {
    logger.info("Closing Library")
        try {
          blockingStub.closeLibrary(LibraryRequest(Some(lib), session))
        }
        catch {
          case e: StatusRuntimeException =>
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
        }
  }

  def addItem(src: String, lib: Library): VoidResult = {
    logger.info("Trying to add item")
    var retVal = VoidResult()
    val source = SourceFile().withLibrary(lib).withPath(src).withOntClass(SembaPaths.itemClassURI)
    try {
      retVal = blockingStub.addToLibrary(source)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    retVal

  }
  def addColl(collName: String, clazz: String, lib: Library): VoidResult = {
    logger.info("Trying to add collection")
    var retVal = VoidResult()
    val source = SourceFile().withLibrary(lib).withOntClass(clazz).withColl(NewCollection(name = collName))
    try {
      retVal = blockingStub.addToLibrary(source)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    retVal

  }

  def getContent(lib: Library): LibraryContent = {
    logger.info("Getting Lib Contents")
    var retVal = LibraryContent()
    try {
      retVal = blockingStub.requestContents(lib)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    retVal

  }
  def getMetadata(src: String, library: Library): ItemDescription = {
    logger.info("Retrieving Metadata")
    var retVal = ItemDescription()
    try {
      retVal = blockingStub.getMetadata(Resource().withUri(src).withLib(library))
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    retVal
  }

  def removeItem(src: String, library: Library): VoidResult = {
    logger.info("Removing Item")
    var retVal = VoidResult()
    try {
      retVal = blockingStub.removeFromLibrary(Resource().withUri(src).withLib(library))
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    retVal
  }

  def subscribeForUpdates(session: String, next: PartialFunction[UpdateMessage, Any] ) = {
    logger.info("Subscribing for Updates")
    try {
      val observer = new StreamObserver[UpdateMessage] {
        override def onError(t: Throwable): Unit = {
          println(t.getMessage)
        }

        override def onCompleted(): Unit = {
          println("Completed")
        }

        override def onNext(value: UpdateMessage): Unit = next(value)
      }
      asyncStub.subscribeUpdates(UpdateRequest(session), observer )
    }
  }

  def sparql(query: String, vars: Seq[String], lib: Library): FilterResult = {
    logger.info("Performing Sparqlmagic")
    var retVal = FilterResult()
    try {
      retVal = blockingStub.sparqlFilter(SparqlQuery().withLibrary(lib).withVars(vars).withQueryString(query))
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    retVal
  }

  def requestCollectionContent(item: Resource): CollectionContent = {
    logger.info("Requesting Collection Contents")
    var retVal = CollectionContent()
    try{
      retVal = blockingStub.requestCollectionContent(item)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    retVal
  }
  def updateMetadata(upd: MetadataUpdate): VoidResult = {
    logger.info("Performing Metadata Update")
    var retVal = VoidResult()
    try{
      retVal = blockingStub.updateMetadata(upd)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    retVal
  }

  def addToCollection(addToColl: AddToCollection): VoidResult = {
    logger.info("Adding Item to Collection")
    var retVal = VoidResult()
    try{
     retVal = blockingStub.addToCollection(addToColl)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    retVal
  }
  def removeFromCollection(removeFromColl: CollectionItem): VoidResult = {
    logger.info("Removing Item From Collection")
    var retVal = VoidResult()
    try{
      retVal = blockingStub.removeFromCollection(removeFromColl)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    retVal
  }

  def createRelation(mod: RelationModification): VoidResult = {
    logger.info("Creating Object to Object Relation")
    var retVal = VoidResult()
    try{
      retVal = blockingStub.createRelation(mod)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    retVal
  }

  def removeRelation(mod: RelationModification): VoidResult = {
    logger.info("Removing Object to Object Relation")
    var retVal = VoidResult()
    try{
     retVal = blockingStub.removeRelation(mod)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    retVal
  }

  def writeResults(lib: Library): VoidResult = {
    logger.info("Sending Benchmark Write Trigger")
    var retVal = VoidResult()
    try{
     retVal = blockingStub.writeBenchmarkResults(lib)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    retVal
  }

  def ping(str: String): String = {
    logger.info("Pinging remote Server")
    var response = TestMsg("Empty")
    try{
      response  = blockingStub.ping(TestMsg(str))
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    response.test
  }

}

