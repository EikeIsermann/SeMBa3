package core.library.storage.model

import akka.agent.Agent
import core.{JobProtocol, JobReply, LibraryAccess}
import core.library.{DeleteItem, RegisterOntology, SaveOntology, StorageModification}
import org.apache.jena.rdf.model.Model
import sembaGRPC.{CollectionItem, UpdateMessage}
import utilities.UpdateMessageFactory

import scala.collection.immutable.HashMap

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

class ModelBasedModification(data: Agent[HashMap[String, Model]], uri: String) extends StorageModification(uri)
{
  override def library: Model = ???

  override def registerOntology(regModel: RegisterOntology): UpdateMessage = ???

  override def receive: Receive = ???

  override def handleJob(jobProtocol: JobProtocol): JobReply = ???

  override def removeItem(removeIt: DeleteItem): UpdateMessage = {
    val item = removeIt.item.uri

    var upd = UpdateMessageFactory.getDeletionMessage(uri)
    /** Remove all collectionitems referencing this item */
    val linkedCollectionItems = LibraryAccess.getCollectionItems(item, library)
    val mapIt = linkedCollectionItems.iterator
    while (mapIt.hasNext) {
      val keyVal = mapIt.next()
      val model = LibraryAccess.getModelForItem(keyVal._1)
      LibraryAccess.removeIndividual(keyVal._2, model)
      self ! createJob(SaveOntology(model), removeIt)
      upd = upd.addCollectionItems(
        CollectionItem(
          parentCollection = keyVal._1,
          uri = keyVal._2
        ))
    }

    val model = LibraryAccess.getModelForItem(item)
    LibraryAccess.removeFromLib(model, library)
    upd = upd.addItems(removeIt.item)
    upd
  }