package com.jeffreyalanwang.dutchrailwaysandroidclient
import android.content.res.Resources
import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.google.android.gms.maps.model.LatLng
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.EnumSet
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import ca.solostudios.fuzzykt.FuzzyKt.ratio as fuzzratio


enum class TrainsetQuality {OLD, NEW}
enum class Trainset(val quality: TrainsetQuality) {
    SLT(TrainsetQuality.OLD),
    ICM(TrainsetQuality.OLD),
    DDZ(TrainsetQuality.OLD),
    VIRM(TrainsetQuality.OLD),
    SNG(TrainsetQuality.NEW),
    ICNG(TrainsetQuality.NEW),
    GTW(TrainsetQuality.NEW),
    Flirt(TrainsetQuality.NEW),
}
enum class TrainAmenity(val friendlyName: String) {
    STROOM("Power outlets"),
    TOILET("Restrooms"),
    WIFI("Wi-Fi"),
    STILTE("Quiet car"),
    FIETS("Bicycle stowage"),
    TOEGANKELIJK("Accessible"),
    unknown("Unknown")
}

@Immutable
data class RoutePlan(
    val stops: ImmutableList<ServiceStop>
) {
    val transferCount: Int
        get() = stops
            .distinctBy { it.passServiceId }
            .drop(1)
            .count()

    val duration: Duration
        get() = stops.last().arrival!! - stops.first().departure!!

    fun stopsByLayover() = stops.byLayover { it.stationId }
    fun stopsByLeg() = stops.byLeg { it.passServiceId }
    companion object {
        fun <T> Iterable<T>.byLayover(
            stationIdSelector: (T) -> Int
        ) = this
            .groupByContinuous(stationIdSelector)
            .map { (stationId, stops) -> stops }

        fun <T> Iterable<T>.byLeg(
            passServiceIdSelector: (T) -> Int
        ) = this
            .groupByContinuous(passServiceIdSelector)
            .map { (passServiceId, stops) -> stops.toPair() }
    }

    val origin
        get() = stops.first().getStation()
    val destination
        get() = stops.last().getStation()
    val departTime
        get() = stops.first().departure!!
    val arriveTime
        get() = stops.last().arrival!!
}

operator fun RoutePlan.plus(other: ServiceStop)
    = RoutePlan((this.stops + other).toImmutableList())

operator fun ServiceStop.plus(other: RoutePlan)
    = RoutePlan(other.stops.plusInsert(0, this).toImmutableList())

@Parcelize
open class Place(
    val id: Int,
    val name: String
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Place) return false

        return this.id == other.id
    }

    override fun hashCode(): Int
        = id.hashCode()
}

@Parcelize
class PolygonData(
    val points: List<LatLng>,
    val holes: List<List<LatLng>> = emptyList(),
) : Parcelable

@Parcelize
class Area(
    id: Int,
    name: String,
) : Place(id, name), Parcelable {
    fun getStations(): List<Station>
        = BackendApi.stations_in_area(this)
    fun getGeom(): PolygonData
        = PolygonData(listOf(
            LatLng(52.3971230, 4.9060153),
            LatLng(52.3935615, 4.8505417),
            LatLng(52.3582454, 4.8246081),
            LatLng(52.3381121, 4.8757882),
            LatLng(52.3460826, 4.9494669),
            LatLng(52.3841327, 4.9474059),
            LatLng(52.3971230, 4.9060153),
        ))
}

@Parcelize
class Station(
    id: Int,
    name: String,
    val address: String,
    val geom: LatLng,
    private var stops: List<ServiceStop>? = null,
) : Place(id, name), Parcelable {
    fun getStops(): List<ServiceStop> {
        if (stops == null) {
            stops = BackendApi.get_stops_at_station(this)
        }
        return stops!!
    }
}

@Parcelize
@Immutable
class ServiceStop(
    val arrival: ZonedDateTime?,
    val departure: ZonedDateTime?,
    val passServiceId: Int,
    val stationId: Int,
) : Parcelable {
    @IgnoredOnParcel private var passService: PassService? = null
    @IgnoredOnParcel private var station: Station? = null

    constructor(arrival: ZonedDateTime, departure: ZonedDateTime, passService: PassService, station: Station)
            : this(arrival, departure, passServiceId=passService.id, stationId=station.id) {
        this.passService = passService
        this.station = station
    }
    constructor(arrival: ZonedDateTime, departure: ZonedDateTime, passServiceId: Int, station: Station)
            : this(arrival, departure, passServiceId=passServiceId, stationId=station.id) {
        this.station = station
    }
    constructor(arrival: ZonedDateTime, departure: ZonedDateTime, passService: PassService, stationId: Int)
            : this(arrival, departure, passServiceId=passService.id, stationId=stationId) {
        this.passService = passService
    }

    fun getStation(): Station {
        if (station == null) {
            station = BackendApi.get_station_info(stationId)
        }
        return station!!
    }
    fun getService(): PassService {
        if (passService == null) {
            passService = BackendApi.get_pass_service(passServiceId)
        }
        return passService!!
    }
}

@Parcelize
class PassService(
    val id: Int,
    val title: String,
    val trainset: Trainset,
    val amenities: EnumSet<TrainAmenity>,
    private var stops: List<ServiceStop>? = null,
) : Parcelable {
    fun getStops(): List<ServiceStop> {
        if (stops == null) {
            stops = BackendApi.get_stops_of_service(this)
        }
        return stops!!
    }
}


private fun parseAmsTime(s: String)
    = LocalDateTime.parse(s)
        .toJavaLocalDateTime()
        .atZone(ZoneId.of("Europe/Amsterdam"))

//val apolloClient = ApolloClient.Builder()
//    .serverUrl("https://example.com/graphql")
//    .httpBatching()
//    .build()

object BackendApi {
    private const val BACKEND_URL = "http://msword-jw125.duckdns.org";

    private val dummyService = PassService(119, "Intercity 2263 to Rotterdam Centraal", Trainset.VIRM, EnumSet.allOf(TrainAmenity::class.java))
    private val dummyServiceStops = listOf(
        ServiceStop(passServiceId=119,arrival=null                                      ,departure=parseAmsTime("2026-05-08T18:36:00.000000"),stationId=358),
        ServiceStop(passServiceId=119,arrival=parseAmsTime("2026-05-08T19:28:00.000000"),departure=parseAmsTime("2026-05-08T19:30:00.000000"),stationId=376),
        ServiceStop(passServiceId=119,arrival=parseAmsTime("2026-05-08T19:49:00.000000"),departure=null                                      ,stationId=361),
    )
    private val dummyAreas = listOf(
        Area(1, "Nederland"),
        Area(10, "Noord-Holland"),
        Area(9, "Zuid-Holland"),
        Area(319, "Rotterdam"),
        Area(287, "'s-Gravenhage"),
        Area(145, "Amsterdam"),
    )
    private val dummyStations = listOf(
        Station(358, "Amsterdam Centraal", "5a, IJ-hal, Centrum, Amsterdam, Noord-Holland, Nederland, 1012 AA, Nederland", LatLng(52.37888718232718, 4.900277682877522)),
        Station(361, "Rotterdam Centraal", "Spoor 8, Stationssingel, Provenierswijk, Noord, Rotterdam, Zuid-Holland, Nederland, 3033 HB, Nederland", LatLng(51.92499923833714, 4.468888827643443)),
        Station(376, "Den Haag HS", "Stationsplein, Stationsbuurt, Centrum, Den Haag, Zuid-Holland, Nederland, 2515 RT, Nederland", LatLng(52.06972122391006, 4.322500294829242)),
    )

    fun <T: Place> autocomplete_place(cls: KClass<T>, query: String): List<Place> { //TODO this should not be loading entire stations. just the data we need
        val candidates = ArrayList<Pair<Place, Double>>();

        if (cls.java.isAssignableFrom(Station::class.java)) {
            candidates.addAll(dummyStations.map {  Pair(it, max(
                fuzzratio(query, it.name),
                fuzzratio(query, it.address),
            ))})
        }

        if (cls.java.isAssignableFrom(Area::class.java)) {
            candidates.addAll(dummyAreas.map { Pair(it, fuzzratio(query, it.name)) })
        }

        return candidates.sortedByDescending{ it.second }.take(10).map{ it.first }
    }

    fun get_pass_service(id: Int): PassService {
        assert(id == dummyService.id)
        return dummyService
    }

    fun get_area_info(id: Int): Area {
        dummyAreas.forEach {
            if (it.id == id) return it
        }
        throw Resources.NotFoundException("Id not found: $id");
    }

    fun get_station_info(id: Int): Station {
        dummyStations.forEach {
            if (it.id == id) return it
        }
        throw Resources.NotFoundException("Id not found: $id");
    }

    fun get_place_info(id: Int): Place {
        dummyStations.forEach {
            if (it.id == id) return it
        }
        dummyAreas.forEach {
            if (it.id == id) return it
        }
        throw Resources.NotFoundException("Id not found: $id");
    }


    fun get_routes(
        origin: Place,
        destination: Place,
        departTime: Instant? = null,
        arriveTime: Instant? = null,
    ) = sequence<RoutePlan> {
        // as a dummy fixture, we only have the one pass service.
        // we simply check if it fits the requested parameters

        check(origin is Station)
        check(destination is Station)

        val (departureStop, arrivalStop) = dummyServiceStops
            .filter { it.passServiceId == dummyService.id } // actually does nothing because we only know stops for the one service
            .let {
                it.firstOrNull {
                    it.stationId == origin.id
                } to it.firstOrNull {
                    it.stationId == destination.id
                }
            }

        if (departureStop == null || arrivalStop == null) {
            return@sequence
        } else if (departureStop.departure == null || arrivalStop.arrival == null) {
            return@sequence
        } else if (departureStop.departure >= arrivalStop.arrival) {
            return@sequence
        } else if (departTime != null && departTime > departureStop.departure) {
            return@sequence
        } else if (arriveTime != null && arriveTime < arrivalStop.arrival) {
            return@sequence
        } else {
            yield(RoutePlan(persistentListOf(departureStop, arrivalStop)))
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
}