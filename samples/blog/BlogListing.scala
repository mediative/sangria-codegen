object CodegenResult {
  case class BlogListing(blogs: List[BlogListing.Blogs])
  object BlogListing {
    case class BlogListingVariables(pagination: Pagination)
    case class Blogs(id: String, title: String, uri: String)
  }
  case class Pagination(first: Int, count: Int, reverse: Option[Boolean])
}
