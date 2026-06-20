* implement amenities floaters
* implement edit amenities
* if possible, remove TripFinderGraphChildNavArgs
* "place as abstract class?"
* sealed class to simplify linting? (when-statements would be exhaustive)
* C/U/D functionality: in BackendApi and in new tab Composable
  * edit places
    * edit stations
      * edit name
      * edit address
      * edit location point
    * edit areas
      * edit name
  * edit passServices
    * add passServices
      * create new
      * duplicate + add time-offset
    * "delete" passServices
      * delete only the stops that have not yet occurred.
      * if all stops are deleted, delete the entire passService 
    * edit stops
      * delete single stop (if not in the past)
      * add new stop
      * modify a stop's arrive or depart time (even if it was in the past)
        and offer to push the depart + all prochains times up/down by the same time delta
  * enforce a "lock" button to enable user/admin mode (needs to be able to modify backend requests later)
* [Apollo](https://www.apollographql.com/docs/kotlin)
  * [Schema](https://www.apollographql.com/docs/apollo-server/schema/schema) with nullable fields 
  * [Mock responses](https://www.apollographql.com/docs/kotlin/testing/mocking-graphql-responses)
  * [Normalized cache](https://www.apollographql.com/docs/kotlin/caching/normalized-cache)
    * make sure to invalidate caches after edits are made
* README