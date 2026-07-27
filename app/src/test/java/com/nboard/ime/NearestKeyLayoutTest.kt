package com.nboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NearestKeyLayoutTest {
    private val rects = listOf(
        KeyHitRect(0, 0, 40, 50),
        KeyHitRect(44, 0, 84, 50),
        KeyHitRect(0, 58, 40, 108)
    )

    @Test
    fun horizontalGap_routesToNearestKey() {
        assertEquals(0, nearestKeyRectIndex(41f, 25f, rects, 24f))
        assertEquals(1, nearestKeyRectIndex(43f, 25f, rects, 24f))
    }

    @Test
    fun verticalGap_routesToNearestRow() {
        assertEquals(0, nearestKeyRectIndex(20f, 53f, rects, 24f))
        assertEquals(2, nearestKeyRectIndex(20f, 56f, rects, 24f))
    }

    @Test
    fun distantTouch_isNotRouted() {
        assertNull(nearestKeyRectIndex(200f, 200f, rects, 24f))
    }
}
