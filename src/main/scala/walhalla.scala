import java.time.ZoneOffset

import org.jsoup.Jsoup
import org.jsoup.select.Elements

import scala.util.matching.Regex
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import doobie._
import doobie.implicits._
// import cats._
import cats.effect._
// import cats.implicits._

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

// import shapeless._



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
    val rex : Regex = """(?i)(.*(?= \d+ device(s)))""".r
    source.eachText flatMap (x => rex findFirstIn(x)) toList
  }

  val acquiredVersion: List[oemResource] = (getOEMs, getCounts.map(_.toInt), getURLs).zipped.map(oemResource)

  val dbVersion  = {
    val a = sql"select * from gsm_resources".query[oemResource]
    a.to[List].transact(xa).unsafeRunSync()
  }

  val dbDiff = acquiredVersion diff dbVersion

  def insertDiff(oem: String, count: Int, location: String): Update0 = {
    sql"""
         insert into gsm_resources_diff (
         			oem, device_count_new, url_new, device_count_old, url_old, insert_time
         		)
         	values(
         		$oem, $count, $location,
         		( select device_count from gsm_resources
         			where gsm_resources.oem = $oem),
         		( select location from gsm_resources
         			where gsm_resources.oem = $oem),
            (select current_timestamp )
         	)""".update
  }

}


class getOemPages(oemLocation: String) {

  val parsedUri = "https://www.gsmarena.com/" + oemLocation
  val source : Elements  = {
    val sesh = Jsoup.connect(parsedUri).get()
    sesh.select(".nav-pages strong , .nav-pages a")
  }

  val pages = source.eachText() toList
  val rex: Regex = """(?<=\-)\d+(?=.php)""".r
  val gsmCode: String = rex findFirstIn oemLocation toString
  val gsm: String = "https://www.gsmarena.com/"

  def genGsmStyledUrl(oem_base: String, gsm_code: String, page_nos: List[String]) : List[String] = {
    //strip unnecessary chars from input url, and then generate URLs according to the gsma scheme
    val oem_title = oem_base replace("-"+gsmCode+".php", "")
    val template: String = gsm + oem_title + "-f-" + gsm_code + "-0-p"
    val tbr = page_nos map  (x => template + x + ".php")
    tbr
  }

 val oemPages: List[String] = if (pages.length > 1 ) {
   genGsmStyledUrl(oemLocation, gsmCode, pages)
  } else {
   List(oemLocation)
 }

}


object walhalla {
  def main(args: Array[String]): Unit = {

    val oem_table = new oemTable
    val oemResourcesDiff = oem_table.dbDiff

    if (oemResourcesDiff.nonEmpty){

      /*oemResourcesDiff foreach ( x => oem_table.insertDiff(x.name, x.count, x.location)
       .run.transact(oem_table.xa).unsafeRunSync()
       )*/

      val someoem = oem_table.getURLs.get(0)

      val pgs = new getOemPages(someoem)
      // println(pgs.tbr.getClass)
      pgs.oemPages foreach println
    }
    
  }
}
