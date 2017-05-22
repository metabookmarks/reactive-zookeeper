package io.metabookmarks.zookeeper



import io.metabookmarks.zookeeper.ZkLogger._
import io.metabookmarks.zookeeper.async._
import org.apache.zookeeper.KeeperException.ConnectionLossException
import org.apache.zookeeper.Watcher.Event.KeeperState
import org.apache.zookeeper.ZooDefs.Ids.OPEN_ACL_UNSAFE
import org.apache.zookeeper.data.Stat
import org.apache.zookeeper.{CreateMode, WatchedEvent, Watcher, ZooKeeper}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

object ZkConnection {
  def apply(zkConnect: String, retry: Retry = RetriesPolicies.NTry()): Future[ZkConnection] = {
    val p = Promise[ZkConnection]
    val con = new ZkConnection(zkConnect, retry)(p)
    p.future
  }
}

class ZkConnection(zkConnect: String, retry: Retry = RetriesPolicies.NTry())(p: Promise[ZkConnection]) {

  class ZkConn extends Watcher {

    private[this] val zooKeeper = new ZooKeeper(zkConnect, 1000, this)

    var connected = false

    def process(event: WatchedEvent): Unit = {
      info(event.toString())
      event.getState match {
        case state @ KeeperState.SyncConnected =>
          info(s"Connected to $zkConnect")
          if (!p.isCompleted)
            p.success(ZkConnection.this)
          connected = true;
        case state @ KeeperState.Disconnected =>
          info(s"Disconnected from $zkConnect")
          connected = true
        case z => info(z.toString())
      }
    }

    def conn(): Future[ZooKeeper] =
      if (connected) Future.successful(zooKeeper)
      else Future.failed(new ConnectionLossException())
  }

  val connectionWatcher = new ZkConn()

  def apply(): Future[ZooKeeper] = connectionWatcher.conn()

  def retrying[T](op: ZooKeeper => Future[T]): Future[T] = retry { apply() flatMap (op) }

  def exists(path: String): Future[Stat] = retrying {
    zk =>
      val p = new AsyncStatCallbackPromise()
      zk.exists(path, true, p, null)
      p.future
  }

  def create(path: String, data: String): Future[ZNode] =
    create(path, Option(data.getBytes("UTF-8")))


  def create(path: String, data: Array[Byte] = null): Future[ZNode] =
    create(path, Option(data))

  def create(path: String, data: Option[Array[Byte]]): Future[ZNode] = retrying {
    zk =>
      val p = new AsyncZNodeCallbackPromise(data)
      val acl = OPEN_ACL_UNSAFE.asScala
      zk.create(path, data.getOrElse(null), acl.asJava, CreateMode.PERSISTENT, p, null)
      p.future
  }

  def get(path: String): Future[ZNode] = retrying { zk =>
    val p = new AsyncZNodeCallbackPromise()
    zk.getData(path, true, p, null)
    p.future
  }

  def data(path: String): Future[Array[Byte]] = retrying {
    zk =>
      val p = new AsyncDataCallbackPromise()
      zk.getData(path, true, p, null)
      p.future
  }

  def delete(path: String, version: Int = 0): Future[Unit] = retrying {
    zk =>
      val p = new AsyncUnitCallbackPromise()
      zk.delete(path, version, p, null)
      p.future
  }

  def children(path: String): Future[Seq[String]] = retrying {
    zk =>
      val p = new AsyncChildrenCallbackPromise()
      zk.getChildren(path, true, p, null)
      p.future
  }

}
