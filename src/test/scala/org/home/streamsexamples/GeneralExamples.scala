package org.home.streamsexamples

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

object GeneralExamples extends App with ScalaFutures with Matchers {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  //both source and sink can be reused and materialized multiple times
  {
    val source = Source(1 to 10)
    val sink: Sink[Int, Future[Int]] = Sink.fold(0)(_ + _)

    val runnableGraph1: RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)
    runnableGraph1.run().futureValue shouldBe 55

    val runnableGraph2: RunnableGraph[NotUsed] = source.to(sink)
    runnableGraph2.run() shouldBe NotUsed

    val eventualInt: Future[Int] = source.runWith(sink)
    eventualInt.futureValue shouldBe 55
  }

  system.terminate().foreach { _ ⇒
    println("Terminated")
  }

}
