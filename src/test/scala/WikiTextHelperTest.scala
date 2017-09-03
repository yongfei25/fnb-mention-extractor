import WikiTextHelper.{AnnotateOption, SentenceOption, TextData}
import org.scalatest._

class WikiTextHelperTest extends FunSuite {
  val paragraph: String =
    """
      |The corpora comprise files divided by language, encoded in the BIO format (Ramshaw & Marcus, 1995).
      |The BIO format is a simple, text-based format that divides texts into single tokens per line, and, separated by a whitespace, tags to indicate which ones are named entities.
      |The most commonly used tags are PER (person), LOC (location) and ORG (organization). To indicate named entities that span multiple tokens,
      |the tags have a prefix of either B- (begining of named entity) or I- (continuation of named entity). O tags are used to indicate that the token is not a named entity.
    """.stripMargin

  test("WikiTextHelper should return sentences that contain entity") {
    val sentences = WikiTextHelper.sentencesContains(paragraph, "BIO format", SentenceOption("."))
    val expected = Array(
      "The corpora comprise files divided by language, encoded in the BIO format (Ramshaw & Marcus, 1995).",
      "The BIO format is a simple, text-based format that divides texts into single tokens per line, and, separated by a whitespace, tags to indicate which ones are named entities."
    )
    assert(sentences.sameElements(expected))
  }

  test("WikiTextHelper should split links") {
    val text = "The [[quick brown fox|firefox]] jumps over the [[lazy dog]]."
    val splits = WikiTextHelper.splitLinks(text, AnnotateOption(" "))
    val expected = Array(
      "The ", "[[quick brown fox|firefox]]", " jumps over the ", "[[lazy dog]]", "."
    )
    assert(splits.sameElements(expected))
  }

  test("WikiTextHelper should return TextData array") {
    val text = "The [[quick brown fox|firefox]] jumps over the [[lazy dog]]."
    val textData = WikiTextHelper.textData(text, AnnotateOption(" "))
    val expected = Array(
      TextData("The", Option.empty, isLink = false),
      TextData("quick brown fox", Option("firefox"), isLink = true),
      TextData("jumps", Option.empty, isLink = false),
      TextData("over", Option.empty, isLink = false),
      TextData("the", Option.empty, isLink = false),
      TextData("lazy dog", Option.empty, isLink = true),
      TextData(".", Option.empty, isLink = false)
    )
    assert(textData.sameElements(expected))
  }
}
