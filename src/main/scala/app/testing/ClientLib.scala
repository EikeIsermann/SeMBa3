package app.testing

import sembaGRPC._
import utilities.debug.DC

import scala.collection.mutable.{HashMap, ListBuffer}
import scala.collection.mutable

/**
  * Created by Eike on 28.04.2017.
  */
class ClientLib(path: String, api: SembaConnectionImpl) {
  var stamp = DC.measure("Setting up library")
  val lib = Library(path)
  var concepts = api.openLib(lib, this)
  var content = mutable.HashMap.empty[String, Resource].++:(api.getContent(lib).libContent)
  var openMetadata = mutable.HashMap.empty[String, ItemDescription]
  var openCollections = mutable.HashMap.empty[String, CollectionContent]
  DC.stop(stamp)


  def close = api.closeLib(lib, this)

  def addItem(src: String) = api.addItem(src, lib)

  def addColl(name: String, clazz: String): VoidResult = {
    assert(concepts.collectionClasses.contains(clazz))
    api.addColl(name,clazz,lib)
  }

  def getMetadata(src: String): ItemDescription = {
    assert(content.contains(src))
    openMetadata.getOrElse(src,
    {
      val desc = api.getMetadata(src, lib)
      openMetadata.put(src, desc)
      desc
    })
  }

  def saveMetadata(update: MetadataUpdate): VoidResult = {
    assert(content.contains(update.item.get.uri))
    api.updateMetadata(update)
  }

  def removeItem(src: String): VoidResult = {
    assert(content.contains(src))
    api.removeItem(src,lib)
  }

  def sparqlToLibContent(query: String, vars: Seq[String]): Map[String, Resource] = {
    val result = api.sparql(query, vars, lib)
    //Assumes head of vars is the Resource.
    val allResourceResults = result.results.map(entry =>
      entry.results(vars.head)
    )
    allResourceResults.foldLeft(Map.empty[String, Resource]) {
      case (acc, uri) => {
        acc.updated(uri, content(uri))
      }
    }
  }

  def sparql(query: String, vars: Seq[String]): FilterResult = {
    api.sparql(query, vars, lib)
  }


  def requestCollectionContent(src: String): CollectionContent = {
    assert(content(src).itemType.isCollection)
    val coll = api.requestCollectionContent(content(src))
    openCollections.put(src, coll)
    coll
  }

  def writeResults(name: String) = api.writeResults(lib, name)

  def addToCollection(src: String, coll: String): VoidResult = {
    assert(content.contains(src) && content(coll).itemType.isCollection)
    val msg = AddToCollection().withNewItem(content(src)).withCollection(content(coll))
    api.addToCollection(msg)
  }

  def removeFromCollection(src: CollectionItem, coll: String): VoidResult = {
    assert(openCollections.contains(coll) && openCollections(coll).contents.contains(src.uri))
    api.removeFromCollection(src)
  }

  private def getConnectionMod(src: CollectionItem, dest: CollectionItem, rel: String): RelationModification ={
    assert(src.parentCollection == dest.parentCollection && concepts.collectionRelations.contains(rel))
    RelationModification()
      .withLibrary(lib)
      .withStart(src)
      .withEnd(dest)
      .withRel(concepts.collectionRelations(rel))
  }

  def connectItems(src: CollectionItem, dest: CollectionItem, rel: String): VoidResult = {
    api.createRelation(getConnectionMod(src,dest,rel))
  }

  def disconnectItems(src: CollectionItem, dest: CollectionItem, rel: String): VoidResult = {
    api.removeRelation(getConnectionMod(src,dest,rel))
  }

  def ping = api.ping("")

  def removedFromCollection(collectionItem: CollectionItem): Unit = {
    val key = collectionItem.parentCollection
    val collection = openCollections(key)
    openCollections.update(key, collection.withContents(collection.contents.-(key)))
  }

  def removedFromLibrary(resource: Resource) = {
    content.remove(resource.uri)
    openMetadata.remove(resource.uri)
  }

  def updatedDescription(desc: ItemDescription) = {
    if(openMetadata.contains(desc.itemURI))openMetadata.update(desc.itemURI, desc)
  }

  def updatedItem(resource: Resource) = {
    content.update(resource.uri, resource)
  }

  def updatedCollectionContent(collectionContent: CollectionContent) = {
    val key = collectionContent.uri
    if(openCollections.contains(key))
      {
        openCollections.update(key, collectionContent)
      }
  }

  def updatedCollectionItem(collectionItem: CollectionItem) = {
    val key = collectionItem.parentCollection
    if(openCollections.contains(key)) {
      val collection = openCollections(key)
      openCollections.update(key, collection.withContents(collection.contents.updated(collectionItem.uri, collectionItem)))
    }
  }


  def addedItem(resource: Resource) = {
    content.put(resource.uri, resource)
  }

  def addedAnnotations(annotations: AnnotationUpdate) = {
    concepts = concepts.addAllAnnotations(annotations.map)
  }

  def addedCollectionItem(collectionItem: CollectionItem) = {
    val key = collectionItem.parentCollection
    if(openCollections.contains(key)) {
      val collection = openCollections(key)
      openCollections.update(key, collection.withContents(collection.contents.updated(collectionItem.uri, collectionItem)))
    }
  }

  def addedDescription(desc: ItemDescription) = {
    openMetadata.update(desc.itemURI, desc)
  }
}
