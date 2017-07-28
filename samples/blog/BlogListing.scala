object BlogListingApi {
  case class BlogListing(blogs: List[BlogListing.Blogs])
  object BlogListing {
    case class BlogListingVariables(pagination: Pagination)
    case class Blogs(id: BlogListingApi.ID, title: String, uri: String)
  }
  case class Pagination(first: Int, count: Int, reverse: Option[Boolean])
  type ID = String
}
