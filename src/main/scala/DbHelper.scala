import java.sql.Connection
import types.{CategoryLink, LinkType}

object DbHelper {
  def getCategoryLinksIn(conn: Connection, categories: List[String]): List[CategoryLink] = {
    var sql = "select cl_from, cl_to, cl_sortkey, cl_sortkey_prefix, cl_type, page_title from categorylinks left join page on page_id = cl_from where cl_to in ("
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
        },
        rs.getString(6) match {
          case null => Option.empty
          case s: String => Option(s)
        }
      ) :: links
    }
    statement.close()
    links
  }

  def getPageSource(conn: Connection, pageId: String): Option[String] = {
    val sql =
      """
        |select old_text
        |from page p left join text t on t.old_id = p.page_latest
        |where page_id = ?;
      """.stripMargin
    val statement = conn.prepareStatement(sql)
    statement.setString(1, pageId)
    val rs = statement.executeQuery()
    val s = if (rs.next()) Option(rs.getString(1)) else Option.empty
    statement.close()
    s
  }

  def getRedirectedTitles(conn: Connection, pageIds: List[String]): List[String] = {
    var sql = "select page_title from redirect r left join page p on p.page_id = r.rd_from where rd_title in ("
    for (i <- pageIds.indices) {
      sql += "?"
      sql += { if (i == pageIds.size - 1) ")" else "," }
    }
    val statement = conn.prepareStatement(sql)
    for (i <- pageIds.indices) {
      statement.setString(i+1, pageIds(i))
    }
    val rs = statement.executeQuery()
    var titles = List[String]()
    while(rs.next()) {
      val title = rs.getString(1)
      if (title != null && title.length > 0) {
        titles = titles :+ rs.getString(1)
      }
    }
    titles
  }
}
