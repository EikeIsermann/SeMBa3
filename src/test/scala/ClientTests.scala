
import app.Application
import app.testing.{ClientImpl, ClientLib}
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
  var clientApi: ClientImpl = _
  var testLib: ClientLib = _
  val testFile = "file:///C:/Users/eikei_000/Desktop/TestLib/Test.pdf" //"file:///Users/uni/Desktop/TestLib/Test.pdf"//
  val libSource = "file:///C:/Users/eikei_000/Desktop/TestLib/library.ttl" //"file:///Users/uni/Desktop/TestLib/library.ttl"//
  val collectionClass =  "http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-teaching.owl#Program"
  val precedes =  "http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-teaching.owl#preceeds"
    val isExample =   "http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-teaching.owl#isExampleFor"
  val testingClient = RawClient.apply()
  val sembaMetadata = "http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-main.owl#sembaMetadata"
  val changedItemName = "ClientTest"
  val newAnnotationValue = AnnotationValue().withValue(Seq("This", "Is", "A", "Test"))
  var mediaItem: Resource = _
  var mediaCollection: Resource = _
  var firstCollectionItem: CollectionItem = _
  var secondCollectionItem: CollectionItem = _
  var metadata: ItemDescription = _



  feature("SeMBa Initialization"){
    scenario("The client registers a Session on the remote server")
    {
      When("registering new client session")
      clientApi = ClientImpl.apply()
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
      val jobID = testLib.addColl("TestCollection", collectionClass).id
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

      scenario("Metadata Update") {
        When("Adding a Metadata Tag")
        val add = ItemDescription(name = changedItemName).addMetadata((sembaMetadata, newAnnotationValue))
        val msg = MetadataUpdate().withItem(mediaItem).withAdd(add)
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

      }
    }

    feature ("Sparql Search"){
      scenario("Simple Search for Metadata Entries"){
        assert(1 === 1)
      }


    scenario("Filter Library Contents for MediaItems that are part of a Collection with a certain Relation") {
      assert(1 === 1)
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
    scenario("Adding an Item using a second Client Connection to the same library"){assert(1 === 1)}
  }



}
