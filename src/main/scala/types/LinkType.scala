package types

object LinkType extends Enumeration {
  type LinkType = Value
  val SubCat:LinkType = Value("subcat")
  val Page:LinkType = Value("page")
  val Other:LinkType = Value("other")
}
