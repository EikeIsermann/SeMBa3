package core.library

import java.net.URI
import java.util.UUID

import akka.actor.{Actor, Props}
import akka.actor.Actor.Receive
import akka.routing.RoundRobinPool
import api.AddToLibrary
import core.{JobHandling, JobProtocol, JobReply, LibInfo}
import sembaGRPC.SourceFile.Source
import utilities.FileFactory

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

class ResourceCreation(libInfo: LibInfo) extends Actor with JobHandling {
  val singleItemImport = context.actorOf(new RoundRobinPool(10).props(Props[SingleItemImport]))
  val singleCollectionImport = context.actorOf(new RoundRobinPool(10).props(Props[CollectionHandler]))

  override def receive: Receive = {
    case job: JobProtocol => {
      acceptJob(job, sender())
      self ! handleJob(job)

    }
    case reply: JobReply => handleReply(reply, self)

  }

  override def handleJob(job: JobProtocol): JobReply = {
      job match {
        case addToLib: AddToLibrary => addItems(addToLib)
      }
    JobReply(job)
  }

  def addItems(add: AddToLibrary) = {
    add.sourceFile.source match {
      case Source.Path(path) => {
        val items = FileFactory.contentsOfDirectory(new URI(path), true, false, false)
        for (item <- items) {
          val job = ImportNewItem(item, libInfo, true)
          singleItemImport ! createJob(job, add)
        }
      }

      case Source.Data(data) => {
        //TODO get input stream, copy to local filesystem, read as new item
      }

      case Source.Coll(newColl) => {
         val job = CreateCollection(newColl.name, newColl.ontClass, libInfo, new URI(libInfo.config.defaultCollectionIcon))
        singleCollectionImport ! createJob(job, add)
      }
      case Source.Empty =>
    }
  }
}
