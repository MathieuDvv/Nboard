package com.nboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticModeTest {
    @Test
    fun offAndSystem_doNotCreateCustomVibration() {
        assertNull(hapticEffectSpec(HapticMode.OFF))
        assertNull(hapticEffectSpec(HapticMode.SYSTEM))
    }

    @Test
    fun strengthLevels_increaseDurationAndAmplitude() {
        val light = hapticEffectSpec(HapticMode.LIGHT)!!
        val medium = hapticEffectSpec(HapticMode.MEDIUM)!!
        val strong = hapticEffectSpec(HapticMode.STRONG)!!

        assertTrue(light.durationMs < medium.durationMs)
        assertTrue(medium.durationMs < strong.durationMs)
        assertTrue(light.amplitude < medium.amplitude)
        assertTrue(medium.amplitude < strong.amplitude)
        assertEquals(200, strong.amplitude)
    }
}
