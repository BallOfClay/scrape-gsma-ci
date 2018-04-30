import org.jsoup.Jsoup
import org.jsoup.select.Elements

import scala.util.matching.Regex
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import doobie._
import doobie.implicits._
import cats._
import cats.effect._
import cats.implicits._

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import shapeless._



trait sqlCon {
  val conf: Config  = ConfigFactory.load("application.conf")

  val xa = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver", // driver classname
    url = s"${conf.getString("url")}", // connect URL (driver-specific)
    user = s"${conf.getString("username")}",              // user
    pass = s"${conf.getString("password")}"                      // password
  )
}

class oemTable extends sqlCon {

  case class oemResource(name: String, count: Int, location: String)

  private val source : Elements  = {
    val sesh = Jsoup.connect("https://www.gsmarena.com/makers.php3").get()
    sesh.select(".st-text a")
  }


  def getURLs : Array[String]  = {
    source.eachAttr("href").asScala.toArray
  }

  def getCounts: Array[String] = {
    val rex : Regex = """(?i)((?<=\s)\d+(?= device(s)))""".r
    source.eachText() flatMap (x => rex findFirstIn(x)) toArray
  }

  def getOEMs: Array[String] = {
    val rex : Regex = """(?i)(.*(?= \d+ devices))""".r
    source.eachText flatMap (x => rex findFirstIn(x)) toArray
  }



  val tmp  = {
    val a = sql"select * from gsm_resources".query[oemResource]
    a.to[List].transact(xa).unsafeRunSync()
  }



}

object walhalla {
  def main(args: Array[String]): Unit = {


    val oem_table = new oemTable

    val oem_names = oem_table.tmp

    oem_names foreach(println)

    //println(oem_names)

  }
}
