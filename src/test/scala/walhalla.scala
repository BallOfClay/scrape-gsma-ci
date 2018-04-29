import java.util

import org.jsoup.Jsoup
import org.jsoup.select.Elements
import scala.util.matching.Regex
import scala.collection.JavaConversions._


class oemTable {

  private val source : Elements  = {
    val sesh = Jsoup.connect("https://www.gsmarena.com/makers.php3").get()

    val maker_nodes = sesh.select(".st-text a")

    maker_nodes
  }


  def getURLs : util.List[String]  = {
    source.eachAttr("href")
  }

  def getCounts: Array[String] = {
    val rex : Regex = """(?i)((?<=\s)\d+(?= device(s)))""".r

    source.eachText() flatMap (x => rex findFirstIn(x)) toArray
  }

  def getOEMs: Array[String] = {
    val rex : Regex = """(?i)(.*(?= \d+ devices))""".r

    source.eachText flatMap (x => rex findFirstIn(x)) toArray
  }

}

object walhalla {
  def main(args: Array[String]): Unit = {

/*
    val oem_table = new oemTable

    val oem_names = oem_table.getURLs
    oem_names foreach(println)
*/

  }
}
