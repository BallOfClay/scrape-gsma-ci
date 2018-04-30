import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

val conf: Config  = ConfigFactory.load()

def printSetting(path: String) {
  println("The setting '" + path + "' is: " + conf.getString(path))
}