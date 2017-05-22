package io.metabookmarks.zookeeper

import org.scalatest.WordSpec

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.semiauto._


/**
  * Created by olivier on 23/06/16.
  */
case class Test(name: String)

class ZkWatcherTest extends WordSpec {

  implicit val dec = deriveDecoder[Test]

  implicit val conv : Array[Byte] => Either[_,Test] = ab =>
    decode(new String(ab, "UTF-8"))

  "ZkWatcher" should {
    "find existing node" in {
      val nodeWatcher = NodeWatcher[Test]("/test") {
        ooo =>
        println(ooo)
      }

      nodeWatcher.start()

     

    }
  }

}
