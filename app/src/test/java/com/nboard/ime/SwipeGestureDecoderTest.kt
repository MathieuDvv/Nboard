package com.nboard.ime

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeGestureDecoderTest {
    private val qwertyCenters = buildMap {
        "qwertyuiop".forEachIndexed { index, char ->
            put(char, SwipePoint(index.toFloat(), 0f))
        }
        "asdfghjkl".forEachIndexed { index, char ->
            put(char, SwipePoint(index + 0.25f, 1f))
        }
        "zxcvbnm".forEachIndexed { index, char ->
            put(char, SwipePoint(index + 0.75f, 2f))
        }
    }

    @Test
    fun resamplingPreservesGestureEndpoints() {
        val result = resampleSwipePath(
            listOf(
                SwipePoint(1f, 2f),
                SwipePoint(4f, 2f),
                SwipePoint(4f, 8f)
            ),
            pointCount = 12
        )

        assertEquals(SwipePoint(1f, 2f), result.first())
        assertEquals(SwipePoint(4f, 8f), result.last())
    }

    @Test
    fun helloGestureWinsDespiteCrossingUnrelatedKeys() {
        val trace = listOf(
            qwertyCenters.getValue('h'),
            SwipePoint(4.5f, 0.8f),
            SwipePoint(3.3f, 0.35f),
            qwertyCenters.getValue('e'),
            SwipePoint(4.1f, 0.25f),
            SwipePoint(6.4f, 0.75f),
            qwertyCenters.getValue('l'),
            qwertyCenters.getValue('o')
        )

        val matches = rankSwipeGeometryCandidates(
            trace = trace,
            foldedCandidates = listOf("hero", "help", "held", "hello"),
            keyCenters = qwertyCenters,
            keySize = 1f
        )

        assertEquals("hello", matches.first().word)
        assertTrue(matches.first().score < SWIPE_GEOMETRY_CONFIDENT_SCORE)
    }

    @Test
    fun finalFingerLocationDisambiguatesSimilarWordTemplates() {
        val trace = buildSwipeWordTemplate("hello", qwertyCenters).orEmpty()

        val matches = rankSwipeGeometryCandidates(
            trace = trace,
            foldedCandidates = listOf("hello", "hellp"),
            keyCenters = qwertyCenters,
            keySize = 1f
        )

        assertEquals("hello", matches.first().word)
        assertTrue(matches.first().endDistance < matches.last().endDistance)
    }

    @Test
    fun candidateUsingUnavailableLayoutLettersIsSkipped() {
        val trace = buildSwipeWordTemplate("hello", qwertyCenters).orEmpty()
        val centersWithoutO = qwertyCenters - 'o'

        val matches = rankSwipeGeometryCandidates(
            trace = trace,
            foldedCandidates = listOf("hello"),
            keyCenters = centersWithoutO,
            keySize = 1f
        )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun fullEnglishHBucketRanksHelloFirst() {
        val projectDirectory = File(System.getProperty("user.dir") ?: ".")
        val dictionary = sequenceOf(
            projectDirectory.resolve("app/src/main/assets/dictionaries/english_50k.txt"),
            projectDirectory.resolve("src/main/assets/dictionaries/english_50k.txt")
        ).first(File::isFile)
        val candidates = dictionary.useLines { lines ->
            lines.mapNotNull { line ->
                line.substringBefore(' ')
                    .lowercase()
                    .takeIf { word -> word.startsWith('h') && word.all(Char::isLetter) }
            }.take(SWIPE_LEXICON_SCAN_LIMIT).toList()
        }
        val trace = listOf(
            qwertyCenters.getValue('h'),
            SwipePoint(4.5f, 0.8f),
            SwipePoint(3.3f, 0.35f),
            qwertyCenters.getValue('e'),
            SwipePoint(4.1f, 0.25f),
            SwipePoint(6.4f, 0.75f),
            qwertyCenters.getValue('l'),
            qwertyCenters.getValue('o')
        )

        val matches = rankSwipeGeometryCandidates(
            trace = trace,
            foldedCandidates = candidates,
            keyCenters = qwertyCenters,
            keySize = 1f
        )

        assertEquals("hello", matches.first().word)
    }
}
