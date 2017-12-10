package org.home.streamsexamples

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Broadcast, Sink, Source}
import akka.{Done, NotUsed}
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

object SinkExamples extends App with ScalaFutures with Matchers {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  {

    val sinkOne: Sink[Int, Future[Done]] = Sink.foreach[Int](i ⇒ println(s"Sink 1 $i"))
    val sinkTwo: Sink[Int, Future[Done]] = Sink.foreach[Int](i ⇒ println(s"Sink 2 $i"))
    val combined: Sink[Int, NotUsed] = Sink.combine(sinkOne, sinkTwo)(Broadcast[Int](_))

    Source(1 to 3).runWith(combined)

  }

  system.terminate().foreach { _ ⇒
    println("Terminated")
  }
}
