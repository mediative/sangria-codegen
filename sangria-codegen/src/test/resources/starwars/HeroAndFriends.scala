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
  case class Droid(id: String, name: Option[String], friends: Option[List[Option[Character]]], appearsIn: Option[List[Option[Episode]]], primaryFunction: Option[String], secretBackstory: Option[String])
  sealed trait Episode
  object Episode {
    case object NEWHOPE extends Episode
    case object EMPIRE extends Episode
    case object JEDI extends Episode
  }
  trait Character {
    def id: String
    def name: Option[String]
    def friends: Option[List[Option[Character]]]
    def appearsIn: Option[List[Option[Episode]]]
    def secretBackstory: Option[String]
  }
  case class Human(id: String, name: Option[String], friends: Option[List[Option[Character]]], appearsIn: Option[List[Option[Episode]]], homePlanet: Option[String], secretBackstory: Option[String])
}
