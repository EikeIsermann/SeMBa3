package data.storage

import akka.actor.Actor
import api._
import logic.core.{JobHandling, JobProtocol}
import logic.resourceCreation.{GenerateDatatypeProperties, SetDatatypeProperties}
import logic.SaveOntology
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.Model
import org.apache.jena.shared.Lock
import sembaGRPC._
import utilities.{Convert, UpdateMessageFactory}
          /*
/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
case class RegisterOntology(uri: String, model: OntModel) extends JobProtocol
case class DeleteItem(item: Resource) extends JobProtocol


abstract class StorageModification (uri: String) extends Actor with JobHandling{

//  override def receive: Receive = {
//    case job: JobProtocol => {
//      val sender = context.sender()
//       acceptJob(job, sender)
//       self ! handleJob(job)
//    }
//    case reply: JobReply => handleReply(reply, self)
//    }

    def library: Model





    def saveOntology( saveOnt: SaveOntology): UpdateMessage = {
      var save: Option[String] = None
      library.enterCriticalSection(Lock.WRITE)
      try{
        save = AccessMethods.writeModel(saveOnt.model)
      }
      finally library.leaveCriticalSection()
      UpdateMessage(kindOfUpdate = UpdateType.NOTIFY, lib = uri).addNotes(
        UpdateMessageFactory.getNotification("Ontology has been saved to file", "Could not save Ontology", save))
    }

    def removeItem( removeIt: DeleteItem): UpdateMessage

    def updateMetadata( updateMeta: UpdateMetadata ): UpdateMessage = {
      val item = updateMeta.metadataUpdate.item.get.uri
      val itemDesc = updateMeta.metadataUpdate.desc.get
      val model = AccessMethods.getModelForItem(item)
      val isDeletion = updateMeta.metadataUpdate.kindOfUpdate.isDelete

      library.enterCriticalSection(Lock.WRITE)
        try {
          val values = itemDesc.metadata.mapValues( v => v.value.toArray)
          AccessMethods.updateMetadata(values, item, model, library, isDeletion)
        }
        finally library.leaveCriticalSection()
      self ! createJob(SaveOntology(model), updateMeta)
      //TODO
      UpdateMessage()

    }
    def addToCollection( addToColl: AddToCollectionMsg): UpdateMessage ={
      val collection = addToColl.addToCollection.collection.get.uri
      val item = addToColl.addToCollection.newItem.get.uri
      val model = AccessMethods.getModelForItem(collection)
      AccessMethods.addCollectionItem(collection, item, model)
      self ! createJob(SaveOntology(model), addToColl)

      //TODO: return CollectionUpdate
      UpdateMessage()
    }

    def removeCollectionItem( removeCollItem: RemoveCollectionItem ): UpdateMessage ={
      val model = AccessMethods.getModelForItem(removeCollItem.collectionItem.parentCollection)
      library.enterCriticalSection(Lock.WRITE)
      try{
         AccessMethods.removeIndividual(removeCollItem.collectionItem.uri, model)
      }
      finally library.leaveCriticalSection()
      self ! createJob(SaveOntology(model), removeCollItem)
      //todo
      UpdateMessage()
    }

  def modifyRelation(relMod: RelationModification, add: Boolean): UpdateMessage = {
    val startItem = relMod.start.get.uri
    val endItem = relMod.end.get.uri
    val relation = relMod.rel.get.uri
    val model = AccessMethods.getModelForItem(
      relMod.start.get.parentCollection)
    var upd = UpdateMessageFactory.getAddMessage(uri)
      .addRelations(
        relMod
    )


    library.enterCriticalSection(Lock.WRITE)
    try {
      if (add) {
        AccessMethods.createRelation(startItem, endItem, relation, model)
      }
      else {
        AccessMethods.removeRelation(startItem, endItem, relation, model)
        upd = upd.withKindOfUpdate(UpdateType.DELETE)
      }
    }
    finally library.leaveCriticalSection()
    saveOntology(SaveOntology(model))
    upd
  }

def registerOntology(regModel: RegisterOntology): UpdateMessage//   ={
//    ont.begin(ReadWrite.WRITE)
//
//    try{
//      ont.addNamedModel(regModel.uri, regModel.model)
//    }
//    finally (ont.close())
//    LibraryAccess.addToLib(regModel.uri, lib(), regModel.model)
//    UpdateMessage(kindOfUpdate = UpdateType.NOTIFY, lib = uri).addNotes(
//      UpdateMessageFactory.getNotification("Ontology has been added to Library", "Could add Ontology", Option(regModel.uri)))
//  }

  def generateDatatypeProperties(genDataProp: GenerateDatatypeProperties): UpdateMessage ={
    lib().enterCriticalSection(Lock.WRITE)
    var update = false
    var concepts =
        LibraryConcepts().withLib(Convert.lib2grpc(uri))
    try{
    for( key <- genDataProp.keys){
      val newProp = AccessMethods.generateDatatypeProperty(key, genDataProp.model)
      if(newProp.isDefined)
        {
          update = true
          concepts = concepts.addAnnotations((key, Convert.ann2grpc(newProp.get)))
        }
    }
    }
    finally lib().leaveCriticalSection()

    if( update ) self ! createJob(SaveOntology(genDataProp.model), genDataProp)
    UpdateMessageFactory.getAddMessage(uri)
      .withConcepts(concepts)

  }

  def setDatatypeProperties(setDataProp: SetDatatypeProperties): UpdateMessage ={
    lib().enterCriticalSection(Lock.WRITE)
    var upd = UpdateMessageFactory.getAddMessage(uri)
    try
    {
      for(prop <- setDataProp.propertyMap.keySet){
        var annotation = AccessMethods.setDatatypeProperty(prop,
          setDataProp.model,setDataProp.item, setDataProp.propertyMap.apply(prop), lib())
        upd = upd.addDescriptions(ItemDescription()
          .withItemURI(setDataProp.item.getURI).addAllMetadata(annotation.map( v => (prop,v))))
      }
    }
    finally lib().leaveCriticalSection()
    //TOOD better saving strategy
    self ! createJob(SaveOntology(setDataProp.model), setDataProp)

    UpdateMessage()
  }

//  override def handleJob(job: JobProtocol): JobReply = {
//    JobReply(job, ArrayBuffer[UpdateMessage]
//    (job match {
//      case removeIt: DeleteItem => removeItem(removeIt)
//      case updateMeta: UpdateMetadata => updateMetadata(updateMeta)
//      case addToColl: AddToCollectionMsg => addToCollection(addToColl)
//      case removeCollItem: RemoveCollectionItem => removeCollectionItem(removeCollItem)
//      case createRel: CreateRelation => modifyRelation(createRel.relationModification, true)
//      case removeRel: RemoveRelation => modifyRelation(removeRel.relationModification, false)
//      case regModel: RegisterOntology => registerOntology(regModel)
//      case genDataProp: GenerateDatatypeProperties => generateDatatypeProperties(genDataProp)
//      case setDataProp: SetDatatypeProperties => setDatatypeProperties(setDataProp)
//      case saveOnt: SaveOntology => saveOntology(saveOnt)
//    })
//    )
//  }
}



          */

