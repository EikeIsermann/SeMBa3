package logic.resourceCreation

import java.net.URI

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.RoundRobinPool
import api.AddToLibrary
import globalConstants.GlobalMessages.StorageWriteResult
import logic.core._
import logic.resourceCreation.CreationStorageMethods.CreateInStorage
import logic.resourceCreation.metadata.MetadataMessages.ThumbnailResult
import sembaGRPC.SourceFile.Source
import utilities.FileFactory

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

class ResourceCreator(libInfo: LibInfo) extends Actor with ActorFeatures with JobHandling {

  val singleItemImport = context.actorOf(new RoundRobinPool(50).props(Props[SingleItemImport]))
  val singleCollectionImport = context.actorOf(new RoundRobinPool(10).props(Props[SingleCollectionImport]))
  val fileMover = context.actorOf(new RoundRobinPool(5).props(Props[FileMover]))
  var counter = 0
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

  override def reactOnReply(reply: JobReply, originalJob: JobProtocol, results: ResultArray): Unit = {
    reply.job match {
      case item: ImportNewItem => {
        /*  counter += 1
          val newItem = results.get(classOf[StorageWriteResult]).payload.items.head
          val moveSourceFile = MoveFile(item.item.toURI.toString, newItem.sourceFile)
          val tempThumbnail = results.get(classOf[ThumbnailResult]).payload.toString
        println("received successful import nr " + counter + newItem.uri)

        fileMover ! createJob(moveSourceFile, originalJob)
        if(newItem.thumbnailLocation != item.libInfo.constants.defaultCollectionIcon)
         fileMover ! createJob(MoveFile(tempThumbnail, newItem.thumbnailLocation, true), originalJob)
         */
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
