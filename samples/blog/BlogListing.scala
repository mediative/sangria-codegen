object CodegenResult {
  case class BlogListing(blogs: List[BlogListing.Blogs])
  object BlogListing {
    case class BlogListingVariables(pagination: Pagination)
    case class Blogs(id: CodegenResult.ID, title: String, uri: String)
  }
  case class Pagination(first: Int, count: Int, reverse: Option[Boolean])
  type ID = _root_.scala.String
}
