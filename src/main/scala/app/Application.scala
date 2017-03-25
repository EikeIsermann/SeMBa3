package app

import java.net.URI
import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import api.AppConnector
import logic.core.Semba
import logic.resourceCreation.metadata.ThumbActor
import org.apache.jena.util.FileManager
import sembaGRPC.VoidResult
import utilities.FileFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


/** Entry point of the Application. Provides global access to sessions and loaded libraries.
  *
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object Application extends App {

  /** Maps a SeMBA [[java.net.URI]] to all Session UUIDs that observe it. */
  val sessions: mutable.HashMap[String, ArrayBuffer[UUID]] = new mutable.HashMap[String, ArrayBuffer[UUID]]()

  /** Main ActorSystem */
  val system = ActorSystem("Semba")

  /** Global reference to AppConnector for sending updates */
  val api = system.actorOf(Props[AppConnector], "apiActor")

  /** Global reference to all opened SemBA library instances*/
  var libraries: mutable.HashMap[String, ActorRef] = new mutable.HashMap[String, ActorRef]()
  //loadLibrary(new URI("file:///users/uni/documents/semba3/appdata/libraries/semba-teaching.owl"),UUID.randomUUID())

  ThumbActor.initialize(system)

  /**
    * Methods
    **/

  /** Initializes a new [[Semba]] Actor if a library is not loaded yet or returns the its [[ActorRef]]
    *
    * @param path URI to the library ontology
    * @param session Client session that requests the library
    * @return Reference to the [[Semba]] representing the lib ontology
    */
  def loadLibrary(path: String, session: UUID): ActorRef = {
    if (!libraries.contains(path)) {
      val backend = system.actorOf(Props(new Semba(path)))
      libraries.put(path, backend)
    }
    if (!sessions.contains(path)) sessions.put(path, ArrayBuffer[UUID](session))
    else sessions.apply(path).+=(session)
    libraries.apply(path)
  }

  /** Removes the sessionID from [[Application.sessions]] and terminates the [[Semba]] if no other sessions are
    * registered.
    *
    * @param path URI to the library ontology
    * @param session Client session that closes the library
    * @return True if library was closed, false if it is still loaded but unsubscribed.
    */
  def closeLibrary(path: String, session: UUID): VoidResult = {
    sessions.apply(path) -= session
    if (sessions.apply(path).isEmpty) {
      libraries.apply(path) ! PoisonPill
      libraries.remove(path)
    }
    VoidResult(true, "Library Closed")
  }

  def closeConnection(session:UUID) = {
    sessions.foreach(entry => if(entry._2.contains(session)) entry._2 -= session)
  }

  /**
    * Returns the ActorRef for a requested library URI
    * @param uri
    * @return
    */
  def get(uri: String): ActorRef = libraries.apply(FileFactory.getURI(uri))


  def ping(actor: ActorRef) = {
    println( actor == api)
    actor ! "Pong"
}
}
