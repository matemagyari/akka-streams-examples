package org.home.streamsexamples

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import org.home.streamsexamples.TestFixture.rememberingSink
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures

import scala.collection.immutable.Seq

object StatefulExamples extends App with ScalaFutures with Matchers {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  //statefulMapConcat
  {

    val flow: Flow[Int, Int, NotUsed] = Flow[Int].statefulMapConcat { () ⇒
      var accummulator: Int = 0

      { input: Int ⇒
        accummulator += input
        Seq(accummulator)
      }
    }

    val result = Source(1 to 5).via(flow).runWith(rememberingSink()).futureValue
    result shouldBe Seq(1, 3, 6, 10, 15)

  }

  system.terminate().foreach { _ ⇒
    println("Terminated")
  }
}
