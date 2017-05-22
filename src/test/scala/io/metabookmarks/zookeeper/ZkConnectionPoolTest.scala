package io.metabookmarks.zookeeper

import org.scalatest.Matchers
import org.scalatest.WordSpec
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import scala.concurrent.ExecutionContext.Implicits.global

class ZkConnectionPoolTest extends WordSpec with Matchers {

  "Compagnion object" should {
    "build connected pool" in {

      val oo = ZkConnection("localhost:2181").flatMap {
        implicit con =>
          for {

            znode <- con.create("/test", """{"name": "zozo"} """).recoverWith {
              case e => con.get("/test")
            }

            stat <- con.exists("/test")
            data <- con.data("/test")
            lucas <- znode.createChild("lucas", "data").recoverWith {
              case e => con.get("/test/lucas")
            }
            bebe <- znode.childrenNames
            // del <- con.delete("/test")
          } yield {
            println(bebe)
          }
      }

      Await.ready(oo, 3 seconds)

    }
  }

}
