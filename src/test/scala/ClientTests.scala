
import app.Application
import app.testing.{ClientImpl, ClientLib}
import globalConstants.SembaPaths
import org.scalatest._
import org.scalatest.concurrent.{TimeLimits, Timeouts}
import org.scalatest.time.Span
import org.scalatest.time.SpanSugar._
import sembaGRPC._

import scala.concurrent.Future
/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */

class ClientSpec extends FeatureSpec with GivenWhenThen with TimeLimits {
  var clientApi: ClientImpl = _
  var testLib: ClientLib = _
  val testFile = "file:///C:/Users/eikei_000/Desktop/TestLib/Test.pdf"
  val libSource = "file:///C:/Users/eikei_000/Desktop/TestLib/library.ttl"
  val collectionClass =  "http://www.hci.uni-wuerzburg.de/ontologies/semba/semba-teaching.owl#Program"
  val testingClient = RawClient.apply()
  var mediaItem: Resource = _
  var mediaCollection: Resource = _


  info("As a SeMBa client")
  info("I want to remotely connect to a SeMBa application")

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
  feature("Modification") {
    scenario("Item Import") {
      When("Importing a file to SeMBa")
      val jobID = testLib.addItem(testFile).id
      Then(" an Update Message should be received")
      Thread.sleep(5000)
      assert(!clientApi.lastUpdates.isEmpty)
      val update = clientApi.lastUpdates.filter(x => x.jobID == jobID)
      assert(!update.isEmpty)
      And("the Item should be part of the library content.")
      mediaItem = update.head.items.head
      assert(testLib.content.contains(mediaItem.uri))
    }
    scenario("Collection Creation") {
      When("Adding a Collection to SeMBa")
      val jobID = testLib.addColl("TestCollection", collectionClass).id
      Then(" an Update Message should be received")
      Thread.sleep(2000)
      assert(!clientApi.lastUpdates.isEmpty)
      val update = clientApi.lastUpdates.filter(x => x.jobID == jobID)
      assert(!update.isEmpty)
      And("the Collection should be part of the library content.")
      mediaCollection = update.head.items.head
      assert(testLib.content.contains(mediaCollection.uri))
      And("the Collection should be empty.")
      assert(testLib.requestCollectionContent(mediaCollection.uri).contents.isEmpty)

    }
    scenario("Adding Collection Items") {
      When("Adding an Item to a Collection")
      val jobID = testLib.addToCollection(mediaItem.uri, mediaCollection.uri).id
      Then(" an Update Message should be received")
      Thread.sleep(2000)
      assert(!clientApi.lastUpdates.isEmpty)
      val update = clientApi.lastUpdates.filter(x => x.jobID == jobID)
      assert(!update.isEmpty)
      And(" a new CollectionItem should be created.")
      assert(!update.head.collectionItems.isEmpty)
      And(" the CollectionItem should be part of the CollectionContent")
      assert(testLib.openCollections(mediaCollection.uri).contents.contains(update.head.collectionItems.head.uri))
    }

    /*TODO
      * Add mediaItem a second time
      * Connect CollectionItems
      * Add second Connection
      * Remove a Connection
      * Remove destination media item
      * Update Item Description
      * Perform Sparql
     */
  }

}
