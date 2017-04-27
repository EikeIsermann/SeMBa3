package logic.resourceCreation

import java.net.URI

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.RoundRobinPool
import api.AddToLibrary
import globalConstants.GlobalMessages.StorageWriteResult
import logic.core._
import logic.core.jobHandling.{Job, JobHandling, JobReply, ResultArray}
import logic.resourceCreation.CreationStorageMethods.CreateInStorage
import logic.resourceCreation.metadata.MetadataMessages.ThumbnailResult
import sembaGRPC.SourceFile.Source
import utilities.FileFactory

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

class ResourceCreator(val config: Config) extends Actor with ActorFeatures with JobHandling {

  val singleItemImport = context.actorOf(new RoundRobinPool(50).props(SingleItemImport.props(config)))
  val singleCollectionImport = context.actorOf(new RoundRobinPool(10).props(SingleCollectionImport.props(config)))
  val fileMover = context.actorOf(new RoundRobinPool(5).props(FileMover.props(config)))
  var counter = 0
  override def preStart(): Unit = {
    context.parent ! InitializationComplete(self)
    super.preStart()
  }

  override def handleJob(job: Job, master: ActorRef): Unit = {
    job match {
      case add: AddToLibrary => {
        acceptJob(add, context.sender())
        addItems(add)

      }
    }

  }


  override def reactOnReply(reply: JobReply, originalJob: Job, results: ResultArray): Unit = {
    reply.job match {
      case item: ImportNewItem => {
          counter += 1
          val newItem = results.get(classOf[StorageWriteResult]).payload.items.head
          val moveSourceFile = MoveFile(item.item.toURI.toString, newItem.sourceFile)
        println("received successful import nr " + counter + newItem.uri)
        fileMover ! moveSourceFile

      }
      case _ =>

        //TODO CollectionImage
      /*case collection: CreateCollection  => {
        val newItem = results.get(classOf[StorageWriteResult]).payload.items.head
        val tempThumbnail = results.get(classOf[ThumbnailResult]).payload.toString
        fileMover ! createJob(moveSourceFile, item)
        if(newItem.thumbnailLocation != item.libInfo.constants.defaultCollectionIcon)
          fileMover ! createJob(MoveFile(tempThumbnail, newItem.thumbnailLocation, true), item)
      }           */


    }

  }

  def addItems(add: AddToLibrary) = {
    add.sourceFile.source match {
      case Source.Path(path) => {
        val items = FileFactory.contentsOfDirectory(new URI(path), true, false, false)
        for (item <- items) {
          val job = ImportNewItem(item, add.sourceFile.ontClass, config, true)
          singleItemImport ! createJob(job, add)
        }
      }

      case Source.Data(data) => {
        //TODO get input stream, copy to local filesystem, read as new item
      }

      case Source.Coll(newColl) => {
         val job = CreateCollection(newColl, add.sourceFile.ontClass, config)
        singleCollectionImport ! createJob(job, add)
      }
      case Source.Empty =>
    }
  }

  override def finishedJob(job: Job, master: ActorRef, results: ResultArray): Unit = {
    job match {
      case add: AddToLibrary => {
        master ! JobReply(add, results)
      }
    }


  }
}
object ResourceCreator{
  def props(config: Config): Props = Props(new ResourceCreator(config))
}
