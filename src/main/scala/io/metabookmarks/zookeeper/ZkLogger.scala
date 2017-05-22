package io.metabookmarks.zookeeper

import org.slf4j.LoggerFactory



object ZkLogger {

  private val log = LoggerFactory.getLogger("io.metabookmarks.zookeeper")

  def info(msg: String): Unit = log.info(msg)
  def debug(msg: String): Unit = log.debug(msg)

}
