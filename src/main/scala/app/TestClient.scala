package app

import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import org.apache.jena.util.FileUtils
import sembaGRPC.SembaAPIGrpc.{SembaAPIBlockingStub, SembaAPIStub}
import sembaGRPC._

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object TestClient extends App {

  val client = TestClient("localhost", 50051)
  var testSessionID: String = ""





  var test: LibraryConcepts = LibraryConcepts()
    testSessionID = client.registerSession().sessionID
    var testLib = Library(uri = "file:/users/uni/documents/semba3/appdata/libraries/library.ttl")
    client.openLib(LibraryRequest().withLib(testLib).withSessionID(testSessionID))
  //  println(client.getContent(testLib))
    client.subscribeForUpdates(testSessionID)

   for(i <- 1 to 1){
      println(client.addItem("file:/users/uni/documents/semba3/appdata/libraries/test/Test.pdf", testLib))
   }
    //println(client.addColl("Test", "http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-teaching.owl#Course", testLib ))
      //println(client.getMetadata("file:///Users/uni/Documents/SeMBa3/appdata/libraries/data/Test/definition.ttl#content", testLib))
   //   println(client.getMetadata("file:///Users/uni/Documents/SeMBa3/appdata/libraries/data/Test_2/definition.ttl#content", testLib))
    //client.removeItem("file:///Users/uni/Documents/SeMBa3/appdata/libraries/data/Test/definition.ttl#content", testLib)
      Thread.sleep(100000)





  def apply(host: String, port: Int): TestClient = {
    /* val test = ManagedChannelBuilder.forAddress(host, port)
     test.usePlaintext(true)
     val  channel =  test.build()
      */
    val channel = ManagedChannelBuilder.forAddress(host, port)
    channel.usePlaintext(true)
    val channel2 = channel.build()
    val blockingStub = SembaAPIGrpc.blockingStub(channel2)
    val asyncStub = SembaAPIGrpc.stub(channel2)
    new TestClient(channel2, blockingStub, asyncStub)

  }

}


class TestClient private(
                          private val channel: ManagedChannel,
                          private val blockingStub: SembaAPIBlockingStub,
                          private val asyncStub: SembaAPIStub
                        ) {
  private[this] val logger = Logger.getLogger(classOf[TestClient].getName)

  def shutdown(session: SessionRequest): Unit = {
    blockingStub.closeConnection(session)
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
  }


  def registerSession(): SessionRequest = {
    logger.info("Trying to register a session")
    var session = SessionRequest()
    try {
      session = blockingStub.registerSession(session)

    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)

    }
    session

  }

  def openLib(libraryRequest: LibraryRequest): LibraryConcepts = {
    logger.info("Trying to open library")
    var concepts = LibraryConcepts()
    try {
      concepts = blockingStub.openLibrary(libraryRequest)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
    concepts
  }

  def addItem(src: String, lib: Library): VoidResult = {
    logger.info("Trying to add item")
    var retVal = VoidResult()
    val source = SourceFile().withLibrary(lib).withPath(src)
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
    val source = SourceFile().withLibrary(lib).withColl(NewCollection(ontClass = clazz, name = collName))
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

  def subscribeForUpdates(session: String) = {
    logger.info("Subscribing for Updates")
    try {
       val observer = new StreamObserver[UpdateMessage] {
         override def onError(t: Throwable): Unit = {
           println(t.getMessage)
         }

         override def onCompleted(): Unit = {
           println("Completed")
         }

         override def onNext(value: UpdateMessage): Unit = println(value.messageContent)
       }
       asyncStub.subscribeUpdates(UpdateRequest(session), observer )
    }
  }

}
