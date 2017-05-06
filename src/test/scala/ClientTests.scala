
import app.Application
import app.testing.{ClientLib, SembaConnectionImpl, SembaConnectionImpl$}
import globalConstants.SembaPaths
import org.scalatest._
import org.scalatest.concurrent.{Eventually, TimeLimits, Timeouts}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.time.SpanSugar._
import sembaGRPC._

import scala.concurrent.Future
/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

class ClientTests extends FeatureSpec with GivenWhenThen with TimeLimits with Eventually with Matchers {

  override implicit val patienceConfig =
    PatienceConfig(timeout = scaled(Span(60, Seconds)), interval = scaled(Span(15, Millis)))
  var clientApi: SembaConnectionImpl = _
  var testLib: ClientLib = _
  val testFile = "file:///Users/uni/Desktop/TestLib/Test.pdf"// "file:///C:/Users/eikei_000/Desktop/TestLib/Test.pdf" //
  val libSource = "file:///Users/uni/Desktop/TestLib/library.ttl"// "file:///C:/Users/eikei_000/Desktop/TestLib/library.ttl" //
  val collectionClass =  "http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-teaching.owl#Program"
  val precedes =  "http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-teaching.owl#preceeds"
    val isExample =   "http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-teaching.owl#isExampleFor"
  val testingClient = RawClient.apply()
  val sembaMetadata = "file:///Users/uni/Desktop/TestLib/library.ttl#generatedMetadata_Creation-Date"  //  "file:///C:/Users/eikei_000/Desktop/TestLib/library.ttl#generatedMetadata_Creation-Date" //
  val changedItemName = "ClientTest"
  val newAnnotationValue = AnnotationValue().withValue(Seq("This", "Is", "A", "Test"))
  val collectionName = "TestCollection"
  var mediaItem: Resource = _
  var mediaCollection: Resource = _
  var firstCollectionItem: CollectionItem = _
  var secondCollectionItem: CollectionItem = _
  var metadata: ItemDescription = _
  val sparqlName = {

    "SELECT ?x\nWHERE\n { ?x <http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-main.owl#sembaTitle> \""+collectionName+"\" ." +
      "   }"
  }

  val sparqlCollection = {
    "SELECT ?mediaItem \nWHERE\n { ?collectionItem a <"+ SembaPaths.collectionItemURI +">;" +
      "<"+precedes+"> ?y ;" +
    "<" + SembaPaths.linksToSource +"> ?mediaItem ." +
      "   }"
  }


  feature("SeMBa Initialization"){
    scenario("The client registers a Session on the remote server")
    {
      When("registering new client session")
      clientApi = SembaConnectionImpl.apply()
      Then("the ID is correctly stored at the client")
      assert(clientApi.session != null)
    }

    scenario("The client requests to open a remote library")
    {
      When("opening a library")
      Then("the concepts and contents should be transmitted to the client in <15 seconds.")
       failAfter(15 seconds){
        testLib = new ClientLib(libSource, clientApi)
      }
      And("they should contain at least the SeMBa base concepts.")
      assert(testLib.concepts.itemClasses.contains("http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-main.owl#MediaItem"))
    }


  }
  feature("Item Creation") {
    scenario("Item Import") {
      When("Importing a file to SeMBa")
      val jobID = testLib.addItem(testFile).id
      Then(" an Update Message should be received")
      var update = Traversable.empty[UpdateMessage]
      Then(" an Update Message should be received")
      eventually {
        clientApi.lastUpdates.filter(x => x.jobID === jobID).size should be(1)
      }
      update = clientApi.lastUpdates.filter(x => x.jobID === jobID)
      assert(!update.isEmpty)
      And("the Item should be part of the library content.")
      mediaItem = update.head.items.head
      assert(testLib.content.contains(mediaItem.uri))
    }
    scenario("Collection Creation") {
      When("Adding a Collection to SeMBa")
      val jobID = testLib.addColl(collectionName, collectionClass).id
      Then(" an Update Message should be received")
      var update = Traversable.empty[UpdateMessage]
      Then(" an Update Message should be received")
      eventually {
        clientApi.lastUpdates.filter(x => x.jobID === jobID).size should be(1)
      }
      update = clientApi.lastUpdates.filter(x => x.jobID === jobID)
      assert(!update.isEmpty)
      And("the Collection should be part of the library content.")
      mediaCollection = update.head.items.head
      assert(testLib.content.contains(mediaCollection.uri))
      And("the Collection should be empty.")
      assert(testLib.requestCollectionContent(mediaCollection.uri).contents.isEmpty)

    }
  }

   feature("Collection Handling 1: Setup") {

    scenario("Adding an Item to a Collection") {
      When("Adding an Item to a Collection")
      val jobID = testLib.addToCollection(mediaItem.uri, mediaCollection.uri).id
      var update = Traversable.empty[UpdateMessage]
      Then(" an Update Message should be received")
      eventually {
        clientApi.lastUpdates.count(x => x.jobID === jobID) should be(1)
      }
      update = clientApi.lastUpdates.filter(x => x.jobID === jobID)
      And(" a new CollectionItem should be created.")
      assert(update.head.collectionItems.nonEmpty)
      And(" the CollectionItem should be part of the CollectionContent")
      assert(testLib.openCollections(mediaCollection.uri).contents.contains(update.head.collectionItems.head.uri))
      firstCollectionItem = testLib.openCollections(mediaCollection.uri).contents(update.head.collectionItems.head.uri)

    }

    scenario("Adding an Item item twice to a Collection") {
      When("Adding an Item twice to a Collection")
      val jobID = testLib.addToCollection(mediaItem.uri, mediaCollection.uri).id
      Then(" an Update Message should be received")
      var update = Traversable.empty[UpdateMessage]
      Then(" an Update Message should be received")
      eventually {
        clientApi.lastUpdates.count(x => x.jobID === jobID) should be(1)
      }
      update = clientApi.lastUpdates.filter(x => x.jobID === jobID)
      assert(update.nonEmpty)
      And(" a new CollectionItem should be created.")
      assert(update.head.collectionItems.nonEmpty)
      And(" the CollectionItem should be part of the CollectionContent")
      assert(testLib.openCollections(mediaCollection.uri).contents.contains(update.head.collectionItems.head.uri))
      secondCollectionItem = testLib.openCollections(mediaCollection.uri).contents(update.head.collectionItems.head.uri)
      And(" both CollectionItems have different URIs")
      assert(firstCollectionItem.uri != secondCollectionItem.uri)
      And(" both CollectionItems link to the same MediaItem")
      assert(firstCollectionItem.libraryResource === mediaItem.uri && secondCollectionItem.libraryResource === mediaItem.uri)
    }


    scenario("Connecting two Items in a Collection") {
      var update = Traversable.empty[UpdateMessage]
      When("Adding a connection from Item #1 to Item #2")
      val jobID = testLib.connectItems(firstCollectionItem, secondCollectionItem, precedes).id
      Then(" an Update Message should be received")
      eventually {
        clientApi.lastUpdates.count(x => x.jobID === jobID) should be(1)
      }
      update = clientApi.lastUpdates.filter(x => x.jobID === jobID)
      assert(!update.isEmpty)
      And("an updated CollectionItem should part of the UpdateMessage.")
      assert(!update.head.collectionItems.isEmpty)
      And(" the CollectionItem should be replaced in the CollectionContent")
      assert(testLib.openCollections(mediaCollection.uri).contents.contains(firstCollectionItem.uri))
      firstCollectionItem = testLib.openCollections(mediaCollection.uri).contents(firstCollectionItem.uri)
      And(" should contain the newly created connection")
      assert(firstCollectionItem.relations(precedes).destination.contains(secondCollectionItem.uri))
    }

    scenario("Adding a second Connection between two Items") {
      var update = Traversable.empty[UpdateMessage]
      When("Adding a connection from Item #1 to Item #2")
      val jobID = testLib.connectItems(firstCollectionItem, secondCollectionItem, isExample).id
      Then(" an Update Message should be received")
      eventually {
        clientApi.lastUpdates.count(x => x.jobID === jobID) should be(1)
      }
      update = clientApi.lastUpdates.filter(x => x.jobID === jobID)
      assert(!update.isEmpty)
      And("an updated CollectionItem should part of the UpdateMessage.")
      assert(!update.head.collectionItems.isEmpty)
      And(" the CollectionItem should be replaced in the CollectionContent")
      assert(testLib.openCollections(mediaCollection.uri).contents.contains(firstCollectionItem.uri))
      firstCollectionItem = testLib.openCollections(mediaCollection.uri).contents(firstCollectionItem.uri)
      And(" should contain the newly created connection")
      assert(firstCollectionItem.relations(isExample).destination.contains(secondCollectionItem.uri))
    }

  }

  feature("Metadata Modifications") {
    scenario("Metadata Retrieval") {
      When("Querying for an Items Metadata")
      failAfter(500 millis) {
        metadata = testLib.getMetadata(mediaItem.uri)
      }
      Then("The metadata object should contain at least the name of the Item")
      assert(metadata.name === mediaItem.name)
      }

      scenario("Adding Metadata to an Item") {
        When("Adding a Metadata Tag and updating the name")
        val add = ItemDescription(name = changedItemName).addMetadata((sembaMetadata, newAnnotationValue))
        val msg = MetadataUpdate().withItem(mediaItem).withAdd(add)
        var update = Traversable.empty[UpdateMessage]

        val jobID = testLib.saveMetadata(msg).id
        Then(" an Update Message should be received")
        eventually {
          update = clientApi.lastUpdates.filter(x => x.jobID === jobID)
          update.size should be(1)
        }
        And("an updated ItemDescription should part of the UpdateMessage.")
        assert(update.head.descriptions.nonEmpty)
        And(" the updated description should contain the added metadata values as well as the new name")
        val description = update.head.descriptions.head
        assert(description.name === changedItemName)
        newAnnotationValue.value.foreach(x => assert(description.metadata(sembaMetadata).value.contains(x)))
      }
    scenario("Removing Metadata from an Item") {
      When("Removing a Metadata Tag")
      val del = ItemDescription().addMetadata((sembaMetadata, newAnnotationValue))
      val add = ItemDescription(name = mediaItem.name)
      val msg = MetadataUpdate().withItem(mediaItem).withDelete(del).withAdd(add)
      var update = Traversable.empty[UpdateMessage]
      val jobID = testLib.saveMetadata(msg).id
      Then(" an Update Message should be received")
      eventually {
        clientApi.lastUpdates.count(x => x.jobID === jobID) should be(1)
      }
      update = clientApi.lastUpdates.filter(x => x.jobID === jobID)
      assert(!update.isEmpty)
      And("an updated ItemDescription should part of the UpdateMessage.")
      assert(!update.head.descriptions.isEmpty)
      And(" the updated description should not contain the removed metadata values")
      val description = update.head.descriptions.head
      assert(description.name != changedItemName)
      newAnnotationValue.value.foreach(x => assert(!description.metadata(sembaMetadata).value.contains(x)))
      }
    }

    feature ("Sparql Search"){
      scenario("Simple Search for name"){
        When("Performing a SparqlSearch for a MediaItem with a certain name")
        val result = testLib.sparql(sparqlName, Seq("?x"))
        Then("The result should only contain items with this name.")
        assert(!result.exists(x => x._2.name != collectionName))
      }
    scenario("Filter Library Contents for MediaItems that are part of a Collection with a certain Relation") {
      When("Performing a SparqlSearch for all MediaItems with a precedes collection in their collectionItem")
      val result = testLib.sparql(sparqlCollection, Seq("?mediaItem"))
      Then("The result should contain the current MediaItem.")
      assert(result.exists(x => x._2.name == mediaItem.name))
      }
    }



  feature("Collection Handling 2: Removal"){
  scenario("Removing a connection between two Items") {
    var update = Traversable.empty[UpdateMessage]
    When("Adding a connection from Item #1 to Item #2")
    val jobID = testLib.disconnectItems(firstCollectionItem, secondCollectionItem, isExample).id
    Then(" an Update Message should be received")
    eventually { clientApi.lastUpdates.count(x => x.jobID === jobID) should be (1)}
    update = clientApi.lastUpdates.filter(x => x.jobID === jobID)
    assert(!update.isEmpty)
    And("an updated CollectionItem should part of the UpdateMessage.")
    assert(!update.head.collectionItems.isEmpty)
    And(" the CollectionItem should be replaced in the CollectionContent")
    assert(testLib.openCollections(mediaCollection.uri).contents.contains(firstCollectionItem.uri))
    firstCollectionItem = testLib.openCollections(mediaCollection.uri).contents(firstCollectionItem.uri)
    And(" should contain the newly created connection")
    assert(!firstCollectionItem.relations.contains(isExample))
  }

  scenario("Removing a CollectionItem which is a connection destination") {
    var update = Traversable.empty[UpdateMessage]
    When("Adding a connection from Item #1 to Item #2")
    val jobID = testLib.removeFromCollection(secondCollectionItem, mediaCollection.uri).id
    Then(" an Update Message should be received")
    eventually { clientApi.lastUpdates.count(x => x.jobID === jobID) should be (1)}
    update = clientApi.lastUpdates.filter(x => x.jobID === jobID)
    assert(update.nonEmpty)
    And("an updated CollectionContent should part of the UpdateMessage.")
    assert(update.head.collectionContent.nonEmpty)
    And(" the CollectionItem should not be part of the CollectionContent")
    assert(!testLib.openCollections(mediaCollection.uri).contents.contains(secondCollectionItem.uri))
    And(" it the relations for the origin CollectionItem should be empty")
    assert(testLib.openCollections(mediaCollection.uri).contents(firstCollectionItem.uri).relations.isEmpty)
  }

  scenario("Removing a MediaItem Item which represents a CollectionItem") {
    var update = Traversable.empty[UpdateMessage]
    When("Adding a connection from Item #1 to Item #2")
    val jobID = testLib.removeItem(firstCollectionItem.libraryResource).id
    Then(" an Update Message should be received")
    eventually { clientApi.lastUpdates.count(x => x.jobID === jobID) should be (1)}
    update = clientApi.lastUpdates.filter(x => x.jobID === jobID)
    assert(update.nonEmpty)
    And("an updated CollectionContent should part of the UpdateMessage.")
    assert(update.head.items.nonEmpty)
    And(" the CollectionItem should not be part of the CollectionContent")
    assert(!testLib.openCollections(mediaCollection.uri).contents.contains(firstCollectionItem.uri))
    And(" it the relations for the origin CollectionItem should be empty")
    assert(!testLib.content.contains(mediaItem.uri))
  }
  }

  feature("Concurrency Check")
  {
    scenario("Adding an Item using a second Client Connection to the same library") {
      var update = Traversable.empty[UpdateMessage]
      When("Creating a Second Client Connection")
      val secApi = SembaConnectionImpl.apply()
      val secLib = new ClientLib(libSource, secApi)
      And("Creating importing a new Item using this Connection")

      val jobID = secLib.addItem(testFile).id
      Then(" an Update Message should be received by the original client")
      eventually {
        clientApi.lastUpdates.count(x => x.jobID === jobID) should be(1)
      }
      update = clientApi.lastUpdates.filter(x => x.jobID === jobID)
      assert(update.nonEmpty)
      And("the resource should be part of the Update message.")
      assert(update.head.items.nonEmpty)
      And(" the resource should be part of the original librarycontent")
      assert(testLib.content.contains(update.head.items.head.uri))
    }
  }
}
