object BlogFragmentsApi {
  case class BlogFragments(blog: BlogFragmentsApi.BlogFragments.Blog)
  object BlogFragments {
    case class BlogFragmentsVariables(blogId: BlogFragmentsApi.ID, pagination: Pagination)
    case class Blog(title: String, articles: List[BlogFragmentsApi.BlogFragments.Blog.Articles], articlesWithAuthorId: List[BlogFragmentsApi.BlogFragments.Blog.ArticlesWithAuthorId])
    object Blog {
      case class Articles(title: String, author: BlogFragmentsApi.BlogFragments.Blog.Articles.Author) extends BlogFragmentsApi.ArticleFragment
      object Articles { case class Author(id: BlogFragmentsApi.ID, name: String) extends BlogFragmentsApi.AuthorFragment }
      case class ArticlesWithAuthorId(id: BlogFragmentsApi.ID, title: String, author: BlogFragmentsApi.BlogFragments.Blog.ArticlesWithAuthorId.Author) extends BlogFragmentsApi.IdFragment with BlogFragmentsApi.ArticleWithAuthorIdFragment
      object ArticlesWithAuthorId { case class Author(id: BlogFragmentsApi.ID, name: String) extends BlogFragmentsApi.IdFragment with BlogFragmentsApi.AuthorFragment }
    }
  }
  trait ArticleFragment {
    def title: String
    def author: AuthorFragment
  }
  trait AuthorFragment {
    def id: BlogFragmentsApi.ID
    def name: String
  }
  trait IdFragment { def id: BlogFragmentsApi.ID }
  trait ArticleWithAuthorIdFragment {
    def title: String
    def author: IdFragment with AuthorFragment
  }
  case class Pagination(first: Int, count: Int, order: Option[PaginationOrder])
  sealed trait PaginationOrder
  object PaginationOrder {
    case object ASC extends BlogFragmentsApi.PaginationOrder
    case object DESC extends BlogFragmentsApi.PaginationOrder
  }
  type ID = String
}
