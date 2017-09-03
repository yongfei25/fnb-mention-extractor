package types

import types.LinkType.LinkType

case class CategoryLink
(
  from: String,
  to: String,
  sortKey: String,
  sortKeyPrefix: String,
  linkType: LinkType
)