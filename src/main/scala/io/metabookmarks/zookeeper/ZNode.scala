package io.metabookmarks.zookeeper

import scala.concurrent.Future


case class ZNode(path: String, data: Option[Array[Byte]]) {

  def createChild(name: String, data: String)(implicit conn: ZkConnection) : Future[ZNode] =
    conn.create(s"$path/$name", Option(data.getBytes("UTF-8")))

  def createChild(name: String, data: Array[Byte]=null)(implicit conn: ZkConnection) : Future[ZNode] =
    conn.create(s"$path/$name", Option(data))

  def childrenNames(implicit conn: ZkConnection) : Future[Seq[String]] =
    conn.children(path)
}
