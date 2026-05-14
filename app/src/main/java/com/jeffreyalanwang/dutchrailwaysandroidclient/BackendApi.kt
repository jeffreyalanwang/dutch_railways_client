package com.jeffreyalanwang.dutchrailwaysandroidclient
import android.content.res.Resources
import com.google.android.gms.maps.model.LatLng
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

open class Place(
    val id: UInt,
    val name: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Place) return false

        return this.id == other.id
    }
}

class Area(
    id: UInt,
    name: String,
) : Place(id, name)

class Station(
    id: UInt,
    name: String,
    val address: String,
    val geom: LatLng,
    private var stops: List<ServiceStop>? = null,
) : Place(id, name) {
    fun getStops(): List<ServiceStop> {
        if (stops == null) {
            stops = BackendApi.get_stops_at_station(this)
        }
        return stops!!
    }
}

class ServiceStop(
    val arrival: ZonedDateTime?,
    val departure: ZonedDateTime?,
    val passServiceId: UInt,
    val stationId: UInt,
) {
    private var passService: PassService? = null
    private var station: Station? = null

    constructor(arrival: ZonedDateTime, departure: ZonedDateTime, passService: PassService, station: Station)
            : this(arrival, departure, passServiceId=passService.id, stationId=station.id) {
        this.passService = passService
        this.station = station
    }
    constructor(arrival: ZonedDateTime, departure: ZonedDateTime, passServiceId: UInt, station: Station)
            : this(arrival, departure, passServiceId=passServiceId, stationId=station.id) {
        this.station = station
    }
    constructor(arrival: ZonedDateTime, departure: ZonedDateTime, passService: PassService, stationId: UInt)
            : this(arrival, departure, passServiceId=passService.id, stationId=stationId) {
        this.passService = passService
    }

    fun getStation() = BackendApi.get_station_info(stationId)
    fun getService() = BackendApi.get_pass_service(passServiceId)
}

class PassService(
    val id: UInt,
    val title: String,
    val trainset: Trainset,
    val amenities: EnumSet<TrainAmenity>,
    private var stops: List<ServiceStop>? = null,
) {
    fun getStops(): List<ServiceStop> {
        if (stops == null) {
            stops = BackendApi.get_stops_of_service(this)
        }
        return stops!!
    }
}

private fun parseAmsTime(s: String)
    = ZonedDateTime.of(LocalDateTime.parse(s), ZoneId.of("Europe/Amsterdam"))

object BackendApi {
    private const val BACKEND_URL = "http://msword-jw125.duckdns.org";

    private val dummyService = PassService(119u, "Intercity 2263 to Rotterdam Centraal", Trainset.VIRM, EnumSet.allOf(TrainAmenity::class.java))
    private val dummyServiceStops = listOf(
        ServiceStop(passServiceId=119u,arrival=null                                      ,departure=parseAmsTime("2026-05-08T18:36:00.000000"),stationId=358u),
        ServiceStop(passServiceId=119u,arrival=parseAmsTime("2026-05-08T19:28:00.000000"),departure=parseAmsTime("2026-05-08T19:30:00.000000"),stationId=376u),
        ServiceStop(passServiceId=119u,arrival=parseAmsTime("2026-05-08T19:49:00.000000"),departure=null                                      ,stationId=361u),
    )
    private val dummyAreas = listOf(
        Area(1u, "Nederland"),
        Area(10u, "Noord-Holland"),
        Area(9u, "Zuid-Holland"),
        Area(319u, "Rotterdam"),
        Area(287u, "'s-Gravenhage"),
        Area(145u, "Amsterdam"),
    )
    private val dummyStations = listOf(
        Station(358u, "Amsterdam Centraal", "5a, IJ-hal, Centrum, Amsterdam, Noord-Holland, Nederland, 1012 AA, Nederland", LatLng(52.37888718232718, 4.900277682877522)),
        Station(361u, "Rotterdam Centraal", "Spoor 8, Stationssingel, Provenierswijk, Noord, Rotterdam, Zuid-Holland, Nederland, 3033 HB, Nederland", LatLng(51.92499923833714, 4.468888827643443)),
        Station(376u, "Den Haag HS", "Stationsplein, Stationsbuurt, Centrum, Den Haag, Zuid-Holland, Nederland, 2515 RT, Nederland", LatLng(52.06972122391006, 4.322500294829242)),
    )

    fun autocomplete_place(query: String, subclasses: EnumSet<PlaceSubclass>): List<Place> {
        val candidates = ArrayList<Pair<Place, Double>>();

        if (PlaceSubclass.Station in subclasses) {
            candidates.addAll(dummyStations.map { Pair(it, max(fuzzratio(query, it.name), fuzzratio(query, it.address))) })
        }

        if (PlaceSubclass.Area in subclasses) {
            candidates.addAll(dummyAreas.map { Pair(it, fuzzratio(query, it.name)) })
        }

        return candidates.sortedByDescending{ it.second }.take(10).map{ it.first }
    }

    fun get_pass_service(id: UInt): PassService {
        assert(id == dummyService.id)
        return dummyService
    }

    fun get_station_info(id: UInt): Station {
        dummyStations.forEach {
            if (it.id == id) return it
        }
        throw Resources.NotFoundException("Id not found: $id");
    }

    // Must sort by arrival time before return

    fun get_stops_of_service(service_id: UInt): List<ServiceStop>
        = dummyServiceStops
            .filter { it.passServiceId == service_id }
            .sortedBy { it.arrival }
    fun get_stops_of_service(service: PassService) = get_stops_of_service(service.id)

    fun get_stops_at_station(station_id: UInt): List<ServiceStop>
        = dummyServiceStops
            .filter { it.stationId == station_id }
            .sortedBy { it.arrival }
    fun get_stops_at_station(station: Station) = get_stops_at_station(station.id)

    fun get_stops(service_id: UInt, station_id: UInt): ServiceStop
        = dummyServiceStops
            .filter { it.passServiceId == service_id }
            .filter { it.stationId == station_id }
            .first()
}