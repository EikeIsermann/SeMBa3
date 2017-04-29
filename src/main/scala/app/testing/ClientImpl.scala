package app.testing

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import globalConstants.SembaPaths
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import sembaGRPC.SembaAPIGrpc.{SembaAPIBlockingStub, SembaAPIStub}
import sembaGRPC._
import utilities.debug.DC

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.collection.parallel.immutable

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object ClientImpl {

  def apply(): ClientImpl = apply("localhost", 50051)
  def apply(host: String, port: Int): ClientImpl = {
    /* val test = ManagedChannelBuilder.forAddress(host, port)Ja
     test.usePlaintext(true)
     val  channel =  test.build()
      */



    val channel = ManagedChannelBuilder.forAddress(host, port).asInstanceOf[ManagedChannelBuilder[_]]
    channel.maxInboundMessageSize(1012*1014*1024)
    channel.usePlaintext(true)
    val channel2 = channel.build()
    val blockingStub = SembaAPIGrpc.blockingStub(channel2)
    val asyncStub = SembaAPIGrpc.stub(channel2)
    new ClientImpl(channel2, blockingStub, asyncStub)

  }

}


class ClientImpl private(
                          private val channel: ManagedChannel,
                          private val blockingStub: SembaAPIBlockingStub,
                          private val asyncStub: SembaAPIStub
                        ) {
  private[this] val logger = Logger.getLogger(classOf[ClientImpl].getName)

  val session: String = registerSession
  var connectedLibs = HashMap.empty[String, List[ClientLib]]


  val updateFunction = (upd: UpdateMessage) => {
    val subs = getSubscribers(upd.lib)
    upd.kindOfUpdate match {
      case UpdateType.NOTIFY => {
        upd.notes.foreach(note => DC.log(note.msg))
      }
      case UpdateType.DELETE => {
        updateValueOperation(subs, upd.items, (s: ClientLib, v: Resource) => s.removedFromLibrary(v))
        updateValueOperation( subs,upd.collectionItems, (s: ClientLib, v: CollectionItem) => s.removedFromCollection(v))
        }
      case UpdateType.REPLACE => {
        updateValueOperation(subs, upd.descriptions, (s: ClientLib, v: ItemDescription) => s.updatedDescription(v))
        updateValueOperation(subs, upd.items, (s: ClientLib, v: Resource) => s.updatedItem(v))
        updateValueOperation(subs, upd.collectionItems, (s: ClientLib, v: CollectionItem) => s.updatedCollectionItem(v))
      }

      case UpdateType.ADD => {
        updateValueOperation(subs, upd.items, (s: ClientLib, v: Resource) => s.addedItem(v))
        updateValueOperation(subs, upd.annotations, (s: ClientLib, v: AnnotationUpdate) => s.addedAnnotations(v))
        updateValueOperation( subs,upd.collectionItems, (s: ClientLib, v: CollectionItem) => s.addedCollectionItem(v))
        updateValueOperation(subs, upd.descriptions,  (s: ClientLib, v: ItemDescription) => s.addedDescription(v))
    }
      case _ => DC.log("Unrecognized Update")
    }
  }

    subscribeForUpdates(session, updateFunction)

  def updateValueOperation[ T, A <: ClientLib](subscribers: List[A], values: Seq[T], f: ((A,T) => Unit)) = {
    values.foreach( value => {
      subscribers.foreach(subscriber => f(subscriber, value))
    })
  }

  def getSubscribers(lib: String): List[ClientLib] = {
    connectedLibs.getOrElse(lib, List.empty[ClientLib])
  }

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

  def openLib(lib: Library, instance: ClientLib): LibraryConcepts = {
    logger.info("Trying to open library")
    connectedLibs = connectedLibs.updated(lib.uri, connectedLibs.getOrElse(
      lib.uri, List(instance)).::(instance)
    )
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
    var opened = connectedLibs.apply(lib.uri)
    opened = opened.filterNot(_.equals(instance))
    opened.isEmpty match {
      case true => {
       connectedLibs = connectedLibs.-(lib.uri)
        try {
          blockingStub.closeLibrary(LibraryRequest(Some(lib), session))
        }
        catch {
          case e: StatusRuntimeException =>
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
        }
      }
      case false => connectedLibs = connectedLibs.updated(lib.uri, opened)
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

  def subscribeForUpdates(session: String, next: UpdateMessage => Any ) = {
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

  def sparql(query: String, vars: Seq[String], lib: Library) = {
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
  def updateMetadata(upd: MetadataUpdate) = {
    logger.info("Performing Metadata Update")
    try{
      blockingStub.updateMetadata(upd)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }

  def addToCollection(addToColl: AddToCollection) = {
    logger.info("Adding Item to Collection")
    try{
      blockingStub.addToCollection(addToColl)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }
  def removeFromCollection(removeFromColl: CollectionItem) = {
    logger.info("Removing Item From Collection")
    try{
      blockingStub.removeFromCollection(removeFromColl)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }

  def createRelation(mod: RelationModification) = {
    logger.info("Creating Object to Object Relation")
    try{
      blockingStub.createRelation(mod)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }

  def removeRelation(mod: RelationModification) = {
    logger.info("Removing Object to Object Relation")
    try{
      blockingStub.removeRelation(mod)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }

  def writeResults(lib: Library) = {
    logger.info("Sending Benchmark Write Trigger")
    try{
      blockingStub.writeBenchmarkResults(lib)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
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
