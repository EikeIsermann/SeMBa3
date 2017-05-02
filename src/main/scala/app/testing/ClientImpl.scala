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
import scala.collection.mutable.ListBuffer
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


class ClientImpl(
                  channel: ManagedChannel,
                  blockingStub: SembaAPIBlockingStub,
                  asyncStub: SembaAPIStub
                ) extends AbstractClient(channel, blockingStub, asyncStub) {
  override val logger = Logger.getLogger(classOf[ClientImpl].getName)


  session = registerSession
  val lastUpdates = new FixedList[UpdateMessage](10)

  var connectedLibs = HashMap.empty[String, List[ClientLib]]

  val updateFunction: PartialFunction[UpdateMessage, Any] = {
    case update: UpdateMessage => {
      val subs = getSubscribers(update.lib)
      update.kindOfUpdate match {
        case UpdateType.NOTIFY => {
          update.notes.foreach(note => DC.log(note.msg))
        }
        case UpdateType.DELETE => {
          updateValueOperation(subs, update.items, (s: ClientLib, v: Resource) => s.removedFromLibrary(v))
          updateValueOperation(subs, update.collectionItems, (s: ClientLib, v: CollectionItem) => s.removedFromCollection(v))
        }
        case UpdateType.REPLACE => {
          updateValueOperation(subs, update.descriptions, (s: ClientLib, v: ItemDescription) => s.updatedDescription(v))
          updateValueOperation(subs, update.items, (s: ClientLib, v: Resource) => s.updatedItem(v))
          updateValueOperation(subs, update.collectionContent, (s: ClientLib, v:CollectionContent) ⇒ s.updatedCollectionContent(v) )
          updateValueOperation(subs, update.collectionItems, (s: ClientLib, v: CollectionItem) => s.updatedCollectionItem(v))
        }

        case UpdateType.ADD => {
          updateValueOperation(subs, update.items, (s: ClientLib, v: Resource) => s.addedItem(v))
          updateValueOperation(subs, update.annotations, (s: ClientLib, v: AnnotationUpdate) => s.addedAnnotations(v))
          updateValueOperation(subs, update.collectionItems, (s: ClientLib, v: CollectionItem) => s.addedCollectionItem(v))
          updateValueOperation(subs, update.descriptions, (s: ClientLib, v: ItemDescription) => s.addedDescription(v))
        }
        case _ => DC.log("Unrecognized Update")
      }
      lastUpdates.append(update)
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

  override def closeLib(lib: Library, instance: ClientLib) = {
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

}


class FixedList[A](max: Int) extends Traversable[A] {

  val list: ListBuffer[A] = ListBuffer()

  def append(elem: A) {
    if (list.size == max) {
      list.trimStart(1)
    }
    list.append(elem)
  }

  def foreach[U](f: A => U) = list.foreach(f)

}
