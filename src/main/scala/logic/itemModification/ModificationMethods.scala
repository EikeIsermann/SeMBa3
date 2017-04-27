package logic.itemModification

import data.storage.{AccessMethods, SembaStorageComponent}
import globalConstants.GlobalMessages.{StorageWriteRequest, StorageWriteResult}
import logic.core.Config
import logic.core.jobHandling.JobResult
import sembaGRPC.AnnotationValue
import utilities.UpdateMessageFactory

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object ModificationMethods {

  case class UpdateMetaInStorage(item: String, name: String, valueMap: Map[String, AnnotationValue],
                                 delete: Boolean, config: Config)
    extends StorageWriteRequest(updateMetaInStorage(item, name, valueMap, delete, config, _))

  def updateMetaInStorage(item: String, name: String, valueMap: Map[String, AnnotationValue], delete: Boolean,
                          config: Config, storage: SembaStorageComponent): JobResult = {

    var update = UpdateMessageFactory.getReplaceMessage(config.libURI)

    val desc = storage.performWrite(AccessMethods.updateMetadata(item, name, valueMap, storage.getABox(), delete))

    update = update.addDescriptions(AccessMethods.retrieveMetadata(item, storage.getABox()))

    JobResult(StorageWriteResult(update))
  }


  case class RemoveItemFromStorage(item: String, config: Config)
    extends StorageWriteRequest(removeItemFromStorage(item, config, _))
    def removeItemFromStorage(item: String, config: Config, storage: SembaStorageComponent): JobResult = {
      var update = UpdateMessageFactory.getDeletionMessage(config.libURI)

      update = storage.performWrite(
        {
          val model = storage.getABox()

          val collectionItems = for(cItem <- AccessMethods.getCollectionItems(item, model))
            yield AccessMethods.removeCollectionItem(item,model,config)

          val deletedResource = AccessMethods.removeIndividual(item, model, config)

          update.addAllCollectionItems(collectionItems).addItems(deletedResource)
        }
      )

      JobResult(StorageWriteResult(update))

    }

  case class AddToCollectionInStorage(collection: String, item: String, config: Config)
    extends StorageWriteRequest(addToCollectionInStorage(collection, item, config, _))
    def addToCollectionInStorage(collection: String, item: String, config: Config,
                                 storage: SembaStorageComponent ): JobResult = {

      var update = UpdateMessageFactory.getAddMessage(config.libURI)

      update = storage.performWrite(

         update.addCollectionItems(AccessMethods.addCollectionItem(collection,  item, storage.getABox(),config))
       )

       JobResult(StorageWriteResult(update))
    }

  case class RemoveCollectionItemFromStorage(item: String, config: Config)
    extends StorageWriteRequest(removeCollectionItemFromStorage(item, config, _))
    def removeCollectionItemFromStorage(item: String, config: Config, storage: SembaStorageComponent): JobResult = {

      var update = UpdateMessageFactory.getDeletionMessage(config.libURI)

      update = storage.performWrite(
        update.addCollectionItems(AccessMethods.removeCollectionItem(item, storage.getABox(), config))
      )

      JobResult(StorageWriteResult(update))
    }


  case class ModifyCollectionRelationInStorage(origin: String, destination: String, relation: String, config: Config,
                                               delete: Boolean)
    extends StorageWriteRequest(modifyCollectionRelationInStorage(origin, destination, relation, config, delete, _))
    def modifyCollectionRelationInStorage(origin: String, destination: String, relation: String, config: Config,
                                          delete: Boolean, storage: SembaStorageComponent): JobResult = {
      var update = UpdateMessageFactory.getReplaceMessage(config.libURI)
      val updatedCollectionItem = storage.performWrite(
        if (delete) AccessMethods.removeCollectionRelation(origin, destination, relation, storage.getABox, config)
        else AccessMethods.createCollectionRelation(origin, destination, relation, storage.getABox, config)
        )

      update = update.addCollectionItems(updatedCollectionItem)

      JobResult(StorageWriteResult(update))
    }





}
