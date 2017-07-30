object HeroFragmentsApi {
  case class HeroAppearancesAndInline(hero: HeroFragmentsApi.HeroAppearancesAndInline.Hero)
  object HeroAppearancesAndInline {
    case class HeroAppearancesAndInlineVariables()
    case class Hero(name: Option[String], appearsIn: Option[List[Option[Episode]]], id: String) extends Appearances
  }
  case class HeroAppearances(hero: HeroFragmentsApi.HeroAppearances.Hero)
  object HeroAppearances {
    case class HeroAppearancesVariables()
    case class Hero(name: Option[String], appearsIn: Option[List[Option[Episode]]]) extends Appearances
  }
  case class HeroAppearancesAndInlineAlias(hero: HeroFragmentsApi.HeroAppearancesAndInlineAlias.Hero)
  object HeroAppearancesAndInlineAlias {
    case class HeroAppearancesAndInlineAliasVariables()
    case class Hero(name: Option[String], appearsIn: Option[List[Option[Episode]]], heroId: String) extends Appearances
  }
  case class HeroFragmentOverlap(hero: HeroFragmentsApi.HeroFragmentOverlap.Hero)
  object HeroFragmentOverlap {
    case class HeroFragmentOverlapVariables()
    case class Hero(id: String, name: Option[String], appearsIn: Option[List[Option[Episode]]]) extends Identifiable with Appearances
  }
  case class HeroNameAliasAndAppearances(hero: HeroFragmentsApi.HeroNameAliasAndAppearances.Hero)
  object HeroNameAliasAndAppearances {
    case class HeroNameAliasAndAppearancesVariables()
    case class Hero(alias: Option[String], name: Option[String], appearsIn: Option[List[Option[Episode]]]) extends NameAlias with Appearances
  }
  trait Appearances {
    def name: Option[String]
    def appearsIn: Option[List[Option[Episode]]]
  }
  trait Identifiable {
    def id: String
    def name: Option[String]
  }
  trait NameAlias { def alias: Option[String] }
  sealed trait Episode
  object Episode {
    case object NEWHOPE extends Episode
    case object EMPIRE extends Episode
    case object JEDI extends Episode
  }
}
