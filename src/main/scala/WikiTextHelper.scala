import java.util.regex.Pattern

import WikiTextHelper.splitEntity

import scala.collection.immutable.SortedMap
import scala.util.matching.Regex

object WikiTextHelper {

  case class SentenceOption
  (
    stop: String,
    separator: String,
    maxTokens: Int,
    minTokens: Int
  )

  case class AnnotateOption
  (
    separator: String
  )

  case class TextData
  (
    text: String,
    linkTitle: Option[String],
    isLink: Boolean
  )

  case class Annotation
  (
    text: String,
    tag: String
  )

  val wikiLinkPattern: Regex = """\[\[(.+?)\]\]""".r

  /**
    * Remove unwanted markup
    * @param s - The paragraph
    * @return
    */
  def removeMarkups (s: String): String = {
    // Remove most markup text with these regex
    s
      // Eg: <ref name="gaiming">game</ref>。
      .replaceAll("""<\w+>?([\w\W]+?)(<\/\w+>|\/>)""", "")
      // Eg: {cite web|url=http://www.people.com}}
      .replaceAll("""\{\{([\w\W]+?)\}\}""", "")
      // Eg: {| border="0" cellpadding="0" width="70%" |}
      .replaceAll("""\{\|([\w\W]+?)\|\}""", "")
      // Eg: == 正餐 ==
      .replaceAll("""==([\w\W]+?)==""", "")
      // Eg: [[File:Roastbeef with yorkshire puddings.jpg]]
      .replaceAll("""\[\[\w+:.+\]\]""", "")
      // Eg: '''番菜''' (Removing the triple quotes)
      .replaceAll("""'''""", "")
  }

  def sentencesContains (paragraph: String, entity: String, option: SentenceOption): Array[String] = {
    val sentences = removeMarkups(paragraph)
      .split("\n")
      .flatMap(_.split(Pattern.quote(option.stop)))

    sentences
      .filter({ s => !s.startsWith("*") && !s.startsWith("|") && s.contains(entity) })
      .map { s =>
        s.split(Pattern.quote(option.separator))
          .take(option.maxTokens)
          .mkString(option.separator)
      }
      .map(_.replaceAll("[\r\n]+", " ").replaceAll("\\s+", " ").trim)
      .filter({ s => s.length >= option.minTokens && s.contains(entity) })
  }

  def splitLinks (sentence: String, option: AnnotateOption): Array[String] = {
    val matches = wikiLinkPattern.findAllIn(sentence).matchData.toList
    var splits = Array[String]()
    var curIndex = 0
    for (matched <- matches) {
      splits :+= sentence.slice(curIndex, matched.start)
      splits :+= sentence.slice(matched.start, matched.end)
      curIndex = matched.end
    }

    if (curIndex != sentence.length) {
      splits :+= sentence.substring(curIndex)
    }
    splits
  }

  def textData (sentence: String, option: AnnotateOption): Array[TextData] = {
    val splits = splitLinks(sentence, option)
    splits.map { s =>
      val matched = wikiLinkPattern.findFirstMatchIn(s)
      if (matched.isEmpty) {
        TextData(s.trim, Option.empty, isLink = false)
      } else {
        val p = matched.get.group(1).split(Pattern.quote("|"))
        val text = p(0)
        val title = if (p.size == 1) Option.empty else Option(p(1))
        TextData(text, title, isLink = true)
      }
    }
  }

  def splitEntity(text: String, entities: Array[String]): Array[(String, Boolean)] = {
    val found = entities.find(text.contains(_))
    if (found.isEmpty) {
      return Array((text, false))
    }
    val textParts = text.split(Pattern.quote(found.get))
    var result = Array[(String, Boolean)]()
    for (i <- textParts.indices) {
      result = result ++ splitEntity(textParts(i), entities)
      if (i != textParts.length - 1) {
        result = result ++ Array((found.get, true))
      }
    }
    result
  }

  def annotate (sentence: String, sortedEntries: Array[String], labels: Map[String, String], option: AnnotateOption): String = {
    val textDataList = textData(sentence, option)
    val entities = sortedEntries
    val annotations = textDataList.flatMap { textData =>
      if (textData.isLink) {
        val found = entities.find { entity =>
          val text = textData.linkTitle.getOrElse(textData.text)
          text.contains(entity)
        }
        val tag = if (found.nonEmpty) labels(found.get) else "O"
        Array(Annotation(textData.text, tag))
      } else {
        val entitySplits = splitEntity(textData.text, entities)
        entitySplits.map { split =>
          val tag = if (split._2) labels(split._1) else "O"
          Annotation(split._1, tag)
        }
      }
    }
    val tokens = annotations.flatMap { annotation =>
      val s = annotation.text.split(option.separator)
      for {
        i <- s.indices
        t = s(i).trim if t.nonEmpty
      } yield {
        if (annotation.tag == "O") {
          s"$t/${annotation.tag}"
        } else if (i == 0) {
          s"$t/B-${annotation.tag}"
        } else {
          s"$t/I-${annotation.tag}"
        }
      }
    }
    tokens.mkString(" ")
  }
}
