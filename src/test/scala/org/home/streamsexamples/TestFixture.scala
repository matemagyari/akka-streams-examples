package org.home.streamsexamples

import akka.stream.scaladsl.Sink

import scala.collection.immutable.Seq
import scala.concurrent.Future

object TestFixture {

  def rememberingSink[T](): Sink[T, Future[Seq[T]]] =
    Sink.fold(Seq.empty[T]) { (acc, x) ⇒
      acc :+ x
    }
}
