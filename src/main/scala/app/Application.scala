package app

import java.net.URI
import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import api.AppConnector
import core.Semba
import sembaGRPC.VoidResult

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
  val api = system.actorOf(Props[AppConnector])

  /** Global reference to all opened SemBA library instances*/
  var libraries: mutable.HashMap[String, ActorRef] = new mutable.HashMap[String, ActorRef]()
  //loadLibrary(new URI("file:///users/uni/documents/semba3/appdata/libraries/semba-teaching.owl"),UUID.randomUUID())



  /**
    * Methods
    **/

  /** Initializes a new [[core.Semba]] Actor if a library is not loaded yet or returns the its [[ActorRef]]
    *
    * @param path URI to the library ontology
    * @param session Client session that requests the library
    * @return Reference to the [[core.Semba]] representing the lib ontology
    */
  def loadLibrary(path: URI, session: UUID): ActorRef = {
    val src = path.toString
    if (!libraries.contains(src)) {
      val backend = system.actorOf(Props(new Semba(path)))
      libraries.put(src, backend)
    }
    if (!sessions.contains(src)) sessions.put(src, ArrayBuffer[UUID](session))
    else sessions.apply(src).+=(session)
    libraries.apply(path.toString)
  }

  /** Removes the sessionID from [[Application.sessions]] and terminates the [[core.Semba]] if no other sessions are
    * registered.
    *
    * @param path URI to the library ontology
    * @param session Client session that closes the library
    * @return True if library was closed, false if it is still loaded but unsubscribed.
    */
  def closeLibrary(path: URI, session: UUID): VoidResult = {
    val src = path.toString
    sessions.apply(src) -= session
    if (sessions.apply(src).isEmpty) {
      libraries.apply(src) ! PoisonPill
      libraries.remove(src)
    }
    VoidResult(true, "Library Closed")
  }

  /**
    * Returns the ActorRef for a requested library URI
    * @param uri
    * @return
    */
  def get(uri: String): ActorRef = libraries.apply(uri)

}
