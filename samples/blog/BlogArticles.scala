object BlogArticlesApi {
  case class BlogArticles(blog: BlogArticlesApi.BlogArticles.Blog)
  object BlogArticles {
    case class BlogArticlesVariables(blogId: BlogArticlesApi.ID, pagination: Pagination)
    case class Blog(title: String, articles: List[BlogArticlesApi.BlogArticles.Blog.Articles])
    object Blog {
      case class Articles(title: String, body: String, tags: List[String], status: ArticleStatus, author: BlogArticlesApi.BlogArticles.Blog.Articles.Author)
      object Articles { case class Author(name: String) }
    }
  }
  sealed trait ArticleStatus
  object ArticleStatus {
    case object DRAFT extends ArticleStatus
    case object PUBLISHED extends ArticleStatus
  }
  case class Pagination(first: Int, count: Int, reverse: Option[Boolean])
  type ID = String
}
