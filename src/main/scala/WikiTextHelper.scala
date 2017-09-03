import java.util.regex.Pattern

import WikiTextHelper.splitEntity

import scala.collection.immutable.SortedMap
import scala.util.matching.Regex

object WikiTextHelper {

  case class SentenceOption
  (
    stop: String
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

  def sentencesContains (paragraph: String, entity: String, option: SentenceOption): Array[String] = {
    val sentences = paragraph.split(Pattern.quote(option.stop))
    sentences.filter(_.contains(entity))
      .map(_.replaceAll("\n", "") + option.stop)
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
    val middle = found.get
    val head = textParts.head
    val tail = if (textParts.size == 2) textParts(1) else ""
    splitEntity(head, entities) ++ Array((middle, true)) ++ splitEntity(tail, entities)
  }

  def annotate (sentence: String, labels: SortedMap[String, String], option: AnnotateOption): String = {
    val textDataList = textData(sentence, option)
    val entities = labels.keys.toList
    val annotations = textDataList.flatMap { textData =>
      if (textData.isLink) {
        val found = entities.find { entity =>
          val text = textData.linkTitle.getOrElse(textData.text)
          text.contains(entity)
        }
        val tag = if (found.nonEmpty) labels(found.get) else "O"
        Array(Annotation(textData.text, tag))
      } else {
        val entitySplits = splitEntity(textData.text, entities.toArray)
        entitySplits.map { split =>
          val tag = if (split._2) labels(split._1) else "O"
          Annotation(split._1, tag)
        }
      }
    }
    val tokens = annotations.flatMap { annotation =>
      val s = annotation.text.split(option.separator)
      for (i <- s.indices) yield {
        if (annotation.tag == "O") {
          s"${s(i)}/${annotation.tag}"
        } else if (i == 0) {
          s"${s(i)}/B-${annotation.tag}"
        } else {
          s"${s(i)}/I-${annotation.tag}"
        }
      }
    }
    tokens.mkString(" ")
  }
}
