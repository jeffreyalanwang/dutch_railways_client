* C/U/D functionality: in BackendApi and in new tab Composable
  * edit places
    * edit stations
      * edit name
      * edit address
      * edit location point
    * edit areas
      * edit name
    -> latlng or address: search placeholder token + expandedsearchbar
    -> transitionable between edit station screen + fullscreen GoogleMap (with movableContentOf?)
  * organize ...ui.screens.child
  * enforce a "lock" button to enable user/admin mode (needs to be able to modify backend requests later)
* [Apollo](https://www.apollographql.com/docs/kotlin)
  * [Schema](https://www.apollographql.com/docs/apollo-server/schema/schema) with nullable fields 
  * [Mock responses](https://www.apollographql.com/docs/kotlin/testing/mocking-graphql-responses)
  * [Normalized cache](https://www.apollographql.com/docs/kotlin/caching/normalized-cache)
    * make sure to invalidate caches after edits are made
* README