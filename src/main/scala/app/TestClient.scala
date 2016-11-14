package app

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import sembaGRPC.SembaAPIGrpc.SembaAPIBlockingStub
import sembaGRPC._

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object TestClient extends App {

  val client = TestClient("localhost", 50051)
  var testSessionID: String = ""
  var test: LibraryConcepts = LibraryConcepts()
  try {
    testSessionID = client.registerSession().sessionID
    var testLib = Library(uri = "file:///users/uni/documents/semba3/appdata/libraries/semba-teaching.owl")
    client.openLib(LibraryRequest().withLib(testLib).withSessionID(testSessionID))
    //println(client.getContent(testLib))
    println(client.addItem("file:///users/uni/documents/semba3/appdata/libraries/test", testLib))

  }
  finally {
    println(test)
  }


  def apply(host: String, port: Int): TestClient = {
    /* val test = ManagedChannelBuilder.forAddress(host, port)
     test.usePlaintext(true)
     val  channel =  test.build()
      */
    val channel = ManagedChannelBuilder.forAddress(host, port)
    channel.usePlaintext(true)
    val channel2 = channel.build()
    val blockingStub = SembaAPIGrpc.blockingStub(channel2)
    new TestClient(channel2, blockingStub)
  }

}


class TestClient private(
                          private val channel: ManagedChannel,
                          private val blockingStub: SembaAPIBlockingStub
                        ) {
  private[this] val logger = Logger.getLogger(classOf[TestClient].getName)

  def shutdown(): Unit = {
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

  def getContent(lib: Library): LibraryContent = {
    logger.info("Trying to add item")
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

}
