import java.io.{File, FileWriter}
import java.sql.{Connection, DriverManager}

import WikiTextHelper.{AnnotateOption, SentenceOption}
import info.bliki.wiki.filter.PlainTextConverter
import info.bliki.wiki.model.WikiModel
import org.mariadb.jdbc.MariaDbDataSource
import types.{CategoryLink, LinkType}

import scala.collection.immutable.Queue

object Main extends App {
  case class PageLink
  (
    title: String,
    pageId: String
  )
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

    val tag = "FNB"
    val maxTokens = 50
    val excludeTitle = Set[String]("列表", "产品", "事件")
    var processed = Set[String]()
    var cats = List[String]("各國飲食")
    var pageQueue = Queue[PageLink]()
    var labels = Map.empty[String, String]
    var skipCount = 1 // how many levels we need to skip

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
//      println(s"Getting links from categories $cats")
      val catLinks = DbHelper.getCategoryLinksIn(conn, cats)
      processed = processed ++ cats

      if (skipCount == 0) {
        for {
          page <- catLinks if page.linkType == LinkType.Page
          title = linkTitle(page.sortKey, page.sortKeyPrefix) if validTitle(title)
        } {
          pageQueue = pageQueue.enqueue(PageLink(title, page.from))
          labels += (title -> tag)
        }
      } else {
        skipCount -= 1
      }

      cats = catLinks
        .filter { cat =>
          cat.linkType == LinkType.SubCat &&
            !processed.contains(cat.sortKey)
        }
        .map(c => c.sortKey)
    }

    val wikiModel = new WikiModel("wiki/${image}", "wiki/${title}")
    val sortedEntries = labels.keys.toArray.sortWith(_.length > _.length)
    val sentenceOption = SentenceOption("。", "", maxTokens)
    val annotationOption = AnnotateOption("")
    val outputWriter = new FileWriter(new File("annotations.txt"))
    var count = 0

    while (pageQueue.nonEmpty) {
      val dq = pageQueue.dequeue
      val link = dq._1
      pageQueue = dq._2
      val source = DbHelper.getPageSource(conn, link.pageId)
      if (source.nonEmpty) {
        val text = wikiModel.render(new PlainTextConverter(), source.get)
        val sentences = WikiTextHelper.sentencesContains(text, link.title, sentenceOption)
        val annotations = sentences.map(WikiTextHelper.annotate(_, sortedEntries, labels, annotationOption))
        annotations.foreach({ s => outputWriter.write(s"$s\n")})
        count += annotations.size
        println(s"Total annotations: $count")
      }
    }
    outputWriter.close()
    conn.close()
  }
}