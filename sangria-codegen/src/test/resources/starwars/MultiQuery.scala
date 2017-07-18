object CodegenResult {
  case class HeroAndFriends(hero: HeroAndFriends.Hero)
  object HeroAndFriends {
    case class HeroAndFriendsVariables()
    case class Hero(name: Option[String], friends: HeroAndFriends.Hero.Friends)
    object Hero { case class Friends(name: Option[String]) }
  }
  case class HeroAndNestedFriends(hero: HeroAndNestedFriends.Hero)
  object HeroAndNestedFriends {
    case class HeroAndNestedFriendsVariables()
    case class Hero(name: Option[String], friends: HeroAndNestedFriends.Hero.Friends)
    object Hero {
      case class Friends(name: Option[String], friends: HeroAndNestedFriends.Hero.Friends.Friends)
      object Friends {
        case class Friends(name: Option[String], friends: HeroAndNestedFriends.Hero.Friends.Friends.Friends)
        object Friends {
          case class Friends(name: Option[String], friends: HeroAndNestedFriends.Hero.Friends.Friends.Friends.Friends)
          object Friends { case class Friends(name: Option[String]) }
        }
      }
    }
  }
  case class FragmentExample(human: FragmentExample.Human, droid: FragmentExample.Droid)
  object FragmentExample {
    case class FragmentExampleVariables()
    case class Human(name: Option[String], appearsIn: Option[List[Option[Episode]]], homePlanet: Option[String])
    case class Droid(name: Option[String], appearsIn: Option[List[Option[Episode]]], primaryFunction: Option[String])
  }
  case class VariableExample(human: VariableExample.Human)
  object VariableExample {
    case class VariableExampleVariables(humanId: String)
    case class Human(name: Option[String], homePlanet: Option[String], friends: VariableExample.Human.Friends)
    object Human { case class Friends(name: Option[String]) }
  }
  trait Common {
    def id: String
    def name: Option[String]
    def friends: Option[List[Option[Character]]]
    def appearsIn: Option[List[Option[Episode]]]
    def secretBackstory: Option[String]
  }
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
}
