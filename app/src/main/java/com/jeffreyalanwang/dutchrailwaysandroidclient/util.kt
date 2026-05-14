package com.jeffreyalanwang.dutchrailwaysandroidclient

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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
