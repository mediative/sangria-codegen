object CodegenResult {
  case class BlogArticles(blog: BlogArticles.Blog)
  object BlogArticles {
    case class BlogArticlesVariables(blogId: CodegenResult.ID, pagination: Pagination)
    case class Blog(title: String, articles: List[BlogArticles.Blog.Articles])
    object Blog {
      case class Articles(title: String, body: String, tags: List[String], status: ArticleStatus, author: BlogArticles.Blog.Articles.Author)
      object Articles { case class Author(name: String) }
    }
  }
  sealed trait ArticleStatus
  object ArticleStatus {
    case object DRAFT extends ArticleStatus
    case object PUBLISHED extends ArticleStatus
  }
  case class Pagination(first: Int, count: Int, reverse: Option[Boolean])
  type ID = _root_.scala.String
}
