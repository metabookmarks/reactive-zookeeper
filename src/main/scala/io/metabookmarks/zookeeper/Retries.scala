package io.metabookmarks.zookeeper

import ZkLogger._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

import async.UnretriableException

trait Retry {
  def apply[T](operation: => Future[T]): Future[T]
}

object RetriesPolicies {
  object OneTry extends Retry {
    def apply[T](operation: => Future[T]): Future[T] = operation
  }

  case class NTry(n: Int=10) extends Retry {
    def apply[T](operation: => Future[T]): Future[T] = {
      def retry(n: Int): Future[T] =
        operation.recoverWith {
          case e : UnretriableException =>
            Future.failed(e)
          case e if (n > 0) =>
            info(s"retry $n $e")
            retry(n - 1)
        }
      retry(n)
    }
  }
}

