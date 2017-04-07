
import app.TestClient
import app.Application
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
  val client = TestClient.apply("localhost", 50051)
  val testLib = Library("file:///Users/uni/Documents/SeMBa3/src/test/resources/testLib/library.ttl")
  val testFile = "file:/Users/uni/Documents/SeMBa3/src/test/resources/testLib/image.jpg"
  val testFileName = "image"
  var concepts: LibraryConcepts = _
  var session: String = _
  var contents: LibraryContent = _
  var addMessage: Option[(Long, UpdateMessage)] = None

  val next = (value: UpdateMessage) => {
    value.kindOfUpdate match {
      case UpdateType.NOTIFY =>
      case UpdateType.DELETE =>
      case UpdateType.REPLACE =>
      case UpdateType.ADD => addMessage = Some(System.currentTimeMillis(), value)
    }
  }



  info("As a SeMBa client")
  info("I want to remotely connect to a SeMBa application")

  feature("SeMBa Initialization"){
    scenario("The client registers a Session on the remote server")
    {
      Given("a working connection to the remote SeMBa Application")
      assert(client.ping("test") === "test")
      When("registering a session")
      session = client.registerSession().sessionID
      Then("the ID is correctly stored at the client")
      assert(client.sessionID == session)
      And("the client can subscribe for updates")
      client.subscribeForUpdates(session, next)
    }

    scenario("The client requests to open a remote library")
    {
      Given("a working connection to the remote SeMBa Application")
      assert(client.ping("test") === "test")
      When("opening a library")
      Then("the concepts should be transmitted to the client in <5 seconds.")
       concepts = failAfter(10 seconds){
        client.openLib(LibraryRequest().withLib(testLib).withSessionID( client.sessionID))
      }
      And("they should contain at least the SeMBa base concepts.")
      assert(concepts.itemClasses.contains("MediaItem"))
    }

    scenario("The client requests the contents of a library")
    {
      Given("a working connection to the remote SeMBa Application")
      assert(client.ping("test") === "test")
      When("requesting the contents of a library")
      Then("the contents should be transmitted to the client in <5 seconds.")
      contents = failAfter(5 seconds) {
        client.getContent(testLib)
      }
    }
  }

  feature("Modification"){
    scenario("Item Import"){
      Given("a working connection to the remote SeMBa Application")
      assert(client.ping("test") === "test")
      When("Importing a file to SeMBa")
      val contentSize = contents.libContent.size
      addMessage = None
      val time = System.currentTimeMillis()
      client.addItem(testFile, testLib)
      Then(" an Update Message should be received")
      Thread.sleep(15000)
      assert(addMessage.get._2.items.head.name === testFileName)
      And("the message should be received within 5 seconds, actual: " + (addMessage.get._1 - time))
      And("the librarycontent should have increased by one")
      assert((client.getContent(testLib).libContent.size - contentSize) === 1)
    }

  }


}
