package io.metabookmarks.zookeeper

/**
slsl
  */
import java.util.Arrays

import org.apache.zookeeper.AsyncCallback.StatCallback

import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.KeeperException.Code
import org.apache.zookeeper.WatchedEvent
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.Watcher.Event
import org.apache.zookeeper.Watcher.Event.EventType
import org.apache.zookeeper.Watcher.Event.KeeperState
import org.apache.zookeeper.ZooKeeper
import org.apache.zookeeper.data.Stat
import org.slf4j.LoggerFactory

sealed trait NodeEvent[A]
case class Exist[A](node: A) extends NodeEvent[A]
case class Updated[A](oldNode: A, newNode: A) extends NodeEvent[A]
case class Deleted[A](node: A) extends NodeEvent[A]

object Log {
  private val log = LoggerFactory.getLogger("io.metabookmarks.zookeeper")
  def debug(msg: String): Unit = log.debug(msg)
  def info(msg: String): Unit = log.info(msg)
  def warn(msg: String): Unit = log.warn(msg)
  def error(msg: String): Unit = log.error(msg)
}

trait DataMonitorListener {
  def deleted(data: Array[Byte]): Unit
  def exist(data: Array[Byte]): Unit
  def updated(oldData: Array[Byte], newData: Array[Byte]): Unit

  def closing(rc: Int): Unit

}
class DataMonitor(zk: ZooKeeper, node: String, chainedWatcher: Option[Watcher], listener: DataMonitorListener) extends Watcher
    with StatCallback {

  zk.exists(node, true, this, None);
  @volatile var dead = false

  var prevData: Option[Array[Byte]] = None

  def process(event: WatchedEvent): Unit = {
    val path = Option(event.getPath)
    event.getType match {
      case Event.EventType.None =>
        // We are are being told that the state of the
        // connection has changed
        event.getState match {
          case Event.KeeperState.SyncConnected =>
          // In this particular example we don't need to do anything
          // here - watches are automatically re-registered with
          // server and any watches triggered while the client was
          // disconnected will be delivered (in order of course)
          case Event.KeeperState.Expired =>
            dead = true
            listener.closing(KeeperException.Code.SESSIONEXPIRED.intValue())

          case e: KeeperState =>
            Log.info(e.name() + " occured")

        }
      case Event.EventType.NodeDeleted =>
        Log.debug("Node deleted");
        dead = true
        listener.closing(KeeperException.Code.SESSIONEXPIRED.intValue())

      case e: EventType =>
        path.filter(_ == node).foreach {
          x =>
            zk.exists(node, true, this, None)
        }
    }

    chainedWatcher.foreach(_.process(event))
  }
  def processResult(rc: Int, path: String, ctx: Any, stat: Stat): Unit = {
    prevData = (Code.get(rc) match {
      case Code.OK => Option(zk.getData(node, false, stat))
      case Code.NONODE => None
      case Code.SESSIONEXPIRED | Code.NOAUTH =>
        dead = true
        listener.closing(rc)
        None
      case _ => None
    }).map {
      data =>
        prevData match {
          case None =>
            listener.exist(data)
          case Some(oldData) =>
            if (!Arrays.equals(oldData, data)) listener.updated(oldData, data)
        }
        data
    }.orElse {
      prevData.foreach(oldData => listener.deleted(oldData))
      None
    }

  }
}

object NodeWatcher {

  val zoo = 2181

  def apply[A](topic: String, hostname: String = "localhost", port: Int = zoo)(f: NodeEvent[A] => Unit)(implicit a: Array[Byte] => Either[_, A]): NodeWatcher[A] =
    new NodeWatcher(topic, hostname, port, f, a)

}

class NodeWatcher[A](node: String, hostname: String, port: Int, apply: NodeEvent[A] => Unit, map: Array[Byte] => Either[_, A]) extends Thread with Watcher {
  lazy val zk = new ZooKeeper(hostname, port, this)
  lazy val dm = new DataMonitor(zk, node, None, listener)

  val listener = new DataMonitorListener {

    def exist(data: Array[Byte]) = {
      map(data) match {
        case Right(node) =>
          apply(Exist(node))
        case Left(e) => Log.error("could not build node object")
      }
    }
    def updated(oldData: Array[Byte], newData: Array[Byte]) = {
      (map(oldData), map(newData)) match {
        case (Right(oldNode), Right(newNode)) =>
          apply(Updated(oldNode, newNode))
        case _ => Log.error("could not build nodes object")
      }
    }
    def deleted(data: Array[Byte]): Unit = {
      map(data) match {
        case Right(node) =>
          apply(Deleted(node))
        case Left(e) => Log.error("could not build node object")
      }
    }

    def closing(rc: Int): Unit = {
      NodeWatcher.this.closing(rc)
    }
  }

  override def run(): Unit = synchronized {
    while (!dm.dead) {
      wait();
    }
  }

  def process(event: WatchedEvent): Unit = dm.process(event)

  def closing(rc: Int): Unit = synchronized {
    println("closing")
    notifyAll();
    zk.close()
  }

}
