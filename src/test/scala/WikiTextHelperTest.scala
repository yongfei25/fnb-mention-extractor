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
    val sentences = WikiTextHelper.sentencesContains(paragraph, "BIO format", SentenceOption(".", " ", 25, 10))
    val expected = Array(
      "The corpora comprise files divided by language, encoded in the BIO format (Ramshaw & Marcus, 1995).",
      "The BIO format is a simple, text-based format that divides texts into single tokens per line, and, separated by a whitespace, tags to indicate which."
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
      TextData("jumps over the", Option.empty, isLink = false),
      TextData("lazy dog", Option.empty, isLink = true),
      TextData(".", Option.empty, isLink = false)
    )
    assert(textData.sameElements(expected))
  }

  test("WikiTextHelper should split entity") {
    val entities = Array("quick brown fox", "lazy dog", "faith", "head with a brick", "life", "bolt")
    val text = "Bolt, the quick brown fox jumps over the lazy dog. Sometimes life is going to hit you in the head with a brick. Don't lose faith. "
    val splits = WikiTextHelper.splitEntity(text.toLowerCase(), entities)
    val expected = Array(
      ("", false),
      ("bolt", true),
      (", the ", false),
      ("quick brown fox", true),
      (" jumps over the ", false),
      ("lazy dog",true),
      (". sometimes ", false),
      ("life", true),
      (" is going to hit you in the ", false),
      ("head with a brick", true),
      (". don't lose ", false),
      ("faith", true),
      (". ", false)
    )
    assert(splits.sameElements(expected))
  }

  test("WikiTextHelper should return annotations") {
    val labels = Map(
      "firefox" -> "ORG",
      "lazy dog" -> "PER",
      "faith" -> "MISC",
      "head with a brick" -> "PER",
      "life" -> "ORG"
    )
    val entries = labels.keys.toArray.sortWith(_.length > _.length)
    val text = "The [[quick brown fox|firefox]] jumps over the [[lazy dog]]. Sometimes life is going to hit you in the head with a brick. Don't lose faith. "
    val annotated = WikiTextHelper.annotate(text, entries, labels, AnnotateOption(" "))
    val expected = "The/O quick/B-ORG brown/I-ORG fox/I-ORG jumps/O over/O the/O lazy/B-PER dog/I-PER ./O Sometimes/O life/B-ORG is/O going/O to/O hit/O you/O in/O the/O head/B-PER with/I-PER a/I-PER brick/I-PER ./O Don't/O lose/O faith/B-MISC ./O"
    assert(annotated.equals(expected))
  }

  test("WikiTextHelper should return annotations that has two entities") {
    val sentence = "小肥羊于1999年开始向国家商标局多次提交“小肥羊”商标注册申请，均被以缺乏显著性而驳回。"
    val labels = Map("小肥羊" -> "FNB")
    val annotations = WikiTextHelper.annotate(sentence, Array("小肥羊"), labels, AnnotateOption(""))
    val expected = "小/B-FNB 肥/I-FNB 羊/I-FNB 于/O 1/O 9/O 9/O 9/O 年/O 开/O 始/O 向/O 国/O 家/O 商/O 标/O 局/O 多/O 次/O 提/O 交/O “/O 小/B-FNB 肥/I-FNB 羊/I-FNB ”/O 商/O 标/O 注/O 册/O 申/O 请/O ，/O 均/O 被/O 以/O 缺/O 乏/O 显/O 著/O 性/O 而/O 驳/O 回/O 。/O"
    assert(annotations.equals(expected))
  }

  test("WikiTextHelper should remove tags") {
    val text = "1860年代左右，中国已经有西式餐馆。西餐在传入中国初期被称为'''番菜'''<ref>{{cite web|url=http://www.people.com.cn</ref>。该类西餐被称为[[海派西餐]]。"
    val result = WikiTextHelper.removeMarkups(text)
    val expected = "1860年代左右，中国已经有西式餐馆。西餐在传入中国初期被称为番菜。该类西餐被称为[[海派西餐]]。"
    // The tag [[海派西餐]] is intentionally retained for entity parsing.
    assert(result.equals(expected))
  }
}
