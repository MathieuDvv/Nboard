package com.nboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeInterpolationTest {
    @Test
    fun longSegment_includesIntermediateSamplesAndEndpoint() {
        val samples = interpolateSwipeSegment(0f, 0f, 20f, 0f, maxStep = 5f)

        assertEquals(4, samples.size)
        assertEquals(5f, samples.first().x, 0.001f)
        assertEquals(20f, samples.last().x, 0.001f)
        assertEquals(1f, samples.last().fraction, 0.001f)
    }

    @Test
    fun diagonalSamples_neverExceedRequestedStep() {
        val samples = interpolateSwipeSegment(0f, 0f, 12f, 16f, maxStep = 5f)
        var previousX = 0f
        var previousY = 0f
        samples.forEach { sample ->
            val distance = kotlin.math.hypot(
                (sample.x - previousX).toDouble(),
                (sample.y - previousY).toDouble()
            )
            assertTrue(distance <= 5.001)
            previousX = sample.x
            previousY = sample.y
        }
    }

    @Test
    fun hWordTrace_keepsIntentionalTurnsAndDropsCrossedKeys() {
        val tokens = listOf("h", "f", "d", "e", "r", "t", "y", "u", "j", "k", "l", "o")
        val dwell = listOf(8L, 2L, 2L, 38L, 3L, 2L, 2L, 2L, 2L, 3L, 35L, 0L)

        assertEquals(
            listOf("h", "e", "l", "o"),
            reduceSwipeIntentTokens(tokens, dwell)
        )
    }

    @Test
    fun hWordCandidate_acceptsModerateScoreAfterStrictnessAdjustment() {
        assertTrue(isSwipeCandidateConfident(bestScore = 36, secondBestScore = 40))
    }

    @Test
    fun ambiguousWeakCandidate_isStillRejected() {
        assertTrue(!isSwipeCandidateConfident(bestScore = 44, secondBestScore = 49))
    }
}
