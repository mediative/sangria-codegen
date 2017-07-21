object CodegenResult {
  case class HeroAndFriends(hero: HeroAndFriends.Hero)
  object HeroAndFriends {
    case class HeroAndFriendsVariables()
    case class Hero(name: Option[String], friends: HeroAndFriends.Hero.Friends)
    object Hero {
      case class Friends(name: Option[String], friends: HeroAndFriends.Hero.Friends.Friends)
      object Friends {
        case class Friends(name: Option[String], friends: HeroAndFriends.Hero.Friends.Friends.Friends)
        object Friends {
          case class Friends(name: Option[String], friends: HeroAndFriends.Hero.Friends.Friends.Friends.Friends)
          object Friends { case class Friends(name: Option[String]) }
        }
      }
    }
  }
}
