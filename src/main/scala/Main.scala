import java.io.{File, FileWriter}
import java.sql.{Connection, DriverManager}

import WikiTextHelper.{AnnotateOption, SentenceOption}
import info.bliki.wiki.filter.PlainTextConverter
import info.bliki.wiki.model.WikiModel
import org.mariadb.jdbc.MariaDbDataSource
import types.{CategoryLink, LinkType}

import scala.collection.immutable.Queue
import scala.util.Random

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
    val maxTokens = 30
    val minTokens = 10
    val stopWord = "。"
    val excludeTitle = Set[String]("列表", "产品", "事件")
    var processed = Set[String]()
    var cats = List[String]("各地飲食")
    var pageQueue = Queue[PageLink]()
    var labels = Map.empty[String, String]
    var skipCount = 1 // how many levels we need to skip

    // The prefix for each sentence
    var sentencePrefix = "清区的传统小吃"
    val numGenerator = new Random()

    def linkTitle (sortKey: String, prefix: String) = {
      sortKey.replace(prefix, "").replaceAll("\\s+", " ").trim
    }
    def pageTitle (title: String) = {
      title.replaceAll("_", " ").replaceAll("\\s+", " ").trim
    }
    def excluded (title: String): Boolean = {
      excludeTitle.exists(title.contains(_))
    }
    def validTitle (title: String): Boolean = {
      title.nonEmpty && !excluded(title)
    }
    def addPrefix (sentence: String): String = {
      val newSentence = sentencePrefix + sentence
      val prefixLen = numGenerator.nextInt(15)
      sentencePrefix = newSentence.takeRight(prefixLen).replace(stopWord, "")
      newSentence.take(maxTokens)
    }

    while (cats.nonEmpty) {
      val catLinks = DbHelper.getCategoryLinksIn(conn, cats)
      processed = processed ++ cats

      if (skipCount == 0) {
        for {
          page <- catLinks if page.linkType == LinkType.Page
          title = pageTitle(page.pageTitle.getOrElse("")) if validTitle(title)
        } {
          pageQueue = pageQueue.enqueue(PageLink(title, page.from))
          labels += (title -> tag)
        }

        val pageTitles = for {
          page <- catLinks if page.linkType == LinkType.Page && page.pageTitle.isDefined
        } yield page.pageTitle.get

        // Enrich entity label by searching redirection to the existing page titles
        val redirectedTitles = DbHelper.getRedirectedTitles(conn, pageTitles)
        redirectedTitles.foreach { title => labels += (pageTitle(title) -> tag) }
        println(s"Found ${redirectedTitles.length} redirected titles.")
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

    val sortedEntries = labels.keys.toArray.sortWith(_.length > _.length)
    val sentenceOption = SentenceOption(stopWord, "", maxTokens, minTokens)
    val annotationOption = AnnotateOption("")
    val outputWriter = new FileWriter(new File("annotations.txt"))
    val printEvery = 500
    var count = 0
    var annotationList = List[String]()

    while (pageQueue.nonEmpty) {
      val dq = pageQueue.dequeue
      val link = dq._1
      pageQueue = dq._2
      val source = DbHelper.getPageSource(conn, link.pageId)
      if (source.nonEmpty) {
        val text = source.get
        var sentences = WikiTextHelper.sentencesContains(text, link.title, sentenceOption)
        sentences = sentences.map(addPrefix)
        val annotations = sentences.map(WikiTextHelper.annotate(_, sortedEntries, labels, annotationOption))
        annotationList = annotationList ++ annotations
      }
    }

    // Shuffle the examples
    annotationList = Random.shuffle(annotationList)
    annotationList.foreach({ s =>
      count +=1
      if (count % printEvery == 0) {
        println(s)
      }
      outputWriter.write(s"$s\n")
    })
    outputWriter.close()
    conn.close()
  }
}