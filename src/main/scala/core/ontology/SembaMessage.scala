package core.ontology

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
sealed trait SembaMessage
case object Initialize extends SembaMessage
case object GiveNetwork extends SembaMessage
