package logic.itemModification

import app.Application
import data.storage.{AccessMethods, SembaStorageComponent}
import globalConstants.GlobalMessages.{StorageWriteRequest, StorageWriteResult}
import logic.core.Config
import logic.core.jobHandling.JobResult
import sembaGRPC.{AnnotationValue, ItemDescription, UpdateMessage}
import utilities.UpdateMessageFactory

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object ModificationMethods {

  case class UpdateMetaInStorage(item: String, name: Option[String], added: Option[ItemDescription], deleted: Option[ItemDescription]
                                 ,config: Config)
    extends StorageWriteRequest(updateMetaInStorage(item, name, added, deleted, config, _))

  def updateMetaInStorage(item: String, name: Option[String], addDesc: Option[ItemDescription], deleteDesc: Option[ItemDescription],
                          config: Config, storage: SembaStorageComponent): UpdateMessage = {

    var update = UpdateMessageFactory.getReplaceMessage(config.libURI)

    val desc = storage.performWrite(
  {
    val model = storage.getABox
      name.foreach(itemName => update = update.addItems(AccessMethods.setName(item, itemName, model, config)))
      addDesc.foreach( add => AccessMethods.updateMetadata(item, add.metadata, model, false))
      deleteDesc.foreach( del => AccessMethods.updateMetadata(item, del.metadata, model, true))
      update = update.addDescriptions(AccessMethods.retrieveMetadata(item, storage.getABox))
  })


    update
  }


  case class RemoveItemFromStorage(item: String, config: Config)
    extends StorageWriteRequest(removeItemFromStorage(item, config, _))
    def removeItemFromStorage(item: String, config: Config, storage: SembaStorageComponent): UpdateMessage = {
      var update = UpdateMessageFactory.getDeletionMessage(config.libURI)
      var updateCollections = UpdateMessageFactory.getReplaceMessage(config.libURI)

      update = storage.performWrite(
        {
          val model = storage.getABox
          val allCollectionItems = AccessMethods.getCollectionItems(item, model)

          val collectionItems = for(cItem <- allCollectionItems)
            yield AccessMethods.removeCollectionItem(cItem,model,config)

          collectionItems.foreach(citem ⇒ updateCollections = updateCollections.addCollectionContent(AccessMethods.retrieveCollectionContent(model, citem.parentCollection, config)))
          Application.api ! updateCollections

          val deletedResource = AccessMethods.removeIndividual(item, model, config)

          update.addItems(deletedResource)
        }
      )
      update
    }

  case class AddToCollectionInStorage(collection: String, item: String, config: Config)
    extends StorageWriteRequest(addToCollectionInStorage(collection, item, config, _))
    def addToCollectionInStorage(collection: String, item: String, config: Config,
                                 storage: SembaStorageComponent ): UpdateMessage = {

      var update = UpdateMessageFactory.getAddMessage(config.libURI)

      update = storage.performWrite(

         update.addCollectionItems(AccessMethods.addCollectionItem(collection,  item, storage.getABox,config))
       )

       update
    }

  case class RemoveCollectionItemFromStorage(item: String, parentCollection: String, config: Config)
    extends StorageWriteRequest(removeCollectionItemFromStorage(item, parentCollection, config, _))
    def removeCollectionItemFromStorage(item: String, parentCollection: String, config: Config, storage: SembaStorageComponent): UpdateMessage = {

      var update = UpdateMessageFactory.getReplaceMessage(config.libURI)

      update = storage.performWrite(
        {
        val model = storage.getABox
        AccessMethods.removeCollectionItem(item, model, config)
        update.addCollectionContent(AccessMethods.retrieveCollectionContent(model, parentCollection,config))
        }
      )

      update
    }


  case class ModifyCollectionRelationInStorage(origin: String, destination: String, relation: String, config: Config,
                                               delete: Boolean)
    extends StorageWriteRequest(modifyCollectionRelationInStorage(origin, destination, relation, config, delete, _))
    def modifyCollectionRelationInStorage(origin: String, destination: String, relation: String, config: Config,
                                          delete: Boolean, storage: SembaStorageComponent): UpdateMessage = {
      var update = UpdateMessageFactory.getReplaceMessage(config.libURI)
      val updatedCollectionItem = storage.performWrite(
        if (delete) AccessMethods.removeCollectionRelation(origin, destination, relation, storage.getABox, config)
        else AccessMethods.createCollectionRelation(origin, destination, relation, storage.getABox, config)
        )

      update = update.addCollectionItems(updatedCollectionItem)

      update
    }





}
