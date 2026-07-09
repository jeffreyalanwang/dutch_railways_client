* C/U/D functionality: in BackendApi and in new tab Composable
* edit stations
  * search locations in order of distance
  * test: [NavigationRefreshTest.kt] "Initial Service" should be first in list
  * organize ...ui.screens.child
  * enforce a "lock" button to enable user/admin mode (needs to be able to modify backend requests later)
* [Apollo](https://www.apollographql.com/docs/kotlin)
  * [Schema](https://www.apollographql.com/docs/apollo-server/schema/schema) with nullable fields 
  * [Mock responses](https://www.apollographql.com/docs/kotlin/testing/mocking-graphql-responses)
  * [Normalized cache](https://www.apollographql.com/docs/kotlin/caching/normalized-cache)
    * make sure to invalidate caches after edits are made
* README