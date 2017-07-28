object HeroAndFriendsApi {
  case class HeroAndFriends(hero: HeroAndFriends.Hero)
  object HeroAndFriends {
    case class HeroAndFriendsVariables()
    case class Hero(name: Option[String], friends: Option[List[Option[HeroAndFriends.Hero.Friends]]])
    object Hero {
      case class Friends(name: Option[String], friends: Option[List[Option[HeroAndFriends.Hero.Friends.Friends]]])
      object Friends {
        case class Friends(name: Option[String], friends: Option[List[Option[HeroAndFriends.Hero.Friends.Friends.Friends]]])
        object Friends {
          case class Friends(name: Option[String], friends: Option[List[Option[HeroAndFriends.Hero.Friends.Friends.Friends.Friends]]])
          object Friends { case class Friends(name: Option[String]) }
        }
      }
    }
  }
}
