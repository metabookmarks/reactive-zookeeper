package io.metabookmarks.zookeeper.async

import io.metabookmarks.zookeeper.ZNode

import scala.collection.JavaConverters._
import org.apache.zookeeper.AsyncCallback.{ChildrenCallback, DataCallback, StatCallback, StringCallback, VoidCallback}
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.data.Stat

import scala.concurrent.Promise
import scala.util.Try



class UnretriableException(msg: String) extends RuntimeException(msg)

protected abstract class AsyncCallbackPromise[T] extends Promise[T] {
  protected val p = Promise[T]()

  def future = p.future

  def isCompleted = p.isCompleted

  def tryComplete(res: Try[T]): Boolean = p.tryComplete(res)

  protected def checkError(rc: Int, path: String)(res: => Unit): Unit =
    KeeperException.Code.get(rc) match {
      case KeeperException.Code.OK => res
      case nok =>
        p.failure(new UnretriableException(s"$nok"))

    }

}

class AsyncZNodeCallbackPromise(data: Option[Array[Byte]] = None) extends AsyncCallbackPromise[ZNode] with StringCallback with DataCallback {
  def processResult(rc: Int, path: String, ctx: AnyRef, name: String): Unit =
    checkError(rc, path) {
      p.success(ZNode(path, data))
    }

  def processResult(rc: Int, path: String, ctx: AnyRef, t: Array[Byte], stat: Stat): Unit = checkError(rc, path) {
    p.success(ZNode(path, Option(t)))
  }
}

class AsyncStringCallbackPromise extends AsyncCallbackPromise[String] with StringCallback {
  def processResult(rc: Int, path: String, ctx: AnyRef, name: String): Unit =
    checkError(rc, path) { p.success(name) }
}

class AsyncStatCallbackPromise extends AsyncCallbackPromise[Stat] with StatCallback {

  def processResult(rc: Int, path: String, ctx: AnyRef, stat: Stat): Unit =
    checkError(rc, path) {
      p.success(stat)
    }

}

class AsyncDataCallbackPromise extends AsyncCallbackPromise[Array[Byte]] with DataCallback {
  def processResult(rc: Int, path: String, ctx: AnyRef, t: Array[Byte], stat: Stat): Unit = checkError(rc, path) {
    p.success(t)
  }
}

class AsyncChildrenCallbackPromise extends AsyncCallbackPromise[List[String]] with ChildrenCallback {
  def processResult(rc: Int, path: String, ctx: AnyRef, names: java.util.List[String]): Unit =
    checkError(rc, path) {
      p.success(names.asScala.toList)
    }
}

class AsyncUnitCallbackPromise extends AsyncCallbackPromise[Unit] with VoidCallback {
  def processResult(rc: Int, path: String, ctx: AnyRef): Unit = checkError(rc, path) {
    p.success(())
  }
}
