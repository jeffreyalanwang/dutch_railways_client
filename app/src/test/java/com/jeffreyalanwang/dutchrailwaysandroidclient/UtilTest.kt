package com.jeffreyalanwang.dutchrailwaysandroidclient

import android.location.Address
import com.google.android.gms.maps.model.LatLng
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilTest {

    @Test
    fun testCheckAll() {
        checkAll(listOf(1, 2, 3)) { it > 0 }
        // Should throw if one fails
        try {
            checkAll(listOf(1, -2, 3)) { it > 0 }
            assertTrue("Should have thrown IllegalStateException", false)
        } catch (_: IllegalStateException) {
            // Success
        }
    }

    @Test
    fun testLetBothElvis() {
        val pair: Pair<Int?, String?> = 1 to "a"
        assertEquals(2, pair.letBothElvis { (a, b) -> a + b.length })
        
        val pairNull: Pair<Int?, String?> = null to "a"
        assertNull(pairNull.letBothElvis { (a, b) -> a + b.length })
    }

    @Test
    fun testTranspose() {
        val p = (1 to 2) to (3 to 4)
        val t = p.transpose()
        assertEquals(1 to 3, t.first)
        assertEquals(2 to 4, t.second)
    }

    @Test
    fun testTransposeWithBlock() {
        val p = (1 to 1) to (2 to 2)
        val t = p.transpose { (a, b) -> (a + b) to (a * b) }
        assertEquals(3 to 2, t.first)
        assertEquals(3 to 2, t.second)
    }

    @Test
    fun testTripleMap() {
        val triple = Triple(1, 2, 3)
        assertEquals(Triple(2, 4, 6), triple.map { it * 2 })
    }

    @Test
    fun testAddNotNull() {
        val list = mutableListOf<Int>()
        list.addNotNull(1)
        list.addNotNull(null)
        assertEquals(1, list.size)
        assertEquals(1, list[0])
    }

    @Test
    fun testAddAllNotNull() {
        val list = mutableListOf<Int>()
        list.addAllNotNull(listOf(1, null, 2))
        assertEquals(2, list.size)
        assertEquals(listOf(1, 2), list)
    }

    @Test
    fun testIterableAll() {
        assertTrue(listOf(true, true).all())
        assertFalse(listOf(true, false).all())
    }

    @Test
    fun testLetIf() {
        assertEquals("YES", 1.letIf({ it > 0 }, { "YES" }, { "NO" }))
        assertEquals("NO", (-1).letIf({ it > 0 }, { "YES" }, { "NO" }))
        
        assertEquals(2, 1.letIf({ it > 0 }) { it + 1 })
        assertEquals(-1, (-1).letIf({ it > 0 }) { it + 1 })

        assertEquals(2, 1.letIf(true) { it + 1 })
        assertEquals(1, 1.letIf(false) { it + 1 })
    }

    @Test
    fun testEqualOn() {
        val p1 = 1 to 1
        assertTrue(p1.equalOn { it })
        
        val p2 = 1 to 2
        assertFalse(p2.equalOn { it })
        
        val p3 = "abc" to "abd"
        assertTrue(p3.equalOn { it.length })
    }

    @Test
    fun testValueOrLazy() {
        var count = 0
        val lazyVal by ValueOrLazy { count++; "lazy" }
        assertEquals("lazy", lazyVal)
        assertEquals(1, count)
        assertEquals("lazy", lazyVal)
        assertEquals(1, count)

        val valProvided by ValueOrLazy("provided") { count++; "lazy" }
        assertEquals("provided", valProvided)
        assertEquals(1, count)
    }

    @Test
    fun testSuspendLazy() = runBlocking {
        var count = 0
        val lazyVal = SuspendLazy { count++; "lazy" }
        assertEquals("lazy", lazyVal.getValue())
        assertEquals(1, count)
        assertEquals("lazy", lazyVal.getValue())
        assertEquals(1, count)
    }

    @Test
    fun testAddressExtensions() {
        val address = mockk<Address>()
        every { address.latitude } returns 10.0
        every { address.longitude } returns 20.0
        every { address.maxAddressLineIndex } returns 1
        every { address.getAddressLine(0) } returns "Line 1"
        every { address.getAddressLine(1) } returns "Line 2"

        assertEquals(LatLng(10.0, 20.0), address.latLng)
        assertEquals(listOf("Line 1", "Line 2"), address.addressLines)
        assertEquals("Line 1, Line 2", address.addressString)
    }

    @Test
    fun testFloatInterpolatesTriple() {
        val triple = Triple(0, 10, 100)
        assertEquals(0, 0f interpolates triple)
        assertEquals(5, 0.25f interpolates triple)
        assertEquals(10, 0.5f interpolates triple)
        assertEquals(55, 0.75f interpolates triple)
        assertEquals(100, 1f interpolates triple)

        val tripleF = Triple(0f, 10f, 100f)
        assertEquals(0f, 0f interpolates tripleF)
        assertEquals(5f, 0.25f interpolates tripleF)
        assertEquals(10f, 0.5f interpolates tripleF)
        assertEquals(55f, 0.75f interpolates tripleF)
        assertEquals(100f, 1f interpolates tripleF)
    }

    @Test
    fun testFloatInterpolatesPair() {
        val pairF = 0f to 10f
        assertEquals(0f, 0f interpolates pairF)
        assertEquals(5f, 0.5f interpolates pairF)
        assertEquals(10f, 1f interpolates pairF)

        val pairI = 0 to 10
        assertEquals(0, 0f interpolates pairI)
        assertEquals(5, 0.5f interpolates pairI)
        assertEquals(10, 1f interpolates pairI)
    }

    @Test
    fun testCompareToRange() {
        val range: ClosedRange<Int> = 1..10
        assertEquals(-1, 0.compareTo(range))
        assertEquals(0, 1.compareTo(range))
        assertEquals(0, 5.compareTo(range))
        assertEquals(0, 10.compareTo(range))
        assertEquals(1, 11.compareTo(range))

        val openRange: OpenEndRange<Int> = 1..<10
        assertEquals(-1, 0.compareTo(openRange))
        assertEquals(0, 1.compareTo(openRange))
        assertEquals(0, 9.compareTo(openRange))
        assertEquals(1, 10.compareTo(openRange))
    }

    @Test
    fun testRangeCompareToValue() {
        val range: ClosedRange<Int> = 1..10
        assertEquals(1, range.compareTo(0))
        assertEquals(0, range.compareTo(5))
        assertEquals(-1, range.compareTo(11))
    }

    @Test
    fun testToIntPercent() {
        assertEquals(50, 0.5f.toIntPercent())
        assertEquals(100, 1f.toIntPercent())
    }

    @Test
    fun testDoubleExtensions() {
        assertTrue(1.0.isWholeNumber())
        assertFalse(1.1.isWholeNumber())
        
        val (int, dec) = 1.25.toParts()
        assertEquals(1, int)
        assertEquals(0.25, dec, 0.0001)
    }

    @Test
    fun testUnfancyQuotes() {
        val fancy = "\u201CHello\u201D \u2018World\u2019"
        assertEquals("\"Hello\" 'World'", fancy.unfancyQuotes())
    }
}
