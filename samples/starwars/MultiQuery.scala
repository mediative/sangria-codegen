object CodegenResult {
  case class HeroAndFriends(hero: HeroAndFriends.Hero)
  object HeroAndFriends {
    case class HeroAndFriendsVariables()
    case class Hero(name: Option[String], friends: Option[List[Option[HeroAndFriends.Hero.Friends]]])
    object Hero { case class Friends(name: Option[String]) }
  }
  case class HeroAndNestedFriends(hero: HeroAndNestedFriends.Hero)
  object HeroAndNestedFriends {
    case class HeroAndNestedFriendsVariables()
    case class Hero(name: Option[String], friends: Option[List[Option[HeroAndNestedFriends.Hero.Friends]]])
    object Hero {
      case class Friends(name: Option[String], friends: Option[List[Option[HeroAndNestedFriends.Hero.Friends.Friends]]])
      object Friends {
        case class Friends(name: Option[String], friends: Option[List[Option[HeroAndNestedFriends.Hero.Friends.Friends.Friends]]])
        object Friends {
          case class Friends(name: Option[String], friends: Option[List[Option[HeroAndNestedFriends.Hero.Friends.Friends.Friends.Friends]]])
          object Friends { case class Friends(name: Option[String]) }
        }
      }
    }
  }
  case class FragmentExample(human: Option[FragmentExample.Human], droid: FragmentExample.Droid)
  object FragmentExample {
    case class FragmentExampleVariables()
    case class Human(name: Option[String], appearsIn: Option[List[Option[Episode]]], homePlanet: Option[String]) extends Common
    case class Droid(name: Option[String], appearsIn: Option[List[Option[Episode]]], primaryFunction: Option[String]) extends Common
  }
  case class VariableExample(human: Option[VariableExample.Human])
  object VariableExample {
    case class VariableExampleVariables(humanId: String)
    case class Human(name: Option[String], homePlanet: Option[String], friends: Option[List[Option[VariableExample.Human.Friends]]])
    object Human { case class Friends(name: Option[String]) }
  }
  trait Common {
    def name: Option[String]
    def appearsIn: Option[List[Option[Episode]]]
  }
  sealed trait Episode
  object Episode {
    case object NEWHOPE extends Episode
    case object EMPIRE extends Episode
    case object JEDI extends Episode
  }
}
