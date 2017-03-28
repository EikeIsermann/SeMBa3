package logic.resourceCreation

import java.io.File
import java.net.URI

import data.storage.{AccessMethods, DatastructureMapping, SembaStorageComponent}
import globalConstants.GlobalMessages.{StorageReadRequest, StorageWriteRequest, UpdateResult}
import logic.core.{JobProtocol, JobReply, JobResult, LibInfo}
import sembaGRPC._
import utilities.{TextFactory, UpdateMessageFactory}

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object CreationStorageMethods {
   case class CreateInStorage(itemType: ItemType, ontClass: String, fileName: String, desc: ItemDescription, config: LibInfo)
     extends StorageWriteRequest(createInStorage(itemType, ontClass, fileName, desc, config, _ ))



   def createInStorage(itemType: ItemType, ontClass: String, fileName: String, desc: ItemDescription,
                       config: LibInfo, storage: SembaStorageComponent): UpdateResult =
   {
     var update = UpdateMessageFactory.getAddMessage(config.libURI)

     val newResource = storage.performWrite(
        {
           AccessMethods.createItem(storage.getABox, desc.name, ontClass, fileName, config )
        }
     )

     update = update.addItems(newResource)

     var itemDescription = desc
     val generatedProperties = storage.performWrite(
       {
         itemDescription.metadata.keys.map(key => AccessMethods.generateDatatypeProperty(key, storage.getTBox(), config))
           .foldLeft(Map.empty[String, Annotation])
           {
             case (annotations, property) =>
             {
             if (property.isDefined) annotations.updated(property.get.getLocalName, DatastructureMapping.wrapAnnotation(property.get, config))
             else annotations
             }
           }
       }
     )
     update = update.withConcepts(LibraryConcepts().withAnnotations(generatedProperties))


     val validKeys = itemDescription.metadata.foldLeft(Map.empty[String, AnnotationValue])
     {
       case (acc, (key, value)) => acc.updated(config.libURI + TextFactory.cleanString(key), value)
     }
     itemDescription = itemDescription.withMetadata(validKeys)

     val setProperties = storage.performWrite(
        AccessMethods.updateMetadata(itemDescription.metadata, newResource.uri, storage.getABox, false)
     )
     update = update.addDescriptions(itemDescription)

     UpdateResult(ArrayBuffer[UpdateMessage](update))
   }



}
