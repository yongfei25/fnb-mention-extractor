import java.sql.Connection

import types.{CategoryLink, LinkType}

object DbHelper {
  def getCategoryLinksIn(conn: Connection, categories: List[String]): List[CategoryLink] = {
    var sql = "select cl_from, cl_to, cl_sortkey, cl_sortkey_prefix, cl_type from categorylinks where cl_to in ("
    for (i <- 1 to categories.size) {
      sql += "?"
      sql += { if (i == categories.size) ")" else "," }
    }
    val statement = conn.prepareStatement(sql)
    for (i <- categories.indices) {
      statement.setString(i+1, categories(i))
    }
    val rs = statement.executeQuery()
    var links = List[CategoryLink]()
    while (rs.next()) {
      links = CategoryLink(
        rs.getString(1),
        rs.getString(2),
        rs.getString(3),
        rs.getString(4),
        rs.getString(5) match {
          case "subcat" => LinkType.SubCat
          case "page" => LinkType.Page
          case _ => LinkType.Other
        }
      ) :: links
    }
    statement.close()
    links
  }
}
