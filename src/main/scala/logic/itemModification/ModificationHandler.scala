package logic.itemModification

import akka.actor.{Actor, Props}
import api._
import logic.core.{JobHandling, LibInfo}
import logic.itemModification.ModificationMethods._

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
class ModificationHandler(config: LibInfo) extends Actor with JobHandling {
   val storagePipeline = config.libAccess

  override def wrappedReceive: Receive = {

    case removeIt: RemoveFromLibrary => {
      acceptJob(removeIt, context.sender())
      val newJob = RemoveItemFromStorage(removeIt.resource.uri, config)
      storagePipeline ! createJob(newJob, removeIt)
    }
    case updateMeta: UpdateMetadata => {
      acceptJob(updateMeta, context.sender())
      val delete = updateMeta.metadataUpdate.kindOfUpdate.isDelete
      val resource = updateMeta.metadataUpdate.item.get
      val metadata = updateMeta.metadataUpdate.desc.get
      val name = if(metadata.name != "") metadata.name else resource.name
      val newJob = UpdateMetaInStorage(resource.uri, name, metadata.metadata, delete, config)
      storagePipeline ! createJob(newJob, updateMeta)
    }

    case removeCollItem: RemoveCollectionItem => {
      acceptJob(removeCollItem, context.sender())
      val newJob = RemoveCollectionItemFromStorage(removeCollItem.collectionItem.uri, config)
      storagePipeline ! createJob(newJob, removeCollItem)
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

object ModificationHandler{
  def props(config: LibInfo) = Props(new ModificationHandler(config))
}
