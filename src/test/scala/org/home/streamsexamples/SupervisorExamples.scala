package org.home.streamsexamples

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.typesafe.scalalogging.StrictLogging
import org.home.streamsexamples.TestFixture._
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.{Failure, Success}

object SupervisorExamples extends App with ScalaFutures with Matchers {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializerFactory.createMaterializer(system)
  import system.dispatcher

  {
    def runWith(m: ActorMaterializer): Future[Seq[Int]] =
      Source(Seq(-1, 0, 1)).map(2 / _).runWith(rememberingSink())(m)

    //should simply skip problematic element
    runWith(ActorMaterializerFactory.createMaterializer(system)).futureValue shouldBe Seq(-2, 2)

    //should fail with default materializer
    runWith(ActorMaterializer()).onComplete {
      case Success(_) ⇒ fail("Should not happen")
      case Failure(t) ⇒ //ok
    }
  }

  system.terminate().foreach { _ ⇒
    println("Terminated")
  }

}

object ActorMaterializerFactory extends StrictLogging {

  private val decider: Supervision.Decider = {
    case e: Exception ⇒
      logger.error("Exception, resume", e)
      Supervision.Resume
    case t ⇒
      logger.error("Stopping after error", t)
      Supervision.Stop
  }

  def createMaterializer(system: ActorSystem) =
    ActorMaterializer(ActorMaterializerSettings(system).withSupervisionStrategy(decider))(system)
}
