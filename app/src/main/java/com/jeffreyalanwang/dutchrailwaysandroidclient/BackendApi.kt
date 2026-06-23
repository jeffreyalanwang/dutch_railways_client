@file:Suppress("FunctionName") // use underscores for BackendApi methods

package com.jeffreyalanwang.dutchrailwaysandroidclient
import android.content.res.Resources
import com.google.android.gms.maps.model.LatLng
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import java.time.ZoneId
import java.util.EnumSet
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
        PassService(119, "Intercity 2263 to Rotterdam Centraal", Trainset.VIRM, EnumSet.allOf(TrainAmenity::class.java))
    )
    private val dummyServiceStops = mutableListOf(
        ServiceStop(passServiceId=119,arrival=null                                      ,departure=parseAmsTime("2026-05-08T18:36:00.000000"),stationId=358),
        ServiceStop(passServiceId=119,arrival=parseAmsTime("2026-05-08T19:28:00.000000"),departure=parseAmsTime("2026-05-08T19:30:00.000000"),stationId=376),
        ServiceStop(passServiceId=119,arrival=parseAmsTime("2026-05-08T19:49:00.000000"),departure=null                                      ,stationId=361),
    )
    private val dummyAreas = mutableListOf(
        Area(1, "Nederland"),
        Area(10, "Noord-Holland"),
        Area(9, "Zuid-Holland"),
        Area(319, "Rotterdam"),
        Area(287, "'s-Gravenhage"),
        Area(145, "Amsterdam"),
    )
    private val dummyStations = mutableListOf(
        Station(358, "Amsterdam Centraal", "5a, IJ-hal, Centrum, Amsterdam, Noord-Holland, Nederland, 1012 AA, Nederland", LatLng(52.37888718232718, 4.900277682877522)),
        Station(361, "Rotterdam Centraal", "Spoor 8, Stationssingel, Provenierswijk, Noord, Rotterdam, Zuid-Holland, Nederland, 3033 HB, Nederland", LatLng(51.92499923833714, 4.468888827643443)),
        Station(376, "Den Haag HS", "Stationsplein, Stationsbuurt, Centrum, Den Haag, Zuid-Holland, Nederland, 2515 RT, Nederland", LatLng(52.06972122391006, 4.322500294829242)),
    )

    fun <T: Place> autocomplete_place(cls: KClass<T>, query: String): List<T> { //TODO this should not be loading entire stations. just the data we need
        val candidates = ArrayList<Pair<Place, Double>>()

        if (cls.java.isAssignableFrom(Station::class.java)) {
            candidates.addAll(dummyStations.map {  Pair(it, max(
                fuzzratio(query, it.name),
                fuzzratio(query, it.address),
            ))})
        }

        if (cls.java.isAssignableFrom(Area::class.java)) {
            candidates.addAll(dummyAreas.map { Pair(it, fuzzratio(query, it.name)) })
        }

        return candidates.sortedByDescending { it.second }.take(10).map { it.first } as List<T>
    }

    fun autocomplete_pass_service(query: String): List<PassService> {
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

    fun get_pass_service(id: Int): PassService {
        dummyPassServices.forEach {
            if (it.id == id) return it
        }
        throw Resources.NotFoundException("Id not found: $id")
    }

    fun get_area_info(id: Int): Area {
        dummyAreas.forEach {
            if (it.id == id) return it
        }
        throw Resources.NotFoundException("Id not found: $id")
    }

    fun get_station_info(id: Int): Station {
        dummyStations.forEach {
            if (it.id == id) return it
        }
        throw Resources.NotFoundException("Id not found: $id")
    }

    fun get_place_info(id: Int): Place {
        dummyStations.forEach {
            if (it.id == id) return it
        }
        dummyAreas.forEach {
            if (it.id == id) return it
        }
        throw Resources.NotFoundException("Id not found: $id")
    }


    fun get_journeys(
        origin: Place,
        destination: Place,
        departTime: Instant? = null,
        arriveTime: Instant? = null,
    ) = sequence<Journey> {
        // as a dummy fixture, we only have the one pass service.
        // we simply check if it fits the requested parameters

        check(origin is Station)
        check(destination is Station)

        for (service in dummyPassServices) {
            val (departureStop, arrivalStop) = dummyServiceStops
                .filter { it.passServiceId == service.id }
                .let {
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
                yield(Journey(persistentListOf(departureStop, arrivalStop)))
            }
        }
    }

    private fun find_best_station(it: Place)
        = when(it) {
            is Station -> listOf(it)
            is Area -> stations_in_area(it)
            else -> emptyList()
        }.map { Pair(0u, it) }

    internal fun stations_in_area(it: Area)
        = when(it) {
            Area(id = 1, "Nederland") -> dummyStations
            Area(10, "Noord-Holland") -> listOf(dummyStations[0])
            Area(9, "Zuid-Holland") -> dummyStations.subList(1, 3)
            Area(319, "Rotterdam") -> listOf(dummyStations[1])
            Area(287, "'s-Gravenhage") -> listOf(dummyStations[2])
            Area(145, "Amsterdam") -> listOf(dummyStations[0])
            else -> emptyList()
        }

    fun get_nl_area() = get_area_info(1)

    // Must sort by arrival time before return

    fun get_stops_of_service(service_id: Int): List<ServiceStop>
        = dummyServiceStops
            .filter { it.passServiceId == service_id }
            .sortedBy { it.arrival }
    fun get_stops_of_service(service: PassService) = get_stops_of_service(service.id)

    fun get_stops_at_station(station_id: Int): List<ServiceStop>
        = dummyServiceStops
            .filter { it.stationId == station_id }
            .sortedBy { it.arrival }
    fun get_stops_at_station(station: Station) = get_stops_at_station(station.id)

    fun get_stops(service_id: Int, station_id: Int): ServiceStop
        = dummyServiceStops
            .filter { it.passServiceId == service_id }
            .filter { it.stationId == station_id }
            .first()

    fun edit_station(id: Int, name: String? = null, address: String? = null, geom: LatLng? = null) {
        val index = dummyStations.indexOfFirst { it.id == id }
        require(index > 0)

        val old = dummyStations[index]
        dummyStations[index] = Station(
            id = id,
            name = name ?: old.name,
            address = address ?: old.address,
            geom = geom ?: old.geom
        )
    }

    fun edit_area(id: Int, name: String? = null) {
        val index = dummyAreas.indexOfFirst { it.id == id }
        require(index > 0)
        val old = dummyAreas[index]
        dummyAreas[index] = Area(
            id = id,
            name = name ?: old.name
        )
    }

    fun add_pass_service(title: String, trainset: Trainset, amenities: EnumSet<TrainAmenity>, stops: List<ServiceStop>)
        = PassService(
            id = (dummyPassServices.maxOfOrNull { it.id } ?: -1) + 1,
            title,
            trainset,
            amenities,
            stops
        ).also { dummyPassServices.add(it) }

    fun delete_pass_service(id: Int) {
        dummyServiceStops.removeAll { it.passServiceId == id }
        dummyPassServices.removeAll { it.id == id }
    }

    fun update_pass_service(
        serviceId: Int,
        trainset: Trainset? = null,
        amenities: EnumSet<TrainAmenity>? = null,
        stops: List<ServiceStop>? = null,
    ) {
        val serviceIndex = dummyPassServices.indexOfFirst { it.id == serviceId }
        val oldService = dummyPassServices[serviceIndex]
        val oldStops = dummyServiceStops.filter { it.passServiceId == serviceId }

        val newTitle = oldService.title.let { oldTitle ->
            oldTitle
            .removeSuffix(oldStops.lastStationName())
            .let {
                if (it == oldTitle) null
                else if (stops == null) null
                else it + stops.lastStationName()
            }
        }

        if (stops != null) {
            check_stops_consistency(stops)
            dummyServiceStops.removeAll { it.passServiceId == serviceId }
            dummyServiceStops.addAll(stops)
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
     * @param stops: List of stops for one [PassService]. Does not need to be sorted.
     */
    fun check_stops_consistency(stops: List<ServiceStop>) {
        require(stops.map { it.passServiceId }.toSet().size == 1 )
        require(stops.map { it.stationId }.toSet().size == stops.size )
        require(
            stops.sortedWith( compareBy(nullsFirst()) { it.arrival } )
                .zipWithNext()
                .all { (a, b) -> a.departure!! < b.arrival!! }
        )
    }
}