@file:Suppress("FunctionName") // use underscores for BackendApi methods

package com.jeffreyalanwang.dutchrailwaysandroidclient.backend
import android.content.res.Resources
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.jeffreyalanwang.dutchrailwaysandroidclient.compareTo
import com.jeffreyalanwang.dutchrailwaysandroidclient.lastStationName
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import java.time.ZoneId
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.time.Instant
import ca.solostudios.fuzzykt.FuzzyKt.ratio as fuzzratio


private fun parseAmsTime(s: String)
    = LocalDateTime.parse(s)
        .toJavaLocalDateTime()
        .atZone(ZoneId.of("Europe/Amsterdam"))

//val apolloClient = ApolloClient.Builder()
//    .serverUrl("https://example.com/graphql")
//    .httpBatching()
//    .build()

object BackendApi {
    private const val BACKEND_URL = "http://msword-jw125.duckdns.org"

    private val dummyPassServices = mutableListOf(
        _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.PassService(
            119,
            "Intercity 2263 to Rotterdam Centraal",
            _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Trainset.VIRM,
            _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity.entries.toSet()
        )
    )
    private val dummyServiceStops = mutableListOf(
        _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop(
            passServiceId = 119,
            arrival = null,
            departure = parseAmsTime("2026-05-08T18:36:00.000000"),
            stationId = 358
        ),
        _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop(
            passServiceId = 119,
            arrival = parseAmsTime("2026-05-08T19:28:00.000000"),
            departure = parseAmsTime("2026-05-08T19:30:00.000000"),
            stationId = 376
        ),
        _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop(
            passServiceId = 119,
            arrival = parseAmsTime("2026-05-08T19:49:00.000000"),
            departure = null,
            stationId = 361
        ),
    )
    private val dummyAreas = mutableListOf(
        _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Area(
            1,
            "Nederland"
        ),
        _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Area(
            10,
            "Noord-Holland"
        ),
        _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Area(
            9,
            "Zuid-Holland"
        ),
        _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Area(
            319,
            "Rotterdam"
        ),
        _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Area(
            287,
            "'s-Gravenhage"
        ),
        _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Area(
            145,
            "Amsterdam"
        ),
    )
    private val dummyStations = mutableListOf(
        _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Station(
            358,
            "Amsterdam Centraal",
            "5a, IJ-hal, Centrum, Amsterdam, Noord-Holland, Nederland, 1012 AA, Nederland",
            LatLng(52.37888718232718, 4.900277682877522)
        ),
        _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Station(
            361,
            "Rotterdam Centraal",
            "Spoor 8, Stationssingel, Provenierswijk, Noord, Rotterdam, Zuid-Holland, Nederland, 3033 HB, Nederland",
            LatLng(51.92499923833714, 4.468888827643443)
        ),
        _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Station(
            376,
            "Den Haag HS",
            "Stationsplein, Stationsbuurt, Centrum, Den Haag, Zuid-Holland, Nederland, 2515 RT, Nederland",
            LatLng(52.06972122391006, 4.322500294829242)
        ),
    )

    fun <T: com.jeffreyalanwang.dutchrailwaysandroidclient.Place> autocomplete_place(cls: KClass<T>, query: String): List<T> { //TODO this should not be loading entire stations. just the data we need
        Log.d("BackendApi", "autocomplete_place: cls=$cls, query=$query")
        val candidates = ArrayList<Pair<com.jeffreyalanwang.dutchrailwaysandroidclient.Place, Double>>()

        if (cls.java.isAssignableFrom(_root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Station::class.java)) {
            candidates.addAll(dummyStations.map {  Pair(it, max(
                fuzzratio(query, it.name),
                fuzzratio(query, it.address),
            ))})
        }

        if (cls.java.isAssignableFrom(_root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Area::class.java)) {
            candidates.addAll(dummyAreas.map { Pair(it, fuzzratio(query, it.name)) })
        }

        return candidates.sortedByDescending { it.second }.take(10).map { it.first } as List<T>
    }

    fun autocomplete_pass_service(query: String): List<com.jeffreyalanwang.dutchrailwaysandroidclient.PassService> {
        Log.d("BackendApi", "autocomplete_pass_service: query=$query")
        return dummyPassServices
            .sortedByDescending {
                maxOf(
                    fuzzratio(query, it.title),
                    fuzzratio(query, AppStringFormats.Time(it.getStops().first().departure!!)),
                    fuzzratio(query, AppStringFormats.Time(it.getStops().last().arrival!!)),
                    fuzzratio(query, it.trainset.name),
                )
            }
    }

    fun get_pass_service(id: Int): com.jeffreyalanwang.dutchrailwaysandroidclient.PassService {
        Log.d("BackendApi", "get_pass_service: id=$id")
        dummyPassServices.forEach {
            if (it.id == id) return it
        }
        throw Resources.NotFoundException("Id not found: $id")
    }

    fun get_area_info(id: Int): com.jeffreyalanwang.dutchrailwaysandroidclient.Area {
        Log.d("BackendApi", "get_area_info: id=$id")
        dummyAreas.forEach {
            if (it.id == id) return it
        }
        throw Resources.NotFoundException("Id not found: $id")
    }

    fun get_station_info(id: Int): com.jeffreyalanwang.dutchrailwaysandroidclient.Station {
        Log.d("BackendApi", "get_station_info: id=$id")
        dummyStations.forEach {
            if (it.id == id) return it
        }
        throw Resources.NotFoundException("Id not found: $id")
    }

    fun get_place_info(id: Int): com.jeffreyalanwang.dutchrailwaysandroidclient.Place {
        Log.d("BackendApi", "get_place_info: id=$id")
        dummyStations.forEach {
            if (it.id == id) return it
        }
        dummyAreas.forEach {
            if (it.id == id) return it
        }
        throw Resources.NotFoundException("Id not found: $id")
    }


    fun get_journeys(
        origin: com.jeffreyalanwang.dutchrailwaysandroidclient.Place,
        destination: com.jeffreyalanwang.dutchrailwaysandroidclient.Place,
        departTime: Instant? = null,
        arriveTime: Instant? = null,
    ) = sequence {
        Log.d("BackendApi", "get_journeys: origin=$origin, destination=$destination, departTime=$departTime, arriveTime=$arriveTime")
        // as a dummy fixture, we only have the one pass service.
        // we simply check if it fits the requested parameters

        check(origin is com.jeffreyalanwang.dutchrailwaysandroidclient.Station)
        check(destination is com.jeffreyalanwang.dutchrailwaysandroidclient.Station)

        for (service in dummyPassServices) {
            val (departureStop, arrivalStop) = dummyServiceStops
                .filter { it.passServiceId == service.id }
                .let { it ->
                    it.firstOrNull {
                        it.stationId == origin.id
                    } to it.firstOrNull {
                        it.stationId == destination.id
                    }
                }

            if (departureStop == null || arrivalStop == null) {
                continue
            } else if (departureStop.departure == null || arrivalStop.arrival == null) {
                continue
            } else if (departureStop.departure >= arrivalStop.arrival) {
                continue
            } else if (departTime != null && departTime > departureStop.departure) {
                continue
            } else if (arriveTime != null && arriveTime < arrivalStop.arrival) {
                continue
            } else {
                yield(
                    _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Journey(
                        persistentListOf(departureStop, arrivalStop)
                    )
                )
            }
        }
    }

    private fun find_best_station(it: com.jeffreyalanwang.dutchrailwaysandroidclient.Place)
        = when(it) {
            is com.jeffreyalanwang.dutchrailwaysandroidclient.Station -> listOf(it)
            is com.jeffreyalanwang.dutchrailwaysandroidclient.Area -> stations_in_area(it)
            else -> emptyList()
        }.map { Pair(0u, it) }
        .also {
            Log.d("BackendApi", "find_best_station: it=$it, result=$it")
        }

    internal fun stations_in_area(it: com.jeffreyalanwang.dutchrailwaysandroidclient.Area)
        = when(it) {
            _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Area(
                id = 1,
                "Nederland"
            ) -> dummyStations
            _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Area(
                10,
                "Noord-Holland"
            ) -> listOf(dummyStations[0])
            _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Area(
                9,
                "Zuid-Holland"
            ) -> dummyStations.subList(1, 3)
            _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Area(
                319,
                "Rotterdam"
            ) -> listOf(dummyStations[1])
            _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Area(
                287,
                "'s-Gravenhage"
            ) -> listOf(dummyStations[2])
            _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Area(
                145,
                "Amsterdam"
            ) -> listOf(dummyStations[0])
            else -> emptyList()
        }.also {
            Log.d("BackendApi", "stations_in_area: it=$it, resultCount=${it.size}")
        }

    fun get_nl_area(): com.jeffreyalanwang.dutchrailwaysandroidclient.Area {
        Log.d("BackendApi", "get_nl_area")
        return get_area_info(1)
    }

    // Must sort by arrival time before return

    fun get_stops_of_service(service_id: Int): List<com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop> {
        Log.d("BackendApi", "get_stops_of_service: service_id=$service_id")
        return dummyServiceStops
            .filter { it.passServiceId == service_id }
            .sortedBy { it.arrival }
    }

    fun get_stops_of_service(service: com.jeffreyalanwang.dutchrailwaysandroidclient.PassService): List<com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop> {
        Log.d("BackendApi", "get_stops_of_service: service=$service")
        return get_stops_of_service(service.id)
    }

    fun get_stops_at_station(station_id: Int): List<com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop> {
        Log.d("BackendApi", "get_stops_at_station: station_id=$station_id")
        return dummyServiceStops
            .filter { it.stationId == station_id }
            .sortedBy { it.arrival }
    }

    fun get_stops_at_station(station: com.jeffreyalanwang.dutchrailwaysandroidclient.Station): List<com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop> {
        Log.d("BackendApi", "get_stops_at_station: station=$station")
        return get_stops_at_station(station.id)
    }

    fun get_stops(service_id: Int, station_id: Int): com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop {
        Log.d("BackendApi", "get_stops: service_id=$service_id, station_id=$station_id")
        return dummyServiceStops
            .filter { it.passServiceId == service_id }
            .first { it.stationId == station_id }
    }

    fun edit_station(id: Int, name: String? = null, address: String? = null, geom: LatLng? = null) {
        Log.d("BackendApi", "SAVING: edit_station: id=$id, name=$name, address=$address, geom=$geom")
        val index = dummyStations.indexOfFirst { it.id == id }
        require(index > 0)

        val old = dummyStations[index]
        dummyStations[index] =
            _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Station(
                id = id,
                name = name ?: old.name,
                address = address ?: old.address,
                geom = geom ?: old.geom
            )
    }

    fun edit_area(id: Int, name: String? = null) {
        Log.d("BackendApi", "SAVING: edit_area: id=$id, name=$name")
        val index = dummyAreas.indexOfFirst { it.id == id }
        require(index > 0)
        val old = dummyAreas[index]
        dummyAreas[index] =
            _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.Area(
                id = id,
                name = name ?: old.name
            )
    }

    fun add_pass_service(title: String, trainset: com.jeffreyalanwang.dutchrailwaysandroidclient.Trainset, amenities: Set<com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity>, stops: List<com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop>): com.jeffreyalanwang.dutchrailwaysandroidclient.PassService {
        Log.d("BackendApi", "SAVING: add_pass_service: title=$title, trainset=$trainset, amenities=$amenities, stopCount=${stops.size}")

        val id = (dummyPassServices.maxOfOrNull { it.id } ?: -1) + 1
        val newService =
            _root_ide_package_.com.jeffreyalanwang.dutchrailwaysandroidclient.PassService(
                id,
                title,
                trainset,
                amenities,
            )
        val stops = stops
            .map { it.copy(passServiceId = id) }

        dummyServiceStops.addAll(stops)
        return newService.also { dummyPassServices.add(it) }
    }

    fun delete_pass_service(id: Int) {
        Log.d("BackendApi", "SAVING: delete_pass_service: id=$id")
        dummyServiceStops.removeAll { it.passServiceId == id }
        dummyPassServices.removeAll { it.id == id }
    }

    fun update_pass_service(
        serviceId: Int,
        trainset: com.jeffreyalanwang.dutchrailwaysandroidclient.Trainset? = null,
        amenities: Set<com.jeffreyalanwang.dutchrailwaysandroidclient.TrainAmenity>? = null,
        stops: List<com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop>? = null,
    ) {
        Log.d("BackendApi", "SAVING: update_pass_service: serviceId=$serviceId, trainset=$trainset, amenities=$amenities, stopCount=${stops?.size ?: "null"}")
        val serviceIndex = dummyPassServices.indexOfFirst { it.id == serviceId }
        val oldService = dummyPassServices[serviceIndex]
        val oldStops = dummyServiceStops.filter { it.passServiceId == serviceId }

        val newTitle = oldService.title.let { oldTitle ->
            oldTitle
            .removeSuffix(oldStops.lastStationName() ?: "")
            .let {
                if (it == oldTitle) null
                else if (stops == null) null
                else it + stops.lastStationName()
            }
        }

        if (stops != null) {
            check_stops_consistency(stops)
            dummyServiceStops.removeAll { it.passServiceId == serviceId }
            stops.map { it.copy(passServiceId = serviceId) }
                .let { dummyServiceStops.addAll(it) }
        }

        if (trainset != null || amenities != null || newTitle != null) {
            dummyPassServices[serviceIndex] = oldService.copy(
                title = newTitle ?: oldService.title,
                trainset = trainset ?: oldService.trainset,
                amenities = amenities ?: oldService.amenities,
            )
        }
    }

    /**
     * Throw [IllegalArgumentException] if stations or stop times overlap.
     * @param stops: List of stops for one [com.jeffreyalanwang.dutchrailwaysandroidclient.PassService]. Does not need to be sorted.
     */
    fun check_stops_consistency(stops: List<com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop>) {
        Log.d("BackendApi", "check_stops_consistency: stopCount=${stops.size}")
        require(stops.map { it.passServiceId }.toSet().size == 1 )
        require(stops.map { it.stationId }.toSet().size == stops.size )
        require(
            stops.sortedWith( compareBy(nullsFirst()) { it.arrival } )
                .zipWithNext()
                .all { (a, b) -> a.departure!! < b.arrival!! }
        )
    }
}