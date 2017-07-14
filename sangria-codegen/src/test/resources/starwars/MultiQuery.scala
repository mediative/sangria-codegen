object CodegenResult {
  case class HeroAndFriends(hero: HeroAndFriends.Hero)
  object HeroAndFriends {
    case class HeroAndFriendsVariables()
    implicit val RequestBuilder: _root_.com.mediative.sangria.codegen.GraphQLRequestBuilder[HeroAndFriendsVariables, HeroAndFriends] = new _root_.com.mediative.sangria.codegen.GraphQLRequestBuilder("HeroAndFriends")
    case class Hero(name: Option[String], friends: HeroAndFriends.Hero.Friends)
    object Hero { case class Friends(name: Option[String]) }
  }
  case class HeroAndNestedFriends(hero: HeroAndNestedFriends.Hero)
  object HeroAndNestedFriends {
    case class HeroAndNestedFriendsVariables()
    implicit val RequestBuilder: _root_.com.mediative.sangria.codegen.GraphQLRequestBuilder[HeroAndNestedFriendsVariables, HeroAndNestedFriends] = new _root_.com.mediative.sangria.codegen.GraphQLRequestBuilder("HeroAndNestedFriends")
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
    implicit val RequestBuilder: _root_.com.mediative.sangria.codegen.GraphQLRequestBuilder[FragmentExampleVariables, FragmentExample] = new _root_.com.mediative.sangria.codegen.GraphQLRequestBuilder("FragmentExample")
    case class Human(name: Option[String], appearsIn: Option[List[Option[Episode]]], homePlanet: Option[String])
    case class Droid(name: Option[String], appearsIn: Option[List[Option[Episode]]], primaryFunction: Option[String])
  }
  case class VariableExample(human: VariableExample.Human)
  object VariableExample {
    case class VariableExampleVariables(humanId: String)
    implicit val RequestBuilder: _root_.com.mediative.sangria.codegen.GraphQLRequestBuilder[VariableExampleVariables, VariableExample] = new _root_.com.mediative.sangria.codegen.GraphQLRequestBuilder("VariableExample")
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
