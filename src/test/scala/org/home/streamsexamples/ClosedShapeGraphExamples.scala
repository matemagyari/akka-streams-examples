package org.home.streamsexamples

import java.util.concurrent.{CountDownLatch, TimeUnit}

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, ThrottleMode, UniformFanOutShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}
import org.home.streamsexamples.TestFixture.rememberingSink
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future

object ClosedShapeGraphExamples extends App with ScalaFutures with Matchers {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  {

    val countDownLatch = new CountDownLatch(6)
    val evens = mutable.ListBuffer.empty[Int]
    val odds = mutable.ListBuffer.empty[Int]
    val evenSink = Sink.foreach[Int] { i ⇒
      evens += i
      countDownLatch.countDown()
    }
    val oddSink = Sink.foreach[Int] { i ⇒
      odds += i
      countDownLatch.countDown()
    }
    val source = Source(1 to 6)

    val graph: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val bcast: UniformFanOutShape[Int, Int] = b.add(Broadcast[Int](2))

      source ~> bcast.in
      bcast.out(0) ~> Flow[Int].filter(_ % 2 == 0) ~> evenSink
      bcast.out(1) ~> Flow[Int].filter(_ % 2 == 1) ~> oddSink
      ClosedShape
    })

    graph.run()

    countDownLatch.await(10, TimeUnit.MILLISECONDS)

    odds.toList shouldBe Seq(1, 3, 5)
    evens.toList shouldBe Seq(2, 4, 6)
  }

  system.terminate().foreach { _ ⇒
    println("Terminated")
  }

  private def withFlow[T](flow: Flow[Int, T, NotUsed]): Seq[T] =
    Source(1 to 10)
      .via(flow)
      .runWith(rememberingSink())
      .futureValue
}
