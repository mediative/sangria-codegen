/*
 * Copyright 2017 Mediative
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediative.sangria.codegen
package starwars

import org.scalatest.{WordSpec, Matchers}
import com.mediative.sangria.codegen.GraphQLDomain

@GraphQLDomain(
  "samples/starwars/schema.graphql",
  "samples/starwars/MultiQuery.graphql"
)
object StarWarsDomain

class StarWarsAnnotationSpec extends WordSpec with Matchers {
  "GraphQLDomain" should {
    "support the Star Wars schema and operations" in {
      import StarWarsDomain.HeroAndFriends

      val heroAndFriends = HeroAndFriends(
        hero = HeroAndFriends.Hero(
          name = Some("Luke"),
          friends = Some(List(Some(HeroAndFriends.Hero.Friends(Some("R2D2")))))
        )
      )

      assert(heroAndFriends.hero.friends.get.head.get.name.get == "R2D2")
    }
  }
}
