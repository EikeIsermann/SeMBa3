package logic.resourceCreation

import java.io.File
import java.net.URI
import java.nio.file.{CopyOption, Files, Path, StandardCopyOption}

import akka.actor.{Actor, ActorRef, Props}
import logic.core._
import logic.core.jobHandling._
import utilities.WriterFactory

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
case class MoveFile(src: String, dest: String, deleteOriginal: Boolean = false) extends Job
class FileMover(val config: Config) extends LibJobExecutor {

  override def performTask(job: Job): JobResult = {
    job match {
      case move: MoveFile => JobResult(moveFile(move))
      case _ => JobResult(ErrorResult())
    }
  }
  def moveFile(move: MoveFile): ResultContent = {
    val src: Path = new File(URI.create(move.src)).toPath
    val dest: Path = new File(URI.create(move.dest)).toPath
    val folder = dest.getParent
    if(!Files.exists(folder)) Files.createDirectories(dest.getParent)
    if(move.deleteOriginal) Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING  )
    else Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING)
    EmptyResult()
  }

}

object FileMover {
  def props(config: Config) = Props(new FileMover(config))
}
