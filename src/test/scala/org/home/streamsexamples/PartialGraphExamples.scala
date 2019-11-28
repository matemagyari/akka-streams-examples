package org.home.streamsexamples

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, Zip, ZipWith}
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

object PartialGraphExamples extends App with ScalaFutures with Matchers {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  {
    //3 input and 1 output connector open
    val pickMaxOf3: Graph[UniformFanInShape[Int, Int], NotUsed] = GraphDSL.create() { implicit b ⇒
      import GraphDSL.Implicits._

      val zipWith = ZipWith[Int, Int, Int](math.max _)
      val zip1 = b.add(zipWith)
      val zip2 = b.add(zipWith)

      zip1.out ~> zip2.in0

      UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
    }

    val resultSink = Sink.head[Int]

    val graph: Graph[ClosedShape.type, Future[Int]] = GraphDSL.create(resultSink) {
      implicit b ⇒ sink ⇒
        import GraphDSL.Implicits._

        // importing the partial graph will return its shape (inlets & outlets)
        val pm3: UniformFanInShape[Int, Int] = b.add(pickMaxOf3)

        Source.single(1) ~> pm3.in(0)
        Source.single(2) ~> pm3.in(1)
        Source.single(3) ~> pm3.in(2)

        pm3.out ~> sink.in
        ClosedShape
    }
    val runnableGraph: RunnableGraph[Future[Int]] = RunnableGraph.fromGraph(graph)

    runnableGraph.run().futureValue shouldBe 3
  }

  {
    //create a Flow from a partial graph

    val flow: Flow[Int, (Int, String), NotUsed] = Flow.fromGraph(GraphDSL.create() { implicit b ⇒
      import GraphDSL.Implicits._

      val broadcast = b.add(Broadcast[Int](2))
      val zip = b.add(Zip[Int, String]())

      broadcast.out(0) ~> zip.in0
      broadcast.out(1).map(_.toString) ~> zip.in1

      FlowShape(broadcast.in, zip.out)
    })

    Source.single(0).via(flow).runWith(Sink.head).futureValue shouldBe (0, "0")

  }

  system.terminate().foreach { _ ⇒
    println("Terminated")
  }
}
