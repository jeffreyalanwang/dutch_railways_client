package com.jeffreyalanwang.dutchrailwaysandroidclient

import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Instant


internal fun List<Pair<ServiceStop, ServiceStop>>.edgesWithin(
    earliest: Instant? = null,
    latest: Instant? = null,
) = this
    .letIf (earliest != null) {
        it.filter { (from, to) ->
            from.departure!! >= earliest!!
        }
    }
    .letIf (latest != null) {
        it.filter { (from, to) ->
            to.arrival!! <= latest!!
        }
    }

internal fun List<Pair<ServiceStop, ServiceStop>>.edgesBetween(
    fromStationId: Int? = null,
    toStationId: Int? = null,
) = this
    .letIf (fromStationId != null) {
        it.filter { (from, to) ->
            from.stationId == fromStationId
        }
    }
    .letIf (toStationId != null) {
        it.filter { (from, to) ->
            to.stationId == toStationId
        }
    }

internal fun List<Pair<ServiceStop, ServiceStop>>.edgesNotBetween(
    fromStationId: Int? = null,
    toStationId: Int? = null,
) = this
    .letIf (fromStationId != null) {
        it.filter { (from, to) ->
            from.stationId != fromStationId
        }
    }
    .letIf (toStationId != null) {
        it.filter { (from, to) ->
            to.stationId != toStationId
        }
    }


internal fun get_journeys_min_total_time(
    origin: Int,
    destination: Int,
    edges: List<Pair<ServiceStop, ServiceStop>>,
): Sequence<Journey> {
    val firstEdgeOptions = edges.edgesBetween(fromStationId = origin)

    val byFirstEdge = firstEdgeOptions.map { edge ->
        get_journeys_min_arrival_time(
            edge.second.stationId,
            destination,
            edges
                .edgesWithin(
                    earliest = edge.second.arrival!!.toKotlinInstant()
                )
                .edgesNotBetween(fromStationId = origin)
        ).map {
            edge.first + it
        }
    }

    return byFirstEdge.flattenSorted {
        it.stops.first().departure!! - it.stops.last().arrival!!
    }
}


private fun get_journeys_min_arrival_time(
    origin: Int,
    destination: Int,
    edges: List<Pair<ServiceStop, ServiceStop>>
) = sequence<Journey> {
    var _edges = edges

    // minimize final arrival time.

    val tentativeEarliestArrival: MutableMap<
        Int,
        Pair<
            Pair<ServiceStop, ServiceStop>,
            Instant,
        >,
    > = mutableMapOf()

    val finalEarliestArrival: MutableMap<
        Int,
        Pair<
            Pair<ServiceStop, ServiceStop>,
            Instant,
        >,
    > = mutableMapOf()

    // Rather than cumulative traversed weight, we can just use arrival time of last stop.
    // Graph weights change based on previous stops for a given path.
    // However, we can skip all paths with later arrivals to a station.

    var currStationId: Int? = origin
    while (currStationId != null) {
        val (currFinalEdge, currEndTime) = tentativeEarliestArrival[currStationId]!!

        // We have found the path that gets to this station the earliest
        tentativeEarliestArrival.remove(currStationId)
        finalEarliestArrival[currStationId] = currFinalEdge to currEndTime
        if (currStationId == destination) {
            yield(
                trace_edges(
                    origin,
                    destination,
                    finalEarliestArrival
                        .mapValues { (k, v) -> v.first }
                )
            )
            return@sequence
        }
        _edges = _edges.edgesNotBetween(toStationId = currStationId)

        val nextEdges = _edges
            .edgesBetween(fromStationId = currStationId)
            .edgesWithin(earliest = currEndTime)
        _edges = _edges.edgesNotBetween(fromStationId = currStationId)

        for ((from, to) in nextEdges) {

            // We are sure we do not have edges to a finalized node
            if (
                to.stationId !in tentativeEarliestArrival.keys ||
                (tentativeEarliestArrival[to.stationId]!!.second
                    > to.arrival!!)
            ) {
                tentativeEarliestArrival[to.stationId] = Pair(
                    Pair(from, to),
                    to.arrival!!.toKotlinInstant()
                )
            }
        }

        currStationId = tentativeEarliestArrival
            .minByOrNull { (stationId, v) -> v.second }
            ?.let { (stationId, v) -> stationId }
    }
}

private fun trace_edges(
    origin: Int,
    destination: Int,
    lastEdgeByDestination: Map<Int, Pair<ServiceStop, ServiceStop>>,
): Journey {
    val buf = ArrayDeque<Pair<ServiceStop, ServiceStop>>()

    var addNext: Int = destination
    while (addNext != origin) { // we need to add an edge arriving at [addNext]
        buf.addFirst(
            lastEdgeByDestination[addNext]!!
        )
        addNext = buf.first() // the last-added edge
            .component1()     // its origin stop
            .stationId
    }

    val flattened = buf
        .groupByContinuous { it.first.passServiceId }
        .map { (key, values) ->
            listOf(values.first().first, values.last().second)
        }
        .flatten()

    return Journey(flattened.toImmutableList())
}

//fun get_journeys(
//    origin: Place,
//    destination: Place,
//    departTime: Instant? = null,
//    arriveTime: Instant? = null,
//): Sequence<Journey> {
//    // These may be in order of distance in cm, if origin or destination is a LatLng
//    val originStationOptions: List<Pair<UInt, Station>> =
//        BackendApi.find_best_station(origin)
//    val destinationStationOptions: List<Pair<UInt, Station>> =
//        BackendApi.find_best_station(destination)
//
//    // Sorted to minimize total distance between
//    // requested origin/destination and the stations
//    // (prioritizing destination distance)
//    val endpointMatrix: Sequence<Pair<Station, Station>>
//        = timesSorted(
//            destinationStationOptions,
//            originStationOptions,
//            selector = { it.first },
//            combine = { a, b -> a + b },
//        )
//        .map { (a, b) -> b to a }
//        .map { (a, b) -> a.second to b.second }
//
//    val stationEdges = listOf(BackendApi.dummyService)
//        .flatMap {  service ->
//            BackendApi.dummyServiceStops
//                .filter { stop ->
//                    stop.passServiceId == service.id
//                }
//                .zipWithNext()
//                .edgesWithin(departTime, arriveTime)
//        }
//    val journeys: Sequence<Journey>
//        = endpointMatrix.map { (originStation, destinationStation) ->
//            get_journeys_min_total_time(originStation.id, destinationStation.id, stationEdges)
//        }
//        .toList()
//        .flattenRoundRobin()
//
//    return journeys
//}