package app

/**
  * Author: Eike Isermann
  * This is a SeMBa3 class
  */
  import akka.actor.{Actor, ActorSystem, Props}

  object Test extends App {

    case class AM()
    case class BM()
    class Test extends Actor with MyTrait {
      override def receive: Receive = {
        case _ => println("Test")
      }
    }

    trait MyTrait { this: Test =>
      override def receive: this.Receive = {
        case a: AM => {
          println("A")
          this.receive(a)
        }
        case x => this.receive(x)
      }

    }
    val system = ActorSystem("Test")
    val myActor = system.actorOf(Props(new Test))

    myActor ! AM
    myActor ! BM

  }
