package org.home.streamsexamples

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.immutable.Seq
import scala.collection.mutable

class MergeHubTest extends FlatSpec with ScalaFutures with Matchers {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  //[[Source]] can be materialized an arbitrary number
  //of times, where each of the new materializations will receive their elements from the original [[Sink]]
  "MergeHub" should "pipe a sink into a source" in {

    //both source and sink can be reused and materialized multiple times
    val (sinkForInput: Sink[Int, NotUsed], sourceForDownstream: Source[Int, NotUsed]) =
      MergeHub
        .source[Int]
        .toMat(BroadcastHub.sink(256))(Keep.both)
        .run()

    val results = mutable.ListBuffer.empty[Int]
    val results2 = mutable.ListBuffer.empty[Int]

    //materialize sourceForDownstream first time
    sourceForDownstream.runWith(Sink.foreach { x ⇒
      results += x
    })

    //materialize sourceForDownstream second time
    sourceForDownstream.runWith(Sink.foreach { x ⇒
      results2 += x
    })

    //pushing elements to [[sinkForInput]] will send them to [[sourceForDownstream]]
    Source(1 to 3).to(sinkForInput).run()

    Thread.sleep(10)
    results.toList shouldBe Seq(1, 2, 3)
    results2.toList shouldBe Seq(1, 2, 3)

    //do it again
    Source(4 to 5).to(sinkForInput).run()

    Thread.sleep(10)
    results.toList shouldBe Seq(1, 2, 3, 4, 5)
    results2.toList shouldBe Seq(1, 2, 3, 4, 5)
  }

}
