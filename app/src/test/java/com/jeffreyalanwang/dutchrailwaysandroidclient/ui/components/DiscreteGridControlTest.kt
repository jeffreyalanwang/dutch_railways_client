package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.components

import androidx.compose.ui.Alignment
import org.junit.Assert.assertEquals
import org.junit.Test

class DiscreteGridControlTest {

    @Test
    fun `update should initialize widths on first call`() {
        val control = DiscreteGridControl()
        val incomingWidths = listOf(50, null, 100)
        control.update(gap = 10, totalWidth = 500, centerIdx = 1, incomingWidths = incomingWidths)

        assertEquals(50, control.widths[0])
        assertEquals(null, control.widths[1])
        assertEquals(100, control.widths[2])
        assertEquals(500, control.totalWidth)
    }

    @Test
    fun `update should expand widths on subsequent calls with larger values`() {
        val control = DiscreteGridControl()
        control.update(gap = 10, totalWidth = 500, centerIdx = 1, incomingWidths = listOf(50, null, 100))
        
        // Second update with larger width for first column
        control.update(gap = 10, totalWidth = 500, centerIdx = 1, incomingWidths = listOf(70, null, 80))

        assertEquals(70, control.widths[0]) // Expanded from 50 to 70
        assertEquals(null, control.widths[1])
        assertEquals(100, control.widths[2]) // Kept 100 because 80 < 100
    }

    @Test(expected = IllegalStateException::class)
    fun `centerFillWidth should throw if no center fill item exists`() {
        val control = DiscreteGridControl()
        control.update(gap = 10, totalWidth = 500, centerIdx = null, incomingWidths = listOf(50, 100))
        control.centerFillWidth()
    }

    @Test
    fun `centerFillWidth should calculate remaining space correctly`() {
        val control = DiscreteGridControl()
        // totalWidth(500) - (widths(50 + 100) + gap(10 * (3-1))) = 500 - (150 + 20) = 330
        control.update(gap = 10, totalWidth = 500, centerIdx = 1, incomingWidths = listOf(50, null, 100))
        assertEquals(330, control.centerFillWidth())
    }

    @Test
    fun `positions should calculate correct x-coordinates without center fill`() {
        val control = DiscreteGridControl()
        control.update(gap = 10, totalWidth = 500, centerIdx = null, incomingWidths = listOf(50, 100))
        
        val alignments = listOf(Alignment.Start, Alignment.End)
        val itemWidths = listOf(50, 80)
        
        val pos = control.positions(alignments, itemWidths)
        
        // Col 0: left=0, right=50. Item width 50, Start -> 0
        assertEquals(0, pos[0])
        // Col 1: left=60, right=160. Item width 80, End -> 160 - 80 = 80
        assertEquals(80, pos[1])
    }

    @Test
    fun `positions should calculate correct x-coordinates with center fill`() {
        val control = DiscreteGridControl()
        control.update(gap = 10, totalWidth = 500, centerIdx = 1, incomingWidths = listOf(50, null, 100))
        
        val alignments = listOf(Alignment.Start, Alignment.CenterHorizontally, Alignment.End)
        val itemWidths = listOf(50, 200, 100)
        
        val pos = control.positions(alignments, itemWidths)
        
        // Col 0 (Rigid): left=0, right=50. Start -> 0
        assertEquals(0, pos[0])
        // Col 2 (Rigid): right=500, left=500-100=400. End -> 400 (if item width 100)
        assertEquals(400, pos[2])
        // Col 1 (Center Fill): left=60, right=390. Center -> 60 + (330 - 200) / 2 = 60 + 65 = 125
        assertEquals(125, pos[1])
    }
    
    @Test
    fun `positions should handle Alignment_CenterHorizontally for rigid items`() {
        val control = DiscreteGridControl()
        control.update(gap = 10, totalWidth = 500, centerIdx = null, incomingWidths = listOf(100))
        
        val pos = control.positions(listOf(Alignment.CenterHorizontally), listOf(60))
        
        // Col 0: left=0, right=100. Center -> 0 + (100 - 60) / 2 = 20
        assertEquals(20, pos[0])
    }
}
