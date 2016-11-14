package app

import java.io.File
import java.net.URI
import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import api.AppConnector
import core.Semba
import core.ontology.Initialize
import sembaGRPC.VoidResult

import scala.collection.immutable.HashMap.HashMap1
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
object Application extends App {

  var libraries: mutable.HashMap[String, ActorRef] = new mutable.HashMap[String, ActorRef]()
  val sessions: mutable.HashMap[String, ArrayBuffer[UUID]] = new mutable.HashMap[String, ArrayBuffer[UUID]]()

  val system = ActorSystem("Semba")
  val api = system.actorOf(Props[AppConnector])
  //loadLibrary(new URI("file:///users/uni/documents/semba3/appdata/libraries/semba-teaching.owl"),UUID.randomUUID())



/**
  * Startup of gRPC services from API package
**/




/**
  * Methods
**/

  def loadLibrary(path : URI, session: UUID): ActorRef = {
  val src = path.toString
  if( !libraries.contains( src ))
  {
    val backend = system.actorOf(Props(new Semba(path)) )
    libraries.put(src , backend)
  }
  if(!sessions.contains(src)) sessions.put(src,  ArrayBuffer[UUID](session))
  else sessions.apply(src).+=(session)
  libraries.apply(path.toString)

}
  def closeLibrary(path : URI, session: UUID): VoidResult = {
    val src = path.toString
    sessions.apply(src)-=session
    if(sessions.apply(src).isEmpty){
      libraries.apply(src) ! PoisonPill
      libraries.remove(src)
    }
    VoidResult(true, "Library Closed")
  }


  def get(uri: String): ActorRef = libraries.apply(uri)

}
