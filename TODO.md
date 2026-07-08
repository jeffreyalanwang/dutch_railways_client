* C/U/D functionality: in BackendApi and in new tab Composable
* edit stations
  * test functionality of refresh of previous screen
  * search location by distance from point
  * figure out why the naventries have nested keys + also keys for rememberSaveable
  * can we refresh the top level tabs using contentkey? and or [refreshkey]
  * test functionality of: // Refresh the previous screen by closing + reopening
  * search locations in order of distance
  * organize ...ui.screens.child
  * enforce a "lock" button to enable user/admin mode (needs to be able to modify backend requests later)
* [Apollo](https://www.apollographql.com/docs/kotlin)
  * [Schema](https://www.apollographql.com/docs/apollo-server/schema/schema) with nullable fields 
  * [Mock responses](https://www.apollographql.com/docs/kotlin/testing/mocking-graphql-responses)
  * [Normalized cache](https://www.apollographql.com/docs/kotlin/caching/normalized-cache)
    * make sure to invalidate caches after edits are made
* README