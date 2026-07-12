package com.jeffreyalanwang.dutchrailwaysandroidclient

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.BackendApi
import com.google.android.gms.maps.model.LatLng
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.toKotlinTimeZone
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime
import kotlin.time.Duration

enum class Endpoint { Origin, Destination }
enum class StopPoint { Arrival, Departure }

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
data class Journey(
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

operator fun Journey.plus(other: ServiceStop)
        = Journey((this.stops + other).toImmutableList())

operator fun ServiceStop.plus(other: Journey)
        = Journey(other.stops.plusInsert(0, this).toImmutableList())

@Parcelize
data class PolygonData(
    val points: List<LatLng>,
    val holes: List<List<LatLng>> = emptyList(),
) : Parcelable

sealed interface Place : Parcelable {
    val id: Int
    val name: String
}

@Parcelize
data class Area(
    override val id: Int,
    override val name: String,
) : Place, Parcelable {
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
data class Station(
    override val id: Int,
    override val name: String,
    val address: String,
    val geom: LatLng,
) : Place, Parcelable {
    fun getStops() = BackendApi.get_stops_at_station(this)
}

/**
 * At least one of [arrival] or [departure] will always be non-null.
 */
@Parcelize
@Immutable
data class ServiceStop(
    val arrival: ZonedDateTime?,
    val departure: ZonedDateTime?,
    val passServiceId: Int,
    val stationId: Int,
) : Parcelable {
    @IgnoredOnParcel private var passService: PassService? = null
    @IgnoredOnParcel private var station: Station? = null
    @IgnoredOnParcel val zoneId
        get() = (arrival ?: departure)?.zone
    @IgnoredOnParcel val timeZone
        get() = zoneId?.toKotlinTimeZone()

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
data class PassService(
    val id: Int,
    val title: String,
    val trainset: Trainset,
    val amenities: Set<TrainAmenity>,
) : Parcelable {
    fun getStops() = BackendApi.get_stops_of_service(this)
}