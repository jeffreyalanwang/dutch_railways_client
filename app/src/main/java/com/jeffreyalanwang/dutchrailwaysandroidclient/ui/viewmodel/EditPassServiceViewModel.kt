package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import com.jeffreyalanwang.dutchrailwaysandroidclient.BackendApi
import com.jeffreyalanwang.dutchrailwaysandroidclient.PassService
import com.jeffreyalanwang.dutchrailwaysandroidclient.ServiceStop
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import com.jeffreyalanwang.dutchrailwaysandroidclient.StopPoint
import com.jeffreyalanwang.dutchrailwaysandroidclient.lastNotNullOfOrNull
import com.jeffreyalanwang.dutchrailwaysandroidclient.map
import com.jeffreyalanwang.dutchrailwaysandroidclient.minus
import com.jeffreyalanwang.dutchrailwaysandroidclient.plus
import com.jeffreyalanwang.dutchrailwaysandroidclient.propagateNull
import com.jeffreyalanwang.dutchrailwaysandroidclient.replaceAt
import com.jeffreyalanwang.dutchrailwaysandroidclient.runFlattened
import com.jeffreyalanwang.dutchrailwaysandroidclient.runningFoldMap
import com.jeffreyalanwang.dutchrailwaysandroidclient.runningFoldMapReversed
import com.jeffreyalanwang.dutchrailwaysandroidclient.toKotlinInstant
import com.jeffreyalanwang.dutchrailwaysandroidclient.toKotlinLocalDateTime
import com.jeffreyalanwang.dutchrailwaysandroidclient.toZonedDateTime
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.ValidityCriteria.gtLeft
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.ValidityCriteria.ltRight
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.ValidityCriteria.notNull
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.viewmodel.ValidityCriteria.onSameDay
import com.jeffreyalanwang.dutchrailwaysandroidclient.updateFirst
import com.jeffreyalanwang.dutchrailwaysandroidclient.updateLast
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atDate
import kotlinx.datetime.toJavaZoneId
import kotlinx.datetime.toKotlinTimeZone
import kotlinx.datetime.todayIn
import kotlinx.parcelize.IgnoredOnParcel
import java.time.ZonedDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class TentativeStop(
    val stationId: Int? = null,
    val arrival: ZonedDateTime? = null,
    val departure: ZonedDateTime? = null,
) {
    /** Required because we cannot identify by a nullable [stationId] or mutable [arrival]. */
    val id = Registrar.getId()

    constructor(serviceStop: ServiceStop)
        : this(serviceStop.stationId, serviceStop.arrival, serviceStop.departure)

    @IgnoredOnParcel val zoneId
        get() = (arrival ?: departure)?.zone
    @IgnoredOnParcel val timeZone
        get() = zoneId?.toKotlinTimeZone()

    fun toServiceStop(passServiceId: Int): ServiceStop? {
        if (stationId == null) return null
        if (arrival == null && departure == null) return null
        return ServiceStop(
            stationId = stationId,
            arrival = arrival,
            departure = departure,
            passServiceId = passServiceId,
        )
    }

    fun getStation() = stationId?.let { BackendApi.get_station_info(it) }

    private object Registrar {
        private var nextId = 0
        fun getId() = nextId++
    }
}

interface EditPassServiceStopsModel {
    val stopsList: List<TentativeStop>
    val stationValidity: List<Boolean>
    val arrivalTimeValidity: List<Boolean>
    val departureTimeValidity: List<Boolean>

    fun updateStation(index: Int, newStation: Station)
    fun updateStopTime(stopId: Int, forPoint: StopPoint, time: LocalTime, shiftFollowing: Boolean = false)
    fun TentativeStop.suggestedTime(point: StopPoint): ZonedDateTime

    fun addStop()
    fun removeStop(index: Int)
    fun reorderStops(iFrom: Int, iTo: Int)
}

/**
 * Currently cannot handle crossing midnight.
 *
 * Intermediately, first + last stops are allowed to have
 * times; however, these will be removed upon save.
 */
private class StopsDelegate(basedOnStops: List<ServiceStop>?): EditPassServiceStopsModel {

    override val stopsList: SnapshotStateList<TentativeStop> =
        ( basedOnStops ?: emptyList() )
            .map { TentativeStop(it) }
            .toMutableStateList()

    override val stationValidity: List<Boolean>
        by derivedStateOf {
            stopsList.map {
                it.stationId != null &&
                stopsList
                    .count { other -> other.stationId == it.stationId }
                    .equals(1)
            }
        }

    private val _timeValidity
        by derivedStateOf {
            val stopTimeValidity = stopsList
                .map { it.arrival to it.departure }
                .runFlattened {
                    val singleCriteria = this.run {
                        listOf(
                            notNull(),
                            onSameDay(),
                        )
                    } + map { time -> time?.toKotlinInstant() }.run {
                        listOf(
                            gtLeft(),
                            ltRight(),
                        )
                    }

                    val allCriteria = this.indices.map { i ->
                        singleCriteria.all { bools -> bools[i] }
                    }

                    // These will always be set to null by [getFinalStops]
                    val out = allCriteria
                        .updateFirst { true }
                        .updateLast { true }

                    return@runFlattened out
                }

            stopTimeValidity.map { it.first } to stopTimeValidity.map { it.second }
        }
    override val arrivalTimeValidity: List<Boolean>
        by derivedStateOf { _timeValidity.first }
    override val departureTimeValidity: List<Boolean>
        by derivedStateOf { _timeValidity.second }

    override fun updateStation(index: Int, newStation: Station) {
        stopsList.replaceAt(index) { stop ->
            stop.copy(stationId = newStation.id)
        }
    }

    override fun updateStopTime(stopId: Int, forPoint: StopPoint, time: LocalTime, shiftFollowing: Boolean) {
        val (stopIndex, oldStop) = stopsList.withIndex().find { it.value.id == stopId } ?: return

        // Convert to a ZonedDateTime
        val tz = stopsList
            .firstNotNullOfOrNull { it.zoneId }
            ?.toKotlinTimeZone()
            ?: TimeZone.currentSystemDefault()

        val date = stopsList
            .firstNotNullOfOrNull {
                (it.arrival ?: it.departure)
                    ?.toKotlinLocalDateTime()
                    ?.date
            }
            ?: Clock.System.todayIn(tz)

        val zonedDateTime = time
            .atDate(date)
            .toZonedDateTime(tz)

        val newStop = when (forPoint) {
            StopPoint.Arrival -> oldStop.copy(arrival = zonedDateTime)
            StopPoint.Departure -> oldStop.copy(departure = zonedDateTime)
        }
        stopsList[stopIndex] = newStop

        if (shiftFollowing) {
            val (oldTime, newTime) =
                when (forPoint) {
                    StopPoint.Arrival -> oldStop.arrival to newStop.arrival
                    StopPoint.Departure -> oldStop.departure to newStop.departure
                }
            check(oldTime != null)
            val delta = newTime!! - oldTime

            // Shift this stop's depart time, if we just changed arrival
            if (forPoint == StopPoint.Arrival) {
                stopsList[stopIndex] = stopsList[stopIndex].run {
                    copy(departure = departure?.plus(delta))
                }
            }
            // Shift all following stops' times
            stopsList.subList(stopIndex + 1, stopsList.size)
                .replaceAll { stop ->
                    stop.copy(
                        arrival = stop.arrival?.let { it + delta },
                        departure = stop.departure?.let { it + delta },
                    )
                }
        }
    }

    override fun addStop() {
        stopsList.add(
            TentativeStop()
        )
    }

    override fun removeStop(index: Int) {
        stopsList.removeAt(index)
    }

    override fun reorderStops(iFrom: Int, iTo: Int) {
        stopsList.run {
            val item = removeAt(iFrom)
            add(iTo, item)
        }
    }

    /** Get a savable version of the tentative stop list. */
    fun getFinalStops(): List<ServiceStop>? {
        if (
            stationValidity
                .plus(arrivalTimeValidity + departureTimeValidity)
                .any { !it }
        ) {
            return null
        }

        val finalStops = stopsList
            .updateFirst { it.copy(arrival = null) }
            .updateLast { it.copy(departure = null) }
            .map {
                it.toServiceStop(passServiceId = -1)
            }
            .propagateNull()

        return finalStops
    }

    override fun TentativeStop.suggestedTime(point: StopPoint): ZonedDateTime {

        when (point as Any) { // coerce to a when-statement, not when-expression
            StopPoint.Arrival if this.arrival != null ->
                return this.arrival
            StopPoint.Departure if this.departure != null ->
                return this.departure
        }

        val oldTimes = this.arrival to this.departure

        val thisIndex = stopsList.indexOfFirst { it.id == this.id }
        val prevDepart = stopsList.take(thisIndex).lastNotNullOfOrNull { it.departure }
        val nextArrive = stopsList.drop(thisIndex + 1).firstNotNullOfOrNull { it.arrival }
        val bounds = prevDepart to nextArrive

        val times =
            generateStopTimes(
                oldTimes.map { it?.toKotlinInstant() },
                bounds.map { it?.toKotlinInstant() },
            )
            .map { suggested ->
                suggested.toZonedDateTime(
                    this.zoneId
                        ?: stopsList.firstNotNullOfOrNull { it.zoneId }
                        ?: TimeZone.currentSystemDefault().toJavaZoneId()
                )
            }

        return when (point) {
            StopPoint.Arrival -> times.first
            StopPoint.Departure -> times.second
        }
    }
}

private object ValidityCriteria {
    fun List<Any?>.notNull() = map { it != null }

    fun List<ZonedDateTime?>.onSameDay(): List<Boolean> {
        val leftDate = firstNotNullOfOrNull { it?.toLocalDate() }
        return map { it?.toLocalDate()  == leftDate }
    }

    fun List<Instant?>.gtLeft() =
        runningFoldMap(Instant.DISTANT_PAST) { acc, time ->
            if (time == null) acc to false
            else maxOf(acc, time) to (acc < time)
        }

    fun List<Instant?>.ltRight() =
        runningFoldMapReversed(Instant.DISTANT_FUTURE) { acc, time ->
            if (time == null) acc to false
            else minOf(acc, time) to (acc > time)
        }
}

private fun generateStopTimes(
    oldTimes: Pair<Instant?, Instant?>,
    bounds: Pair<Instant?, Instant?>,
): Pair<Instant, Instant> {
    val (oldArrival, oldDeparture) = oldTimes
    val (startBound, endBound) = bounds

    // If both bounds are provided, center the stop between them
    if (startBound != null && endBound != null) {
        // New stop's duration should equal the old, if possible
        val duration =
            if (oldArrival != null && oldDeparture != null)
                oldDeparture - oldArrival
            else 1.minutes

        val newArrival = startBound +
                (endBound - startBound - duration) / 2
        val newDeparture = newArrival + duration

        return newArrival to newDeparture
    }

    // If one bound is null, use the other
    if (startBound != null) {
        return startBound to startBound + 1.minutes
    }
    if (endBound != null) {
        return endBound - 1.minutes to endBound
    }

    // If both bounds are null, use the old times
    if (oldArrival != null && oldDeparture != null) {
        return oldArrival to oldDeparture
    }
    if (oldArrival != null) {
        return oldArrival to oldArrival + 1.minutes
    }
    if (oldDeparture != null) {
        return oldDeparture - 1.minutes to oldDeparture
    }

    // All args were null
    return Clock.System.now().let { it to it + 1.minutes }
}

class EditPassServiceViewModel private constructor(
    basedOnService: PassService?,
    private val destPassServiceId: Int?,
    private val stopsDelegate: StopsDelegate = StopsDelegate(basedOnService?.getStops()),
) : ViewModel(), EditPassServiceStopsModel by stopsDelegate {

    constructor(
        basedOnService: PassService?,
        destPassServiceId: Int?
    ) : this(
        basedOnService,
        destPassServiceId,
        StopsDelegate(basedOnService?.getStops())
    )

    var trainsetSelection by mutableStateOf(basedOnService?.trainset)
    val trainsetValid by derivedStateOf { trainsetSelection != null }

    var amenitiesMultiSelection by mutableStateOf(basedOnService?.amenities ?: emptySet())

    // e.g. "Intercity 2398 to Rotterdam Centraal"
    private val trainName: String = basedOnService
        ?.run {
            val index = title.indexOf(" to ")
            if (index < 0) title
            else title.substring(0, index)
        }
        ?: "Train"
    val title by derivedStateOf {
            val destStationSuffix = stopsList
                .lastNotNullOfOrNull { it.getStation() }
                ?.run { " to $name" }

            trainName + (destStationSuffix ?: "")
        }

    /** Returns null if state is invalid for saving. */
    fun saveChanges(): Int? {
        val stops = stopsDelegate.getFinalStops()
        if (!trainsetValid || stops == null) return null

        return if (destPassServiceId == null) {
            BackendApi.add_pass_service(
                title,
                trainset = trainsetSelection!!,
                amenities = amenitiesMultiSelection,
                stops = stops,
            ).id
        } else {
            BackendApi.update_pass_service(
                destPassServiceId,
                trainset = trainsetSelection!!,
                amenities = amenitiesMultiSelection,
                stops = stops,
            )
            destPassServiceId
        }
    }
}
