package logic.itemModification

import akka.actor.{Actor, ActorRef, Props}
import api._
import logic.core.{Config, LibActor}
import logic.core.jobHandling.{Job, JobHandling}
import logic.itemModification.ModificationMethods._

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class ModificationHandler(val config: Config) extends LibActor {
   val storagePipeline = config.libAccess






  override def handleJob(job: Job, master: ActorRef): Unit = {
    job match {
      case removeIt: RemoveFromLibrary => {
        acceptJob(removeIt, context.sender())
        val newJob = RemoveItemFromStorage(removeIt.resource.uri, config)
        storagePipeline ! createJob(newJob, removeIt)
      }
      case updateMeta: UpdateMetadata => {
        acceptJob(updateMeta, context.sender())
        val resource = updateMeta.metadataUpdate.item.get
        val addedProps = updateMeta.metadataUpdate.add
        val deletedProps = updateMeta.metadataUpdate.delete
        val name: Option[String] = if (addedProps.isDefined && addedProps.get.name != "") Some(addedProps.get.name) else None
        val newJob = UpdateMetaInStorage(resource.uri, name, addedProps, deletedProps, config)
        storagePipeline ! createJob(newJob, updateMeta)
      }

      case removeCollItem: RemoveCollectionItem => {
        acceptJob(removeCollItem, context.sender())
        val newJob = RemoveCollectionItemFromStorage(removeCollItem.collectionItem.uri, removeCollItem.collectionItem.parentCollection, config)
        storagePipeline ! createJob(newJob, removeCollItem)
      }

      case addCollItem: AddToCollectionMsg => {
        acceptJob(addCollItem, context.sender())
        val collection = addCollItem.addToCollection.collection.get
        val item = addCollItem.addToCollection.newItem.get
        val newJob = AddToCollectionInStorage(collection.uri, item.uri, config)
        storagePipeline ! createJob(newJob, addCollItem)
      }

      case createRel: CreateRelation => {
        acceptJob(createRel, context.sender())
        val mod = createRel.relationModification
        val start = mod.start.get.uri
        val end = mod.end.get.uri
        val rel = mod.rel.get.uri
        val newJob = ModifyCollectionRelationInStorage(start, end, rel, config, false)
        storagePipeline ! createJob(newJob, createRel)
      }
      case removeRel: RemoveRelation => {
        acceptJob(removeRel, context.sender())
        val mod = removeRel.relationModification
        val start = mod.start.get.uri
        val end = mod.end.get.uri
        val rel = mod.rel.get.uri
        val newJob = ModifyCollectionRelationInStorage(start, end, rel, config, true)
        storagePipeline ! createJob(newJob, removeRel)
      }
    }
  }
}

object ModificationHandler{
  def props(config: Config) = Props(new ModificationHandler(config))
}
