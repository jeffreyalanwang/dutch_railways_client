package com.jeffreyalanwang.dutchrailwaysandroidclient

import android.location.Address
import androidx.compose.runtime.Stable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastRoundToInt
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toJavaZoneId
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.toJavaDuration
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

fun <T> checkAll(values: Iterable<T>, block: (T) -> Boolean)
    = values.forEach { check(block(it)) }

infix fun <T: Comparable<U>, U> T?.geBothElvis(other: U?): Boolean? {
    return if (this == null || other == null) null
        else this >= other
}

infix fun <T: Any, U: Any> T?.toBothElvis(that: U?): Pair<T, U>? {
    return if (this == null || that == null) null
        else this to that
}

fun <T: Any, U: Any, R> Pair<T?, U?>.letBothElvis(block: (Pair<T, U>) -> R): R? {
    val (a, b) = this
    return (
        if (a == null || b == null) null
        else block(a to b)
    )
}

fun <A, B, C, D> Pair<Pair<A, B>, Pair<C, D>>.transpose()
    = (first.first to second.first) to (first.second to second.second)

fun <A, B, R, S> Pair<Pair<A, A>, Pair<B, B>>.transpose(block: (Pair<A, B>) -> Pair<R, S>)
    = block(first.first to second.first) to block(first.second to second.second)

fun <T, R> Pair<T, T>.map(block: (T) -> R): Pair<R, R>
    = block(first) to block(second)

fun <T, R> Triple<T, T, T>.map(block: (T) -> R): Triple<R, R, R>
        = Triple(block(first), block(second), block(third))

fun <T, U, R> Pair<T, T>.zip(other: Pair<U, U>, block: (T, U) -> R): Pair<R, R>
    = block(first, other.first) to block(second, other.second)

fun <T> Iterable<T>.toPair(): Pair<T, T> {
    val iterator = this.iterator()
    val out = Pair(iterator.next(), iterator.next())
    check(!iterator.hasNext())
    return out
}

infix fun <K, V1, V2> Map<K, V1>.zipOnKeys(other: Map<K, V2>)
    = this.zipOnKeys(other) { a, b -> a to b }

fun <K, V1, V2, R> Map<K, V1>.zipOnKeys(other: Map<K, V2>, block: (V1, V2) -> R): Map<K, R>
    = (this.keys intersect other.keys)
        .associateWith {
            block(
                this.getValue(it),
                other.getValue(it),
                )
        }

fun <T, U: Comparable<U>> Iterable<T>.isSorted(selector: (T)->U)
    = this
        .map { selector(it) }
        .isSorted()

fun <T: Comparable<T>> Iterable<T>.isSorted()
    = this
        .zipWithNext { a, b -> a < b }
        .all { it }

/**
 * Flatten by merging sequences.
 *
 * In other words, yields the minimum next value from the
 * provided sequences until they are all exhausted.
 *
 * Receiver object must hold sequences which themselves
 * yield in sorted order.
 */
fun <T, U: Comparable<U>> Iterable<Sequence<T>>.flattenSorted(selector: (T) -> U)
    = sequence<T> {
        val sources = this@flattenSorted
            .map { sequence -> sequence.iterator() }
            .filter { sequence -> sequence.hasNext() }
            .map { sequence -> sequence to sequence.next() }
            .toMutableList()

        while (sources.isNotEmpty()) {
            val next = sources
                .withFlatIndex()
                .minBy { (index, iterator, nextItem) -> selector(nextItem) }
            val nextIdx = next.first
            val nextIterator = next.second
            val nextItem = next.third

            yield(nextItem)

            if (nextIterator.hasNext()) {
                sources[nextIdx] = sources[nextIdx]
                    .copy(
                        second = nextIterator.next()
                    )
            } else {
                sources.removeAt(nextIdx)
            }
        }
    }

inline fun <T, reified U: Comparable<U>> List<List<T>>.flattenSorted(selector: (T) -> U): List<T> {
    val sourceLists = this
    val nextIndices = Array(this.size) { 0 }
    val nextValues = Array(this.size) { i -> this[i].getOrNull(0)?.let{ selector(it) } }
    val out = ArrayList<T>(sourceLists.sumOf { it.size })

    while (nextValues.any { it != null }) {
        val listIdx = nextValues
            .withIndex()
            .filter { it.value != null }
            .minBy { it.value!! }
            .index

        val itemIdx = nextIndices[listIdx]
        out += sourceLists[listIdx][itemIdx]

        nextValues[listIdx] = sourceLists[listIdx]
            .getOrNull(
                ++nextIndices[listIdx]
            )
            ?.let { selector(it) }
    }

    return out
}

fun <T> Iterable<Sequence<T>>.flattenRoundRobin()
        = sequence<T> {
    val sources = this@flattenRoundRobin.map { it.iterator() }.toMutableList()
    var i = 0
    while (sources.isNotEmpty()) {
        if (sources[i].hasNext()) {
            yield(sources[i].next())
            i++
        }
        else {
            sources.removeAt(i)
        }
        i %= sources.size
    }
}

fun <T> Collection<T?>.propagateNull(): List<T>? {
    val notNull = this.filterNotNull()
    return if (notNull.size == this.size) notNull else null
}

fun IntRange.Companion.from(start: Int, size: Int)
    = start..(start + size)

/**
 *  All indices are based from the original/old list.
 *  Guaranteed to be sorted by index.
 *  Most recent mutations are last in the list.
 */
class Diff<T>(val mutations: List<Mutation<T>>): List<Diff.Mutation<T>> by mutations {
    val containsRemoval
        get() = any { it is Mutation.Remove }
    val containsAddition
        get() = any { it is Mutation.Add }

    fun <K, V> applyOn(
        subject: MutableMap<K, V>,
        onRemove: ((Int, K, V) -> Unit)? = null,
        onAdd: (Int, addedItem: T) -> Pair<K, V>,
    ) = applyOn(
        subject,
        remove = { i ->
            val key = keys.toList()[i]
            val value = remove(key)!!
            key to value
        },
        add = { i, (key, value) -> add(i, key, value) },
        onRemove = { i, (key, value) -> onRemove?.invoke(i, key, value) },
        onAdd = onAdd,
    )

    fun applyOn(
        subject: MutableList<T>,
        onRemove: ((Int, T) -> Unit)? = null,
        onAdd: (Int, addedItem: T) -> T = { i, it -> it },
    ) = applyOn(
        subject,
        remove = { i -> removeAt(i) },
        add = { i, it -> add(i, it) },
        onRemove = onRemove,
        onAdd = onAdd,
    )

    /**
     * [remove] and [add] define operations on [subject].
     * [onRemove] and [onAdd] provide/handle item values.
     */
    private fun <TCollection, TItem> applyOn(
        subject: TCollection,
        remove: TCollection.(Int) -> TItem,
        add: TCollection.(Int, added: TItem) -> Unit,
        onRemove: ((Int, TItem) -> Unit)? = null,
        onAdd: (Int, addedItem: T) -> TItem,
    ) = applyOn(
        remove = { index ->
            val removed = subject.remove(index)
            onRemove?.invoke(index, removed)
        },
        add = { index, item ->
            val itemToAdd = onAdd.invoke(index, item)
            subject.add(index, itemToAdd)
        },
    )

    fun applyOn(
        remove: (Int) -> Unit,
        add: (Int, addedItem: T) -> Unit,
    ) {
        for (mutation in asReversed()) {
            with (mutation) {
                when (this) {
                    is Mutation.Remove -> remove(index)
                    is Mutation.Add -> add(index, item)
                }
            }
        }
    }

    sealed interface Mutation<T> {
        data class Remove<T>(val index: Int): Mutation<T>
        data class Add<T>(val index: Int, val item: T): Mutation<T>
    }

    companion object {
        /**
         * Requirements:
         *  * [old] and [new] do not contain duplicates within themselves.
         *  * Items in [old] and [new] are in the same order.
         *
         * When a removal and addition occur adjacent to each other,
         *  the removal is assumed to have been done first.
         * */
        fun <T> of(old: List<T>, new: List<T>): Diff<T> {
            check(old.toSet().size == old.size)
            check(new.toSet().size == new.size)

            var iOld = 0
            var iNew = 0
            val out = mutableListOf<Mutation<T>>()
            while (iOld < old.size && iNew < new.size) {
                if (old[iOld] == new[iNew]) {
                    iOld++
                    iNew++
                    continue
                }

                val a = new.indexOf(old[iOld])
                val b = old.indexOf(new[iNew])

                if (a >= 0 && b >= 0) {
                    check(a > iNew)
                    check(b > iOld)
                    throw IllegalArgumentException(
                        "Items switched order:"
                        + " ${old[iOld]} at old[$iOld] and new[$a]"
                        + " ${new[iNew]} at old[$b] and new[$iNew]"
                    )
                } else if (a >= 0) {
                    check(a > iNew)
                    while (a > iNew) {
                        out += Mutation.Add(iOld, new[iNew])
                        iNew++
                        check(new[iNew] !in old)
                    }

                    // At this point, old[iOld] == new[iNew]
                    iOld++
                    iNew++
                } else if (b >= 0) {
                    check(b > iOld)
                    while (b > iOld) {
                        out += Mutation.Remove(iOld)
                        iOld++
                        check(old[iOld] !in new)
                    }

                    // At this point, old[iOld] == new[iNew]
                    iOld++
                    iNew++
                } else { // curr element in [old] must be removed,
                         // and curr element in [new] must be added;
                         // potentially, there are no more elements in common
                    // Assume removals first
                    while (iOld <= old.lastIndex && old[iOld] !in new) {
                        out += Mutation.Remove(iOld)
                        iOld++
                    }
                    // Additions are processed in next loop iteration,
                    // or if we are at the end, then below
                }
            }

            while (iNew <= new.lastIndex) {
                out += Mutation.Add(iOld, new[iNew])
                iNew++
            }

            return Diff(out)
        }
    }
}

fun <K, V> MutableMap<K, V>.add(index: Int, key: K, value: V) {
    val keysList = keys.toList()
    val popped = (keysList.lastIndex downTo index)
        .map {
            keys.toList()[index].let {
                it to remove(it)!!
            }
        }
    put(key, value)
    for ((k, v) in popped.asReversed()) {
        put(k, v)
    }
}

/**
 * Same as [groupBy] but produces separate groups for elements
 * which have the same key by [keySelector] if an element exists
 * between them with a different [keySelector] key.
 */
fun <T, K> Iterable<T>.groupByContinuous(keySelector: (T)->K): List<Pair<K, List<T>>> {
    val out = mutableListOf<Pair<K, MutableList<T>>>()

    for (item in this) {
        val itemKey = keySelector(item)
        val lastGroup = out.lastOrNull()

        if (lastGroup != null && itemKey == lastGroup.first) {
            lastGroup.second.add(item)
        } else {
            out.add(itemKey to mutableListOf(item))
        }
    }

    return out
}

operator fun <T, U> Sequence<T>.times(other: Sequence<U>)
    = this.flatMap { thisItem ->
        other.map { otherItem ->
            Pair(thisItem, otherItem)
        }
    }

fun <T, U: Comparable<U>> timesSorted(
    a: List<T>,
    b: List<T>,
    selector: (T) -> U,
    combine: (U, U) -> U,
) = sequence<Pair<T, T>> {
    assert(a.map(selector).isSorted())
    assert(b.map(selector).isSorted())

    if (a.size < 2 || b.size < 2) {
        yieldAll(a.asSequence() * b.asSequence())
        return@sequence
    }

    val getSortKey = { (_a, _b): Pair<T, T> -> combine( selector(_a), selector(_b) ) }

    // Each item holds the next index in [b]
    //  with which this position in [a] is not yet paired.
    // Values always decrease as index increases.
    val aNextToPair = MutableList(a.size) { 0 }

    // Each item holds, based on this position in [aNextToPair],
    //  the next pairing's value by [combine].
    // It holds [null] if it will definitely not be paired next
    //  (i.e. its value in [aNextToPair] is the same as
    //  that of another item at a lower index).
    val aNextPairValue = MutableList<U?>(a.size) { null }
        .apply { this[0] = getSortKey(a[0] to b[0]) }

    while (aNextToPair.any { it != b.size }) {
        val idxBestCandidate =
            // Get the aIndex, bIndex, and sort key for the
            // first of each unique [b]-index in [aNextToPair].
            (aNextToPair zipIndexed aNextPairValue)
            .fold(
                ArrayList<Triple<Int, Int, U>>(aNextToPair.size)
            ) { acc, (aIndex, bIndex, value) ->
                acc.apply {
                    if (isEmpty() || bIndex != last().second) {
                        add( Triple(
                            aIndex,
                            bIndex,
                            value!!
                        ) )
                    }
                }
            }
            // Get the best candidate
            .minBy { it.third } // first element wins (and they are sorted by aIndex)
            .run { Pair(first, second) }

        val bestCandidate = Pair(
            a[idxBestCandidate.first],
            b[idxBestCandidate.second],
        )
        val aIdxBestCandidate = idxBestCandidate.first
        yield(bestCandidate)

        // Set up the next iteration
        // 1. Update aNextToPair at aIdxBestCandidate
        aNextToPair[aIdxBestCandidate]++
        // 2. Update aNextPairValue at aIdxBestCandidate
        if (
            aNextToPair[aIdxBestCandidate] == b.size ||
            aNextToPair[aIdxBestCandidate] == aNextToPair[aIdxBestCandidate - 1]
        ) {
            aNextPairValue[aIdxBestCandidate] = null
        } else {
            aNextPairValue[aIdxBestCandidate] = getSortKey(
                bestCandidate.copy(
                    second = b[aNextToPair[aIdxBestCandidate]]
                )
            )
        }
        // 3. Update aNextPairValue at [aIdxBestCandidate + 1]
        if (aNextPairValue[aIdxBestCandidate + 1] == null) {
            aNextPairValue[aIdxBestCandidate + 1] = getSortKey(
                Pair(
                    a[aIdxBestCandidate + 1],
                    b[aNextToPair[aIdxBestCandidate + 1]],
                )
            )
        }
    }
}

fun <T> MutableList<T>.replaceSubList(start: Int, endExclusive: Int = size, block: (T)->T) {
    for (i in start until endExclusive) {
        this[i] = block(this[i])
    }
}

fun <T> MutableList<T>.replaceAt(index: Int, block: (T)->T) {
    this[index] = block(this[index])
}

fun <K, V> MutableMap<K, V>.replaceAt(key: K, block: (V?)->V)
        = set(key, block(get(key)))

fun <T> PersistentList<T>.removeLast()
        = this.removeAt(size - 1)

fun <T> List<T>.update(index: Int, block: (T) -> T)
    = mapIndexed { i, item -> if (i == index) block(item) else item }

fun <T> List<T>.updateFirst(block: (T) -> T)
    = update(0, block)

fun <T> List<T>.updateLast(block: (T) -> T)
    = update(lastIndex, block)

fun <T> List<T>.update(index: Int, value: T): List<T>
    = update(index) { value }

fun <T> List<T>.plusInsert(index: Int, element: T): List<T> {
    return this.subList(0, index)
        .plus(element)
        .plus(this.subList(index, this.size))
}

fun <T> List<T>.dropAt(index: Int): List<T> {
    return this.subList(0, index) +
        this.subList(index, this.size)
            .drop(1)
}

fun joinToString(separator: String, vararg strings: String)
    = strings.joinToString(separator)

inline fun <T, A, R> Iterable<T>.runningFoldMap(initial: A, block: (A, T) -> Pair<A, R>): List<R> {
    var acc = initial
    return this.map {
        val (a, r) = block(acc, it)

        acc = a
        return@map r
    }
}

inline fun <T, A, R> Iterable<T>.runningFoldMapReversed(initial: A, crossinline block: (A, T) -> Pair<A, R>)
    = runReversed { runningFoldMap(initial, block) }

infix fun <T, U> Array<out T>.zipIndexed(other: Iterable<U>): List<Triple<Int, T, U>>
    = this.toList().zipIndexed(other)

infix fun <T, U> Iterable<T>.zipIndexed(other: Iterable<U>): List<Triple<Int, T, U>> {
    val first = iterator()
    val second = other.iterator()

    var i = 0
    val list = ArrayList<Triple<Int, T, U>>()

    while (first.hasNext() && second.hasNext()) {
        val item = Triple(i, first.next(), second.next())
        list.add(item)
        i++
    }

    return list
}

fun <E> MutableList<E>.addNotNull(element: E?) = element?.let { add(it) }
fun <E> MutableList<E>.addAllNotNull(elements: Collection<E?>) = addAll(elements.filterNotNull())

fun Iterable<Boolean>.all() = all { it }

fun <K, V> Collection<K>.associateWithIndexed(
    valueSelector: (Int, K) -> V
) = LinkedHashMap<K, V>(
        (size / 0.75F + 1.0F).toInt()
            .coerceAtLeast(16)
    )
    .apply {
        forEachIndexed { i, key ->
            set(key, valueSelector(i, key))
        }
    }


fun <T, U> List<Pair<T, U>>.withFlatIndex()
    = this
        .withIndex()
        .map { (index, pair) -> Triple(index, pair.first, pair.second) }

inline fun <T, R : Any> Iterable<T>.lastNotNullOfOrNull(transform: (T) -> R?): R? {
    var result: R? = null
    for (element in this) {
        result = transform(element) ?: result
    }
    return result
}

/** Run [block] on flattened list, then un-flatten before returning. */
fun <T, R> Iterable<Pair<T, T>>.runFlattened(block: List<T>.() -> List<R>): List<Pair<R, R>>
    = this.flatMap { it.toList() }
        .block()
        .chunked(2) { it[0] to it[1] }

/** Run [block] on reversed list, then un-reverse before returning. */
fun <T, R> Iterable<T>.runReversed(block: List<T>.() -> List<R>)
    = this.reversed().block().reversed()

inline fun <T, U, R> T.letWith(receiver: U, block: U.(T) -> R): R
    = block(receiver, this)

inline fun <T, R> T.letIf(
    condition: (T) -> Boolean,
    then: (T) -> R,
    otherwise: (T) -> R,
): R = if (condition(this)) then(this)
    else otherwise(this)

inline fun <T: R, R> T.letIf(condition: (T) -> Boolean, then: (T)->R): R
    = this.letIf(condition, then) { this }

inline fun <T> T.letIf(condition: Boolean, then: (T)->T): T
    = this.letIf({ condition }, then)

fun List<ServiceStop>.lastStationName() = this.lastOrNull()?.getStation()?.name

fun ZonedDateTime.toKotlinLocalTime() = toKotlinLocalDateTime().time
fun ZonedDateTime.toKotlinLocalDate() = toKotlinLocalDateTime().date

operator fun ZonedDateTime.plus(duration: Duration): ZonedDateTime
    = this + duration.toJavaDuration()

fun LocalDateTime.toZonedDateTime(zone: ZoneId): ZonedDateTime
    = this.toJavaLocalDateTime().atZone(zone)

fun LocalDateTime.toZonedDateTime(zone: TimeZone): ZonedDateTime
    = this.toZonedDateTime(zone.toJavaZoneId())

context(tz: TimeZone)
fun Instant.toLocalTime()
    = this.toLocalDateTime(tz).time

fun ZonedDateTime.toKotlinLocalDateTime()
    = this.toLocalDateTime().toKotlinLocalDateTime()

fun ZonedDateTime.toKotlinInstant()
    = this.toInstant().toKotlinInstant()

fun Instant.toZonedDateTime(zone: ZoneId): ZonedDateTime
    = this.toJavaInstant().atZone(zone)

operator fun ZonedDateTime.minus(other: ZonedDateTime)
    = this.toKotlinInstant().minus(other.toKotlinInstant())

operator fun ZonedDateTime.compareTo(other: Instant)
    = this.toKotlinInstant().compareTo(other)


infix operator fun Instant.compareTo(other: ZonedDateTime)
    = this.compareTo(other.toKotlinInstant())

fun <T> Pair<T, T>.equalOn(selector: (T) -> Any?): Boolean
    = map { selector(it) } .run { first == second }

/**
 * Stores a value provided on initialization.
 * Otherwise, calculates a value using a provided code block.
 */
class ValueOrLazy<T>(
    private var field: T? = null,
    private val block: () -> T,
) : ReadOnlyProperty<Any?, T> {

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return field ?: block().also { field = it }
    }

}

class ReadOnlyLateInit<T> : ReadWriteProperty<Any, T> {
    private var value: T? = null
    var isInitialized = false
        private set

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        if (!isInitialized) {
            throw UninitializedPropertyAccessException(
                "Property ${property.name} has not been initialized."
            )
        }
        return value!!
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        if (isInitialized) {
            throw IllegalStateException(
                "Property ${property.name} is already initialized,"
                + " and cannot be modified."
            )
        }
        this.value = value
        isInitialized = true
    }
}

/**
 * A [Lazy] property delegate whose [setValue] method is a suspending function.
 * Because of this asynchronous condition, this cannot be used as a normal
 *  property delegate.
 */
@Stable
class SuspendLazy<T: Any?>(
    private val block: suspend () -> T,
) {
    // Using a MutableStateFlow makes the class [Stable]
    // and allows atomicity that ensures only one coroutine
    // initializes the value.
    private val valueFlow = MutableStateFlow<Pair<Boolean, T?>>(false to null)

    @Suppress("UNCHECKED_CAST")
    suspend fun getValue(): T {
        val (initialized, value) = valueFlow
            .updateAndGet { (initialized, value) ->
                true to
                    if (initialized) value
                    else block()
            }
        return value as T
    }
}

val Address.latLng get() = LatLng(latitude, longitude)
val Address.addressLines get() = (0..maxAddressLineIndex).map { i -> getAddressLine(i) }
val Address.addressString get() = addressLines.joinToString(", ")

fun List<LatLng>.calculateBounds()
    = this
        .fold(
            LatLngBounds.builder()
        ) { builder, point ->
            builder.include(point)
        }
        .build()

@JvmName("calculateBoundsForPlaces")
fun List<Place>.calculateBounds()
    = this.map { it.points }
        .reduce { a, b -> a + b }
        .calculateBounds()

/**
 * Return the minimum and maximum latitude and longitude of the polygon.
 */
fun PolygonData.getBounds(): LatLngBounds
    = this.points.calculateBounds()

/**
 * For use in finding a combined [LatLngBounds].
 */
val Place.points: List<LatLng>
    get() = when(this) {
        is Station -> listOf(this.geom)
        is Area -> this.getGeom().points
        else -> throw NotImplementedError()
    }


fun getCurrStop(stops: List<ServiceStop>): IndexedValue<ServiceStop> {
    for (item in stops.dropLast(1).withIndex()) {
        if (item.value.departure!! > Clock.System.now()) {
            return item
        }
    }

    return IndexedValue(stops.size-1, stops.last())
}

infix fun Float.interpolates(
    keypoints: Triple<Int, Int, Int>,
) = if (this < .5f) ( this        * 2) interpolates (keypoints.first  to keypoints.second)
    else            ((this - .5f) * 2) interpolates (keypoints.second to keypoints.third )

infix fun Float.interpolates(
    keypoints: Triple<Float, Float, Float>,
) = if (this < .5f) ( this        * 2) interpolates (keypoints.first  to keypoints.second)
    else            ((this - .5f) * 2) interpolates (keypoints.second to keypoints.third )

infix fun Float.interpolates(
    bounds: Pair<Float, Float>,
) = when (this) {
    0f -> bounds.first
    1f -> bounds.second
    else -> bounds.first + (bounds.second - bounds.first) * this
}

infix fun Float.interpolates(
    bounds: Pair<Int, Int>,
) = when (this) {
    0f -> bounds.first
    1f -> bounds.second
    else -> bounds.first + ((bounds.second - bounds.first) * this).fastRoundToInt()
}

fun <T: Comparable<T>> T.compareTo(range: ClosedRange<T>): Int {
    compareTo(range.start)
        .let { if (it < 0) return it }
    compareTo(range.endInclusive)
        .let { if (it > 0) return it }
    return 0
}

fun <T: Comparable<T>> T.compareTo(range: OpenEndRange<T>): Int {
    compareTo(range.start)
        .let { if (it < 0) return it }
    compareTo(range.endExclusive)
        .let { if (it >= 0) return it }
    return 0
}

fun <T: Comparable<T>> ClosedRange<T>.compareTo(value: T): Int
  = -value.compareTo(this)

fun <T: Comparable<T>> OpenEndRange<T>.compareTo(value: T): Int
  = -value.compareTo(this)

fun Float.toIntPercent() = (this * 100).toInt()

fun Double.isWholeNumber() = (this == toInt().toDouble())
fun Double.toParts() = toInt().let { it to (this - it) }

private const val FANCY_LEFT_DOUBLE_QUOTE = '\u201C'
private const val FANCY_RIGHT_DOUBLE_QUOTE = '\u201D'
private const val FANCY_LEFT_SINGLE_QUOTE = '\u2018'
private const val FANCY_RIGHT_SINGLE_QUOTE = '\u2019'
private const val NORMAL_DOUBLE_QUOTE = '"'
private const val NORMAL_SINGLE_QUOTE = '\''

fun CharSequence.unfancyQuotes(): String
    = this
        .replace("[$FANCY_LEFT_DOUBLE_QUOTE$FANCY_RIGHT_DOUBLE_QUOTE]".toRegex() , "$NORMAL_DOUBLE_QUOTE")
        .replace("[$FANCY_LEFT_SINGLE_QUOTE$FANCY_RIGHT_SINGLE_QUOTE]".toRegex() , "$NORMAL_SINGLE_QUOTE")

