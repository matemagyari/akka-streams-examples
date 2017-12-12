package org.home.streamsexamples

import java.util.concurrent.{CountDownLatch, TimeUnit}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, UniformFanOutShape}
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

    countDownLatch.await(100, TimeUnit.MILLISECONDS)

    odds.toList shouldBe Seq(1, 3, 5)
    evens.toList shouldBe Seq(2, 4, 6)
  }

  {
    val countDownLatch = new CountDownLatch(6)
    val source = Source(1 to 3)

    val graph: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(GraphDSL.create() { implicit b ⇒
      import GraphDSL.Implicits._

      val bcast = b.add(Broadcast[Int](2))
      val merge = b.add(Merge[Int](2))

      val sink = Sink.foreach[Int] { _ ⇒
        countDownLatch.countDown()
      }

      source ~> bcast.in

      bcast.out(0) ~> Flow[Int].map(_ + 1) ~> merge
      bcast.out(1) ~> Flow[Int].map(_ + 2) ~> merge

      merge.out ~> sink

      ClosedShape
    })

    graph.run()

    countDownLatch.await(100, TimeUnit.MILLISECONDS)
  }

  {
    val sink1 = Sink.head[Int]
    val sink2 = Sink.head[Int]

    val graph: RunnableGraph[(Future[Int], Future[Int])] =
      RunnableGraph.fromGraph(GraphDSL.create(sink1, sink2)((_, _)) { implicit b ⇒ (s1, s2) ⇒
        import GraphDSL.Implicits._

        val bcast = b.add(Broadcast[Int](2))

        Source.single(1) ~> bcast.in

        bcast.out(0) ~> s1.in
        bcast.out(1) ~> s2.in

        ClosedShape
      })

    val results: (Future[Int], Future[Int]) = graph.run()
    results._1.futureValue shouldBe 1
    results._2.futureValue shouldBe 1

  }

  system.terminate().foreach { _ ⇒
    println("Terminated")
  }
}
