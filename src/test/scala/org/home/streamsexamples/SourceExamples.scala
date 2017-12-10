package org.home.streamsexamples

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Merge, Sink, Source}
import org.home.streamsexamples.TestFixture._
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures

import scala.collection.immutable.Seq
import scala.concurrent.duration._

object SourceExamples extends App with ScalaFutures with Matchers {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  //Source.zipN
  {
    val source: Source[Seq[Int], NotUsed] = Source.zipN(
      Seq(
        Source(1 to 3),
        Source(4 to 6)
      ))

    source.runWith(rememberingSink()).futureValue shouldBe Seq(
      Vector(1, 4),
      Vector(2, 5),
      Vector(3, 6))
  }

  //Source.zipWithN

  {
    def zipper(xs: Seq[Int]) = xs.mkString("-")

    val source: Source[String, NotUsed] = Source.zipWithN(zipper)(
      Seq(
        Source(1 to 3),
        Source(4 to 6)
      ))

    source.runWith(rememberingSink()).futureValue shouldBe Seq("1-4", "2-5", "3-6")
  }

  //Source.combine
  {
    val s1: Source[Int, NotUsed] = Source(1 to 3)
    val s2: Source[Int, NotUsed] = Source(1 to 3)

    val merged: Source[Int, NotUsed] = Source.combine(s1, s2)(Merge(_))

    val mergedResult: Int = merged.runWith(Sink.fold(0)(_ + _)).futureValue

    mergedResult shouldBe 12
  }

  //tick
  {
    val result = Source
      .tick(initialDelay = 0 millisecond, interval = 3 milliseconds, tick = 1)
      .takeWithin(10 milliseconds)
      .runWith(rememberingSink())
      .futureValue

    //result shouldBe Seq(1, 1, 1)
  }

  system.terminate().foreach { _ ⇒
    println("Terminated")
  }
}
