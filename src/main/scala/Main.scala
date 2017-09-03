import java.io.{File, FileWriter}
import java.sql.{Connection, DriverManager}

import org.mariadb.jdbc.MariaDbDataSource
import types.{CategoryLink, LinkType}

import scala.collection.mutable

object Main extends App {
  override def main(args: Array[String]): Unit = {
    val host = "127.0.0.1"
    val port = 3306
    val db = "zhwiki"
    val username = "dataUser"
    val password = "dataUserPassword"
    val url = s"jdbc:mariadb://$host:$port/$db"
    val mysql = new MariaDbDataSource()
    mysql.setUser(username)
    mysql.setPassword(password)
    mysql.setUrl(url)
    var conn: Connection = null
    try {
      conn  = mysql.getConnection
    } catch {
      case e: Exception => e.printStackTrace()
    }

    val excludeTitle = Set[String]("列表", "产品", "事件")
    val outputWriter = new FileWriter(new File("pages.txt"))
    var processed = Set[String]()
    var cats = List[String]("各國飲食")

    def linkTitle (sortKey: String, prefix: String) = {
      sortKey.replace(prefix, "").replaceAll("\\s+", " ").trim
    }
    def excluded (title: String): Boolean = {
      excludeTitle.exists(title.contains(_))
    }
    def validTitle (title: String): Boolean = {
      title.length > 0 && !excluded(title)
    }

    while (cats.nonEmpty) {
      println(s"Getting links from categories $cats")
      val catLinks = DbHelper.getCategoryLinksIn(conn, cats)
      processed = processed ++ cats

      // Write pages
      println("Writing page links")
      for {
        page <- catLinks if page.linkType == LinkType.Page
        title = linkTitle(page.sortKey, page.sortKeyPrefix) if validTitle(title)
      } {
        outputWriter.write(s"$title, ${page.from}\n")
      }

      cats = catLinks
        .filter { cat =>
          cat.linkType == LinkType.SubCat &&
            !processed.contains(cat.sortKey)
        }
        .map(c => c.sortKey)
    }
    outputWriter.close()
    conn.close()
  }
}