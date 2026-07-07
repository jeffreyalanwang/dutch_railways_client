package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.jeffreyalanwang.dutchrailwaysandroidclient.Station
import org.junit.Assert.assertEquals
import org.junit.Test

class FuncsTest {

    @Test
    fun testLatLngBoundsForDisplay() {
        val latLng = LatLng(52.0, 5.0)
        val bounds = latLng.boundsForDisplay()
        
        val latPadding = 1.0 / 60
        val lngPadding = 1.0 / 90
        
        assertEquals(52.0 - latPadding, bounds.southwest.latitude, 0.000001)
        assertEquals(5.0 - lngPadding, bounds.southwest.longitude, 0.000001)
        assertEquals(52.0 + latPadding, bounds.northeast.latitude, 0.000001)
        assertEquals(5.0 + lngPadding, bounds.northeast.longitude, 0.000001)
    }

    @Test
    fun testIntOffsetInterpolates() {
        val triple = Triple(IntOffset(0, 0), IntOffset(10, 20), IntOffset(100, 200))
        
        assertEquals(IntOffset(0, 0), 0f interpolates triple)
        assertEquals(IntOffset(5, 10), 0.25f interpolates triple)
        assertEquals(IntOffset(10, 20), 0.5f interpolates triple)
        assertEquals(IntOffset(55, 110), 0.75f interpolates triple)
        assertEquals(IntOffset(100, 200), 1f interpolates triple)
    }

    @Test
    fun testIntSizeInterpolates() {
        val triple = Triple(IntSize(0, 0), IntSize(10, 20), IntSize(100, 200))
        
        assertEquals(IntSize(0, 0), 0f interpolates triple)
        assertEquals(IntSize(5, 10), 0.25f interpolates triple)
        assertEquals(IntSize(10, 20), 0.5f interpolates triple)
        assertEquals(IntSize(55, 110), 0.75f interpolates triple)
        assertEquals(IntSize(100, 200), 1f interpolates triple)
    }
}
