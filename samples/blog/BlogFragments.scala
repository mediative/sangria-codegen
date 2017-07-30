object BlogFragmentsApi {
  case class BlogFragments(blog: BlogFragmentsApi.BlogFragments.Blog)
  object BlogFragments {
    case class BlogFragmentsVariables(blogId: BlogFragmentsApi.ID, pagination: Pagination)
    case class Blog(title: String, articles: List[BlogFragmentsApi.BlogFragments.Blog.Articles], articlesWithAuthorId: List[BlogFragmentsApi.BlogFragments.Blog.ArticlesWithAuthorId])
    object Blog {
      case class Articles(title: String, author: BlogFragmentsApi.BlogFragments.Blog.Articles.Author) extends ArticleFragment
      object Articles { case class Author(id: BlogFragmentsApi.ID, name: String) extends AuthorFragment }
      case class ArticlesWithAuthorId(id: BlogFragmentsApi.ID, title: String, author: BlogFragmentsApi.BlogFragments.Blog.ArticlesWithAuthorId.Author) extends IdFragment with ArticleWithAuthorIdFragment
      object ArticlesWithAuthorId { case class Author(id: BlogFragmentsApi.ID, name: String) extends IdFragment with AuthorFragment }
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
  case class Pagination(first: Int, count: Int, reverse: Option[Boolean])
  type ID = String
}
