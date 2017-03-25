package logic.dataExport

import akka.actor.Actor
import logic.core.{AccessToStorage, ActorFeatures}

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
trait DataExport extends Actor with ActorFeatures with AccessToStorage {

}
