package logic.resourceCreation

import java.io.File
import java.net.URI

import data.storage.{AccessMethods, DatastructureMapping, SembaStorageComponent}
import globalConstants.GlobalMessages.{StorageReadRequest, StorageWriteRequest, UpdateResult}
import logic.core._
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
                       config: LibInfo, storage: SembaStorageComponent): JobResult =
   {

     var update = UpdateMessageFactory.getAddMessage(config.libURI)

     val newResource = storage.performWrite(
        {
          val model = storage.getABox()
          val res = AccessMethods.createItem(model, desc.name, ontClass, fileName, config )
          storage.saveABox(model)
          res
        }
     )

     update = update.addItems(newResource)

     var itemDescription = desc
     val generatedProperties = storage.performWrite(
       {
         val model = storage.getTBox()
         val desc = itemDescription.metadata.keys.map(key => AccessMethods.generateDatatypeProperty(key, model, config))
           .foldLeft(Map.empty[String, Annotation])
           {
             case (annotations, property) =>
             {
             if (property.isDefined) annotations.updated(property.get.getLocalName, DatastructureMapping.wrapAnnotation(property.get, config))
             else annotations
             }
           }
         storage.saveTBox(model)
         desc
       }
     )
     update = update.withConcepts(LibraryConcepts().withAnnotations(generatedProperties))


     val validKeys = itemDescription.metadata.foldLeft(Map.empty[String, AnnotationValue])
     {
       case (acc, (key, value)) => acc.updated(config.libURI + TextFactory.cleanString(key), value)
     }
     itemDescription = itemDescription.withMetadata(validKeys)

     val setProperties = storage.performWrite(
       {
       val model = storage.getABox()
        val props = AccessMethods.updateMetadata(itemDescription.metadata, newResource.uri, model, false)
        storage.saveABox(model)
        props
       }
     )
     update = update.addDescriptions(itemDescription)

     JobResult(UpdateResult(ArrayBuffer[UpdateMessage](update)))
   }



}
