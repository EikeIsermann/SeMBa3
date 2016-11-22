package core.library

import akka.actor.Actor
import akka.agent.Agent
import api._
import app.Application
import core.{JobHandling, JobProtocol, JobReply, LibraryAccess}
import org.apache.jena.ontology.OntModel
import org.apache.jena.shared.Lock
import sembaGRPC._
import utilities.{Convert, UpdateMessageFactory}

import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
case class RegisterOntology(uri: String, model: OntModel) extends JobProtocol

class CriticalOntologyAccess(lib: Agent[OntModel], uri: String) extends Actor with JobHandling{

  override def receive: Receive = {
    case job: JobProtocol => {
      val sender = context.sender()
       acceptJob(job, sender)

      job match {
          case removeIt: RemoveFromLibrary => removeItem(removeIt)
          case updateMeta: UpdateMetadata => updateMetadata(updateMeta)
          case addToColl: AddToCollectionMsg => addToCollection(addToColl)
          case removeCollItem: RemoveCollectionItem => removeCollectionItem(removeCollItem)
          case createRel: CreateRelation => modifyRelation(createRel.relationModification, true)
          case removeRel: RemoveRelation => modifyRelation(removeRel.relationModification, false)
          case regModel: RegisterOntology => registerOntology(regModel)
          case genDataProp: GenerateDatatypeProperties => generateDatatypeProperties(genDataProp)
          case setDataProp: SetDatatypeProperties => setDatatypeProperties(setDataProp)
          case saveOnt: SaveOntology => saveOntology(saveOnt)
        }
    }
    case reply: JobReply => handleReply(reply, self)
    }


    def saveOntology( saveOnt: SaveOntology): Unit = {
      var save: Option[String] = None
      lib().enterCriticalSection(Lock.WRITE)
      try{
        save = LibraryAccess.writeModel(saveOnt.model)
      }
      finally lib().leaveCriticalSection()
      val message = UpdateMessage(UpdateType.NOTIFY, uri).withNote(
        UpdateMessageFactory.getNotification("Ontology has been saved to file", "Could not save Ontology", save))
      self ! JobReply(saveOnt, ArrayBuffer(message))
    }

    def removeItem( removeIt: RemoveFromLibrary) = {
      val item = removeIt.resource.uri

      /** Remove all collectionitems referencing this item */
      val linkedCollectionItems = LibraryAccess.getCollectionItems(item, lib())
      val mapIt = linkedCollectionItems.iterator
      while (mapIt.hasNext) {
        val keyVal = mapIt.next()
        val model = LibraryAccess.getModelForItem(keyVal._1)
        LibraryAccess.removeIndividual(keyVal._2, model)
      }
      for( c <- linkedCollectionItems.values.toList.distinct){

      }
      val model = LibraryAccess.getModelForItem(item)
      LibraryAccess.removeFromLib(model, lib())
      //TODO return libraryUpdate
    }

    def updateMetadata( updateMeta: UpdateMetadata ) = {
      val item = updateMeta.metadataUpdate.item.get.uri
      val itemDesc = updateMeta.metadataUpdate.desc.get
      val model = LibraryAccess.getModelForItem(item)
      val isDeletion = updateMeta.metadataUpdate.kindOfUpdate.isDelete

        lib().enterCriticalSection(Lock.WRITE)
        try {
          val values = itemDesc.metadata.mapValues( v => v.value.toArray)
          LibraryAccess.updateMetadata(values, item, model, isDeletion)
        }
        finally lib().leaveCriticalSection()

      // TODO: return MetadataUpdate
    }
    def addToCollection( addToColl: AddToCollectionMsg): Unit ={
      val collection = addToColl.addToCollection.collection.get.uri
      val item = addToColl.addToCollection.newItem.get.uri
      val model = LibraryAccess.getModelForItem(collection)
      LibraryAccess.addCollectionItem(collection, item, model)

      //TODO: return CollectionUpdate
    }

    def removeCollectionItem( removeCollItem: RemoveCollectionItem ): Unit ={
      val model = LibraryAccess.getModelForItem(removeCollItem.collectionItem.parentCollection)
      lib().enterCriticalSection(Lock.WRITE)
      try{
         LibraryAccess.removeIndividual(removeCollItem.collectionItem.uri, model)
      }
      finally lib().leaveCriticalSection()
    }

  def modifyRelation(relMod: RelationModification, add: Boolean) = {
    val startItem = relMod.start.get.uri
    val endItem = relMod.end.get.uri
    val relation = relMod.rel.get.uri
    val model = LibraryAccess.getModelForItem(
      relMod.start.get.parentCollection)

    lib().enterCriticalSection(Lock.WRITE)
    try {
      if (add) LibraryAccess.createRelation(startItem, endItem, relation, model)
      else LibraryAccess.removeRelation(startItem, endItem, relation, model)
    }
    finally lib().leaveCriticalSection()
    //Todo return CollectionUpdate
  }

  def registerOntology(regModel: RegisterOntology): Unit ={
    LibraryAccess.addToLib(regModel.uri, lib(), regModel.model)
    self ! JobReply(regModel)
  }

  def generateDatatypeProperties(genDataProp: GenerateDatatypeProperties): Unit ={
    lib().enterCriticalSection(Lock.WRITE)
    try{
    for( key <- genDataProp.keys){
      LibraryAccess.generateDatatypeProperty(key, genDataProp.model)
    }
    }
    finally lib().leaveCriticalSection()
    self ! JobReply(genDataProp)
  }

  def setDatatypeProperties(setDataProp: SetDatatypeProperties): Unit ={
    lib().enterCriticalSection(Lock.WRITE)
    try
    {
      for(prop <- setDataProp.propertyMap.keySet){
        LibraryAccess.setDatatypeProperty(prop, setDataProp.model,setDataProp.item, setDataProp.propertyMap.apply(prop))
      }
    }
    finally lib().leaveCriticalSection()

    if(LibraryAccess.activeModels().values.exists(_ == setDataProp.model)){
      println("test")
      //TODO send datatypeProperty update
    }
    self ! JobReply(setDataProp)
  }

  override def handleJob(jobProtocol: JobProtocol): JobReply = {
    JobReply(jobProtocol)
  }
}



