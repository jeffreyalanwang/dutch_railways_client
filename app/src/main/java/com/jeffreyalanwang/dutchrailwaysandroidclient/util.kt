package com.jeffreyalanwang.dutchrailwaysandroidclient

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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
    val aNextToPair = MutableList(a.size, { 0 })

    // Each item holds, based on this position in [aNextToPair],
    //  the next pairing's value by [combine].
    // It holds [null] if it will definitely not be paired next
    //  (i.e. its value in [aNextToPair] is the same as
    //  that of another item at a lower index).
    val aNextPairValue = MutableList<U?>(a.size, { null })
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

fun <T> List<T>.update(index: Int, value: T): List<T> {
    val newList = this.toMutableList()
    newList[index] = value
    return newList
}

fun <T> List<T>.plusInsert(index: Int, element: T): List<T> {
    return this.subList(0, index)
        .plus(element)
        .plus(this.subList(index, this.size))
}

fun <T> List<T>.dropAt(index: Int): List<T> {
    return this.subList(0, index)
        .plus(this.subList(index, this.size).drop(1))
}

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

fun <T, U> List<Pair<T, U>>.withFlatIndex()
    = this
        .withIndex()
        .map { (index, pair) -> Triple(index, pair.first, pair.second) }

inline fun <T> T.letIf(condition: Boolean, then: (T)->T): T
    = if (!condition) this else then(this)

class ReadOnlyLateInit<T> : ReadWriteProperty<Any?, T> {
    private var value: T? = null
    var isInitialized = false
        get() = field
        private set(x) { field = x }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (!isInitialized) {
            throw UninitializedPropertyAccessException(
                "Property ${property.name} has not been initialized."
            )
        }
        return value!!
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
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

