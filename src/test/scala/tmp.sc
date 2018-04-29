import org.jsoup.Jsoup
import scala.collection.JavaConversions._

import scala.util.matching.Regex

val sesh = Jsoup.connect("https://www.gsmarena.com/makers.php3").get()

val maker_nodes = sesh.select(".st-text a")

maker_nodes.eachAttr("href")

