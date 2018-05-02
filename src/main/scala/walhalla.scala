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


  def getURLs : List[String]  = {
    source.eachAttr("href").asScala.toList
  }

  def getCounts: List[String] = {
    val rex : Regex = """(?i)((?<=\s)\d+(?= device(s)))""".r
    source.eachText() flatMap (x => rex findFirstIn(x)) toList
  }

  def getOEMs: List[String] = {
    val rex : Regex = """(?i)(.*(?= \d+ devices))""".r
    source.eachText flatMap (x => rex findFirstIn(x)) toList
  }


  val acquiredVersion: List[oemResource] = (getOEMs, getCounts.map(_.toInt), getURLs).zipped.map(oemResource)

  val dbVersion  = {
    val a = sql"select * from gsm_resources".query[oemResource]
    a.to[List].transact(xa).unsafeRunSync()
  }


  val cp = acquiredVersion diff dbVersion

/*  def saveDiff(name, count, url) : Update0 = {
    sql"insert into "
  }*/

}

object walhalla {
  def main(args: Array[String]): Unit = {

    val oem_table = new oemTable

    val oem_names = oem_table.cp

    println(oem_names)

  }
}
