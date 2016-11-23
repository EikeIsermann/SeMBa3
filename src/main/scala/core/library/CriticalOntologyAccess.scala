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
case class DeleteItem(item: Resource) extends JobProtocol
class CriticalOntologyAccess(lib: Agent[OntModel], uri: String) extends Actor with JobHandling{

  override def receive: Receive = {
    case job: JobProtocol => {
      val sender = context.sender()
       acceptJob(job, sender)
       self ! handleJob(job)
    }
    case reply: JobReply => handleReply(reply, self)
    }


    def saveOntology( saveOnt: SaveOntology): UpdateMessage = {
      var save: Option[String] = None
      lib().enterCriticalSection(Lock.WRITE)
      try{
        save = LibraryAccess.writeModel(saveOnt.model)
      }
      finally lib().leaveCriticalSection()
      UpdateMessage(kindOfUpdate = UpdateType.NOTIFY, lib = uri).addNotes(
        UpdateMessageFactory.getNotification("Ontology has been saved to file", "Could not save Ontology", save))
    }

    def removeItem( removeIt: DeleteItem): UpdateMessage = {
      val item = removeIt.item.uri
      var upd = UpdateMessageFactory.getDeletionMessage(uri)
      /** Remove all collectionitems referencing this item */
      val linkedCollectionItems = LibraryAccess.getCollectionItems(item, lib())
      val mapIt = linkedCollectionItems.iterator
      while (mapIt.hasNext) {
        val keyVal = mapIt.next()
        val model = LibraryAccess.getModelForItem(keyVal._1)

        LibraryAccess.removeIndividual(keyVal._2, model)

        upd = upd.addCollectionItems(
          CollectionItem(
          parentCollection = keyVal._1,
          uri = keyVal._2
        ))
      }

      val model = LibraryAccess.getModelForItem(item)
      LibraryAccess.removeFromLib(model, lib())
      upd = upd.addItems(removeIt.item)
      upd
    }

    def updateMetadata( updateMeta: UpdateMetadata ): UpdateMessage = {
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
      //TODO
      UpdateMessage()

    }
    def addToCollection( addToColl: AddToCollectionMsg): UpdateMessage ={
      val collection = addToColl.addToCollection.collection.get.uri
      val item = addToColl.addToCollection.newItem.get.uri
      val model = LibraryAccess.getModelForItem(collection)
      LibraryAccess.addCollectionItem(collection, item, model)

      //TODO: return CollectionUpdate
      UpdateMessage()
    }

    def removeCollectionItem( removeCollItem: RemoveCollectionItem ): UpdateMessage ={
      val model = LibraryAccess.getModelForItem(removeCollItem.collectionItem.parentCollection)
      lib().enterCriticalSection(Lock.WRITE)
      try{
         LibraryAccess.removeIndividual(removeCollItem.collectionItem.uri, model)
      }
      finally lib().leaveCriticalSection()
      //todo
      UpdateMessage()
    }

  def modifyRelation(relMod: RelationModification, add: Boolean): UpdateMessage = {
    val startItem = relMod.start.get.uri
    val endItem = relMod.end.get.uri
    val relation = relMod.rel.get.uri
    val model = LibraryAccess.getModelForItem(
      relMod.start.get.parentCollection)
    var upd = UpdateMessageFactory.getAddMessage(uri)
      .addRelations(
        relMod
    )


    lib().enterCriticalSection(Lock.WRITE)
    try {
      if (add) {
        LibraryAccess.createRelation(startItem, endItem, relation, model)
      }
      else {
        LibraryAccess.removeRelation(startItem, endItem, relation, model)
        upd = upd.withKindOfUpdate(UpdateType.DELETE)
      }
    }
    finally lib().leaveCriticalSection()
    upd
  }

  def registerOntology(regModel: RegisterOntology): UpdateMessage ={
    LibraryAccess.addToLib(regModel.uri, lib(), regModel.model)
    UpdateMessage(kindOfUpdate = UpdateType.NOTIFY, lib = uri).addNotes(
      UpdateMessageFactory.getNotification("Ontology has been added to Library", "Could add Ontology", Option(regModel.uri)))
  }

  def generateDatatypeProperties(genDataProp: GenerateDatatypeProperties): UpdateMessage ={
    lib().enterCriticalSection(Lock.WRITE)
    var concepts =
        LibraryConcepts().withLib(Convert.lib2grpc(uri))
    try{
    for( key <- genDataProp.keys){
      val newProp = LibraryAccess.generateDatatypeProperty(key, genDataProp.model)
      if(newProp.isDefined)
        {
          concepts = concepts.addAnnotations((key, Convert.ann2grpc(newProp.get)))
        }
    }
    }
    finally lib().leaveCriticalSection()
    UpdateMessageFactory.getAddMessage(uri)
      .withConcepts(concepts)

  }

  def setDatatypeProperties(setDataProp: SetDatatypeProperties): UpdateMessage ={
    lib().enterCriticalSection(Lock.WRITE)
    var upd = UpdateMessageFactory.getAddMessage(uri)
    try
    {
      for(prop <- setDataProp.propertyMap.keySet){
        var annotation = LibraryAccess.setDatatypeProperty(prop,
          setDataProp.model,setDataProp.item, setDataProp.propertyMap.apply(prop))
        upd = upd.addDescriptions(ItemDescription()
          .withItemURI(setDataProp.item.getURI).addAllMetadata(annotation.map( v => (prop,v))))
      }
    }
    finally lib().leaveCriticalSection()


    UpdateMessage()
  }

  override def handleJob(job: JobProtocol): JobReply = {
    JobReply(job, ArrayBuffer[UpdateMessage]
    (job match {
      case removeIt: DeleteItem => removeItem(removeIt)
      case updateMeta: UpdateMetadata => updateMetadata(updateMeta)
      case addToColl: AddToCollectionMsg => addToCollection(addToColl)
      case removeCollItem: RemoveCollectionItem => removeCollectionItem(removeCollItem)
      case createRel: CreateRelation => modifyRelation(createRel.relationModification, true)
      case removeRel: RemoveRelation => modifyRelation(removeRel.relationModification, false)
      case regModel: RegisterOntology => registerOntology(regModel)
      case genDataProp: GenerateDatatypeProperties => generateDatatypeProperties(genDataProp)
      case setDataProp: SetDatatypeProperties => setDatatypeProperties(setDataProp)
      case saveOnt: SaveOntology => saveOntology(saveOnt)
    })
    )
  }
}



