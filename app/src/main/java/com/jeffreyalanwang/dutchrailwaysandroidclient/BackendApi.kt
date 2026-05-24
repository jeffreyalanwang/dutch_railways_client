package com.jeffreyalanwang.dutchrailwaysandroidclient
import android.content.res.Resources
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.EnumSet
import kotlin.math.max
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

enum class PlaceSubclass { Area, Station }

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
class Area(
    id: Int,
    name: String,
) : Place(id, name), Parcelable

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
class ServiceStop(
    val arrival: ZonedDateTime?,
    val departure: ZonedDateTime?,
    val passServiceId: Int,
    val stationId: Int,
) : Parcelable {
    private var passService: PassService? = null
    private var station: Station? = null

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

    fun getStation() = BackendApi.get_station_info(stationId)
    fun getService() = BackendApi.get_pass_service(passServiceId)
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
    = ZonedDateTime.of(LocalDateTime.parse(s), ZoneId.of("Europe/Amsterdam"))

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

    fun autocomplete_place(query: String, subclasses: EnumSet<PlaceSubclass>): List<Place> { //TODO this should not be loading entire stations. just the data we need
        val candidates = ArrayList<Pair<Place, Double>>();

        if (PlaceSubclass.Station in subclasses) {
            candidates.addAll(dummyStations.map { Pair(it, max(fuzzratio(query, it.name), fuzzratio(query, it.address))) })
        }

        if (PlaceSubclass.Area in subclasses) {
            candidates.addAll(dummyAreas.map { Pair(it, fuzzratio(query, it.name)) })
        }

        return candidates.sortedByDescending{ it.second }.take(10).map{ it.first }
    }

    fun get_pass_service(id: Int): PassService {
        assert(id == dummyService.id)
        return dummyService
    }

    fun get_station_info(id: Int): Station {
        dummyStations.forEach {
            if (it.id == id) return it
        }
        throw Resources.NotFoundException("Id not found: $id");
    }

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