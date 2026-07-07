package com.jeffreyalanwang.dutchrailwaysandroidclient.backend

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.jeffreyalanwang.dutchrailwaysandroidclient.backend.StringWithFields.Companion.parseUnsignedDecimalFields
import com.jeffreyalanwang.dutchrailwaysandroidclient.isWholeNumber
import com.jeffreyalanwang.dutchrailwaysandroidclient.letBothElvis
import com.jeffreyalanwang.dutchrailwaysandroidclient.letIf
import com.jeffreyalanwang.dutchrailwaysandroidclient.unfancyQuotes
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object Geocoding {
    /**
     * Someone must call this in some composable in
     * the [Activity] before [Geocoding] is used.
     */
    fun initialize(context: Context) {
        if (::geocoder.isInitialized) return

        geocoder = Geocoder(context)
    }

    @Composable
    fun initialize() {
        if (::geocoder.isInitialized) return

        val context = LocalContext.current
        geocoder = Geocoder(context)
    }
    private lateinit var geocoder: Geocoder

    suspend fun autocomplete_location(
        query: CharSequence,
    ): List<Address> = suspendCancellableCoroutine { c ->
        geocoder.getFromLocationName(query.toString(), 5) { r -> c.resume(r) }
    }

    suspend fun autocomplete_location(
        query: CharSequence,
        bounds: LatLngBounds,
    ): List<Address> = suspendCancellableCoroutine { c ->
        geocoder.getFromLocationName(
            query.toString(),
            5,
            bounds.southwest.latitude, // Lower-left latitude
            bounds.southwest.longitude, // Lower-left longitude
            bounds.northeast.latitude, // Upper-right latitude
            bounds.northeast.longitude, // Upper-right longitude
        ) { r -> c.resume(r) }
    }

    suspend fun closest_address(
        latLng: LatLng,
    ): Address? = suspendCancellableCoroutine { c ->
        geocoder.getFromLocation(
            latLng.latitude,
            latLng.longitude,
            1,
        ) { r -> c.resume(r.getOrNull(0)) }
    }
}

fun parseLatLng(query: CharSequence): LatLng? {
    //  45° 46' 52" N  108° 30' 14" W
    //  45° 46.00'  N  108° 30.00'  W
    //  45.0000     N  108.0000     W
    //  108.0000    W  45.0000     N
    // -45.0000       -108.0000
    // Also, any of these separated with comma

    val query = query.trim().unfancyQuotes()

    @Suppress("RegExpRepeatedSpace")
    val pattern = Regex("""
       (?:
            # Decimal degrees.
            # Positive or negative.
            ( -? \d (?:\.\d+)? )        
        |
            # Degrees, minutes, seconds.
            # Only one must be provided, and some/all units may be omitted.
            # Must end with N/S or E/W.
            (
                (?: \d            \s* [°']? \s* ){0,2}
                    \d (?:\.\d+)? \s* [°'"]? \s* 
                [neswNESW]
            )
        )
        \s* ,? \s*
       (?:
            ( -? \d (?:\.\d+)? )
        |
            (
                (?: \d            \s* [°']? \s* ){0,2}
                    \d (?:\.\d+)? \s* [°'"]? \s* 
                [neswNESW]
            )
       )
    """, RegexOption.COMMENTS)

    val result = pattern.matchEntire(query)
        ?: return null

    val (firstDecDeg, firstDecDms, secondDecDeg, secondDecDms) = result.destructured.toList().map { it.ifEmpty { null } }

    // Should we flip the order? Match first/second to NS/EW

    // if [null], component has no preference (only pos/neg decimal degrees given)
    val impliesFlip: Pair<Boolean?, Boolean?> =
        firstDecDms?.contains("[ewEW]") to secondDecDms?.contains("[nsNS]")

    if (impliesFlip.letBothElvis { it.first != it.second } ?: false) {
        // One requires to flip, the other requires not to
        return null
    }

    val (nsDecDeg, nsDecDms, ewDecDeg, ewDecDms) =
        if ( impliesFlip.run { first ?: second } ?: false ) { // we now know for sure that first & second agree
            listOf(secondDecDeg, secondDecDms, firstDecDeg, firstDecDms)
        } else {
            // both required no flip, or neither had a requirement
            listOf(firstDecDeg, firstDecDms, secondDecDeg, secondDecDms)
        }

    val nsCoord =
        nsDecDeg?.toDouble()  // regex ensures this is a valid decimal number
        ?: parseDms(nsDecDms!!)
        ?: return null        // [parseDms] considered component invalid
    val ewCoord =
        ewDecDeg?.toDouble()
        ?: parseDms(ewDecDms!!)
        ?: return null

    return LatLng(nsCoord, ewCoord)
}

private const val DEGREE_SYMBOL = '°'
private const val PRIME = '′'
private const val DOUBLE_PRIME = '″'

private enum class DmsComponent(val symbol: Char) {
    DEGREES(DEGREE_SYMBOL),
    MINUTES(PRIME),
    SECONDS(DOUBLE_PRIME),
}

private data class NumToDmsComponent(val num: Double, val component: DmsComponent?) {
    companion object {
        /**
         * Determines the [DmsComponent] this [num] is supposed to correspond to
         * based on [label].
         * @param label The text following [num] in a user input string.
         */
        fun fromLabel(num: Double, label: CharSequence): NumToDmsComponent? {
            val component =
                DmsComponent.entries.associateBy { "${it.symbol}" }
                    .plus("" to null) // [component == null]: we do not know which field this one is meant to fill
                    .getOrElse(label.toString()) { return null }
            return NumToDmsComponent(num, component)
        }
    }
}

fun parseDms(query: CharSequence): Double? {
    val query = query.trim().unfancyQuotes().replace('\'', PRIME).replace('"', DOUBLE_PRIME)

    // Determine direction
    if (query.count { it.uppercase() !in "NSEW" } != 1) {
        return null
    }
    val (sizeBeforeNSEW, nsew) = query
        .toCharArray() // use locale-agnostic [Char.uppercase()]
        .withIndex()
        .last { it.value.uppercase() in "NSEW" }
    val isPositive = nsew in "NE"

    // Determine degrees, minutes, and seconds
    val dmsString = query.take(sizeBeforeNSEW).parseUnsignedDecimalFields()
    if (dmsString.fieldCount !in 0..3) {
        return null
    }
    if (dmsString.fields.dropLast(1).any { !it.isWholeNumber() }) {
        return null
    }
    val numsToFields = dmsString
        .letIf({ !it.endsWithField }) { it + StringWithFields.Piece(string = "") }
        .dropWhile { !it.isField }
        .zipWithNext { a, b -> a.field!! to b.string!! }
        .map { (num, label) ->                          // [label]: text following [num] in [dmsString]
            NumToDmsComponent.fromLabel(num, label) ?: return null
        }
        .toMutableList()

    val degrees = numsToFields.run {
        if (first().component !in listOf(DmsComponent.DEGREES, null)) {
            removeFirst().num
        } else {
            0.0
        }
    }
    val minutes = numsToFields.run {
        if (first().component !in listOf(DmsComponent.MINUTES, null)) {
            removeFirst().num
        } else {
            0.0
        }
    }
    val seconds = numsToFields.run {
        if (first().component !in listOf(DmsComponent.SECONDS, null)) {
            removeFirst().num
        } else {
            0.0
        }
    }
    if (numsToFields.isNotEmpty()) {
        return null
    }

    if (
           degrees !in 0.0..180.0 // maximum abs(longitude); for only latitude, max is 90
        || minutes !in 0.0..60.0
        || seconds !in 0.0..60.0
    ) {
        return null
    }

    return if (isPositive) 1.0 else -1.0 * (
        degrees + (1/60f) * (
            minutes + (1/60f) * (
                seconds
            )
        )
    )
}

/**
 * Guarantees that no string pieces are empty strings.
 */
@JvmInline
value class StringWithFields<T>
private constructor(
    private val list: List<Piece<T>>
): List<StringWithFields.Piece<T>> by list {

    val startsWithField get() = first().isField
    val endsWithField get() = last().isField

    val stringPieceCount get() = count { !it.isField }
    val fieldCount get() = count { it.isField }

    val fields get() = mapNotNull { it.field }

    fun toString(fieldToString: (T) -> CharSequence)
        = buildString {
            for (piece in list) {
                piece.ifIsField(
                    then = { append(fieldToString(it)) },
                    otherwise = { append(it) },
                )
            }
        }

    companion object {
        fun <T> CharSequence.parseFields(
            fieldMatcher: Regex,
            fieldExtractor: (String) -> T,
        ) = StringWithFields<T>(
                list = buildList {

                    fun addStringPiece(str: CharSequence) = this@buildList.add( Piece(str) )
                    fun addFieldPiece(fieldStr: String) = this@buildList.add(Piece( fieldExtractor(fieldStr) ))

                    val string = this@parseFields
                    val matches = fieldMatcher.findAll(string)

                    if (matches.none()) {
                        if (string.isNotEmpty()) {
                            addStringPiece(string)
                        }
                        return@buildList
                    }

                    // Each iteration: add next substring + then next field
                    var lastFieldEndedAt = -1
                    for (fieldMatch in matches.drop(1)) {
                        string
                            .substring( (lastFieldEndedAt + 1) ..< fieldMatch.range.start )
                            .letIf({ it.isNotEmpty() }) { addStringPiece(it) }

                        addFieldPiece(fieldMatch.value)

                        lastFieldEndedAt = fieldMatch.range.endInclusive
                    }

                    // Handle last substring (if exists)
                    if (lastFieldEndedAt < string.lastIndex) {
                        string
                            .substring(lastFieldEndedAt + 1 .. string.lastIndex)
                            .letIf({ it.isNotEmpty() }) { addStringPiece(it) }
                    }

                }
            )

        fun CharSequence.parseUnsignedDecimalFields()
            = parseFields(
                @Suppress("RegExpRepeatedSpace")
                Regex("""
                       (?: 
                            \d+         # whole number
                        |
                            \d* \.\d+   # decimal number (no digits required before decimal point)
                        )
                    """, RegexOption.COMMENTS),
                String::toDouble,
            )

        fun CharSequence.parseDecimalFields()
            = parseFields(
                @Suppress("RegExpRepeatedSpace")
                Regex("""
                    -?              # signed
                   (?: 
                        \d+         # whole number
                    |
                        \d* \.\d+   # decimal number (no digits required before decimal point)
                    )
                """, RegexOption.COMMENTS),
                String::toDouble,
            )
    }

    /**
     * Represents either a field (of type T) or a
     * substring of the whole [StringWithFields].
     */
    @ConsistentCopyVisibility
    data class Piece<T>
    private constructor(val string: CharSequence?, val field: T?)
    {
        constructor(string: CharSequence): this(string, null)
        constructor(field: T): this(null, field)

        val isField get() = (field != null)

        fun ifIsField(
            then: (T) -> Unit,
            otherwise: (CharSequence) -> Unit,
        ) {
            field?.let { then(it) }
            ?: string!!.let { otherwise(it) }
        }
    }
}

