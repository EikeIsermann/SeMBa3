package logic.resourceCreation

import java.net.URI

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.RoundRobinPool
import api.AddToLibrary
import globalConstants.GlobalMessages.UpdateResult
import logic.core._
import sembaGRPC.SourceFile.Source
import utilities.FileFactory

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

class ResourceCreator(libInfo: LibInfo) extends Actor with ActorFeatures with JobHandling {

  val singleItemImport = context.actorOf(new RoundRobinPool(10).props(Props[SingleItemImport]))
  val singleCollectionImport = context.actorOf(new RoundRobinPool(10).props(Props[SingleCollectionImport]))


  override def preStart(): Unit = {
    context.parent ! InitializationComplete(self)
    super.preStart()
  }
  def wrappedReceive: Receive = {
    case job: AddToLibrary => {
      acceptJob(job, context.sender())
      addItems(job)

    }
    case reply: JobReply => handleReply(reply)

  }

  def addItems(add: AddToLibrary) = {
    add.sourceFile.source match {
      case Source.Path(path) => {
        val items = FileFactory.contentsOfDirectory(new URI(path), true, false, false)
        for (item <- items) {
          val job = ImportNewItem(item, add.sourceFile.ontClass, libInfo, true)
          singleItemImport ! createJob(job, add)
        }
      }

      case Source.Data(data) => {
        //TODO get input stream, copy to local filesystem, read as new item
      }

      case Source.Coll(newColl) => {
         val job = CreateCollection(newColl, add.sourceFile.ontClass, libInfo)
        singleCollectionImport ! createJob(job, add)
      }
      case Source.Empty =>
    }
  }

  override def finishedJob(job: JobProtocol, master: ActorRef, results: ResultArray): Unit = {
    job match {
      case add: AddToLibrary => {
        master ! JobReply(add, results)
      }
    }


  }
}
object ResourceCreator{
  def props(libInfo: LibInfo): Props = Props(new ResourceCreator(libInfo))
}