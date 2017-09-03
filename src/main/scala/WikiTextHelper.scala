import java.util.regex.Pattern
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
    splits.flatMap { s =>
      val matched = wikiLinkPattern.findFirstMatchIn(s)
      if (matched.isEmpty) {
        s.trim.split(Pattern.quote(option.separator)).map { token =>
          TextData(token, Option.empty, isLink = false)
        }
      } else {
        val p = matched.get.group(1).split(Pattern.quote("|"))
        val text = p(0)
        val title = if (p.size == 1) Option.empty else Option(p(1))
        Array(TextData(text, title, isLink = true))
      }
    }
  }

  def annotate (sentence: String, labels: Map[String, String], option: AnnotateOption): String = {
    sentence
  }
}
