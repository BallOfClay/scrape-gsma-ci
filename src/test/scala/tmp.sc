import org.jsoup.Jsoup
import org.jsoup.select.Elements

import scala.util.matching.Regex

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

val source : Elements  = {
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

val scrapedURLs = getURLs
val scrapedCounts = getCounts
val scrapedOEMs = getOEMs

case class oemResource(name: String, count: Int, location: String)

val test = (scrapedOEMs, scrapedCounts.map(_.toInt), scrapedURLs).zipped.map(oemResource)



