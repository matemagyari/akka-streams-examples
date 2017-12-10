package org.home.streamsexamples

import java.util.concurrent.{CountDownLatch, TimeUnit}

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source}
import akka.{Done, NotUsed}
import org.home.streamsexamples.TestFixture._
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._

object FlowExamples extends App with ScalaFutures with Matchers {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  //only blueprint, can be materialized multiple times
  {
    val results = mutable.ListBuffer.empty[Int]
    val latch1 = new CountDownLatch(3)
    val latch2 = new CountDownLatch(3)

    val sink: Sink[Int, Future[Done]] = Sink.foreach[Int] { i ⇒
      results += i
      if (results.size < 4) {
        latch1.countDown()
      }
      else {
        latch2.countDown()
      }
    }

    val graph: RunnableGraph[NotUsed] = Source(1 to 3).to(sink)

    graph.run()
    latch1.await(10, TimeUnit.MILLISECONDS)
    results.toList shouldBe Seq(1, 2, 3)

    graph.run()
    latch2.await(10, TimeUnit.MILLISECONDS)
    results.toList shouldBe Seq(1, 2, 3, 1, 2, 3)

  }

  {

    val flow = Flow[Int]
      .filter(_ % 2 == 0)
      .map(_ + 1)
      .collect { case x if x % 3 == 0 ⇒ x }
      .mapConcat(i ⇒ Seq(i))

    Source(1 to 10)
      .via(flow)
      .to(rememberingSink())
      .run()
  }

  //mapAsync
  {
    def double(i: Int) = Future { i * 2 }
    withFlow(Flow[Int].mapAsync(4)(double)) shouldBe (1 to 10).map(_ * 2)
  }

  //grouped
  {
    withFlow(Flow[Int].grouped(4)) shouldBe Seq(Seq(1, 2, 3, 4), Seq(5, 6, 7, 8), Seq(9, 10))
  }

  //take
  {
    withFlow(Flow[Int].take(3)) shouldBe (1 to 3)
  }

  //takeWhile
  {
    withFlow(Flow[Int].takeWhile(_ < 6)) shouldBe (1 to 5)
    withFlow(Flow[Int].takeWhile(_ < 6, inclusive = true)) shouldBe (1 to 6)

  }

  //takeWithin and throttle
  {
    val result = Source(1 to 10)
      .throttle(1, 10 millisecond, 1, ThrottleMode.shaping) //1 element per 10 ms
      .takeWithin(25 milliseconds)
      .runWith(rememberingSink())
      .futureValue

    result shouldBe (1 to 3)
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
