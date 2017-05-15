package logic.resourceCreation

import java.io.File
import java.net.URI
import java.util.UUID

import data.storage.{AccessMethods, DatastructureMapping, SembaStorageComponent}
import globalConstants.GlobalMessages.{StorageReadRequest, StorageWriteRequest, StorageWriteResult}
import logic.core._
import logic.core.jobHandling.JobResult
import sembaGRPC._
import utilities.{TextFactory, UpdateMessageFactory}
import System.{currentTimeMillis => time}

import globalConstants.SembaPresets
import utilities.debug.DC

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object CreationStorageMethods {
   case class CreateInStorage(itemType: ItemType, ontClass: String, fileName: String, desc: ItemDescription, config: Config, thumb_path: String, id: UUID)
     extends StorageWriteRequest(createInStorage(itemType, ontClass, fileName, desc, config, thumb_path, id, _ ))


   //TODO performing all operations in one performWrite cycle returning the completed UpdateMessage might add performance.
   def createInStorage(itemType: ItemType, ontClass: String, fileName: String, desc: ItemDescription,
                       config: Config, thumb_path: String, id: UUID, storage: SembaStorageComponent): UpdateMessage =
   {
     //var test = DC.measure("Create Item")
     var update = UpdateMessageFactory.getAddMessage(config.libURI)

          val newResource = storage.performWrite(
             {
               val model = storage.getABox
               val res = AccessMethods.createItem(model, desc.name, ontClass, fileName, config, thumb_path, id)
               res
             }
          )
     update = update.addItems(newResource)
    // DC.stop(test)

   //  test = DC.measure("Generate Annotations")
     var prefixUri = config.constants.resourceBaseURI + SembaPresets.generatedPrefix
     var metadataLookup = desc.metadata.foldLeft(Map.empty[String,(String, AnnotationValue)]){case (lookup, valuePair) => lookup.updated(valuePair._1, (prefixUri + TextFactory.cleanString(valuePair._1), valuePair._2))}
     var itemDescription = desc
          val generatedProperties = storage.performWrite(
            {
              val model = storage.getTBox
              val desc = itemDescription.metadata.keys.map(key => AccessMethods.generateMetadataType(metadataLookup(key)._1, key, model, config))
                .foldLeft(Map.empty[String, Annotation])
                {
                  case (annotations, returnTuple) =>

                  if (returnTuple._1) annotations.updated(returnTuple._2.getURI,
                    DatastructureMapping.wrapAnnotation(returnTuple._2, config))
                  else annotations
                }
              desc
            }
          )
   //  DC.stop(test)

    // test = DC.measure("Add Metadata")

     update = update.addAnnotations(AnnotationUpdate(generatedProperties))
      var metadataEntries = metadataLookup.values.toMap
          storage.performWrite(
            {
             val props = AccessMethods.updateMetadata(newResource.uri, metadataEntries, storage.getABox, false)
             itemDescription = AccessMethods.retrieveMetadata(newResource.uri, storage.getABox)
              props
            }
          )
          update = update.addDescriptions(itemDescription)
    // DC.stop(test)

     update

   }

}
