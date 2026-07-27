package com.nboard.ime

import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import kotlin.math.ceil
import kotlin.math.ln
import java.util.Locale

internal fun interpolateSwipeSegment(
    fromX: Float,
    fromY: Float,
    toX: Float,
    toY: Float,
    maxStep: Float
): List<SwipeSamplePoint> {
    if (maxStep <= 0f) return listOf(SwipeSamplePoint(toX, toY, 1f))
    val dx = toX - fromX
    val dy = toY - fromY
    val distance = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
    val steps = ceil(distance / maxStep).toInt().coerceIn(1, 64)
    return (1..steps).map { index ->
        val fraction = index.toFloat() / steps
        SwipeSamplePoint(
            x = fromX + dx * fraction,
            y = fromY + dy * fraction,
            fraction = fraction
        )
    }
}

internal fun reduceSwipeIntentTokens(
    tokens: List<String>,
    dwellDurationsMs: List<Long>,
    dwellThresholdMs: Long = SWIPE_DWELL_COMMIT_MS
): List<String> {
    if (tokens.isEmpty()) return emptyList()
    val normalizedTokens = tokens.map { it.lowercase(Locale.ROOT) }
    val reduced = mutableListOf<String>()
    val lastIndex = normalizedTokens.lastIndex
    normalizedTokens.forEachIndexed { index, token ->
        if (token.length != 1 || !token.first().isLetter()) return@forEachIndexed
        val dwell = dwellDurationsMs.getOrNull(index) ?: 0L
        val keep = index == 0 || index == lastIndex || dwell >= dwellThresholdMs
        if (keep && reduced.lastOrNull() != token) {
            reduced.add(token)
        }
    }

    if (reduced.size < 3 && normalizedTokens.size >= 3) {
        val bestMiddle = (1 until normalizedTokens.lastIndex)
            .maxByOrNull { dwellDurationsMs.getOrNull(it) ?: 0L }
            ?.let(normalizedTokens::get)
            ?.takeIf { it.length == 1 && it.first().isLetter() }
        if (!bestMiddle.isNullOrBlank()) {
            val first = reduced.firstOrNull()
            val last = reduced.lastOrNull()
            if (first != null && last != null && bestMiddle != first && bestMiddle != last) {
                reduced.clear()
                reduced.add(first)
                reduced.add(bestMiddle)
                reduced.add(last)
            }
        }
    }
    return reduced
}

internal fun isSwipeCandidateConfident(
    bestScore: Int,
    secondBestScore: Int,
    confidentScore: Int = SWIPE_CONFIDENT_SCORE,
    minimumMargin: Int = SWIPE_MIN_SCORE_MARGIN
): Boolean {
    if (bestScore <= confidentScore) {
        return true
    }
    if (secondBestScore == Int.MAX_VALUE) {
        return false
    }
    return secondBestScore - bestScore >= minimumMargin
}

internal fun NboardImeService.moveCursorLeft() {
    val inputConnection = currentInputConnection ?: return
    inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
    inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
}

internal fun NboardImeService.moveCursorRight() {
    val inputConnection = currentInputConnection ?: return
    inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT))
    inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
}

internal fun NboardImeService.shouldHandleSwipeTyping(): Boolean {
    if (!swipeTypingEnabled) {
        return false
    }
    if (isNumbersMode || isEmojiMode || isClipboardOpen || isAiMode || isGenerating || isVoiceListening || isVoiceStopping) {
        return false
    }
    return true
}

internal fun NboardImeService.isVoiceInputLongPressAvailable(): Boolean {
    if (!voiceInputEnabled) {
        return false
    }
    if (isNumbersMode || isEmojiMode || isClipboardOpen || isAiMode || isGenerating) {
        return false
    }
    return currentInputConnection != null
}

internal fun NboardImeService.beginSwipeTyping(anchorView: View, token: String, rawX: Float, rawY: Float): Boolean {
    if (token.isBlank()) {
        return false
    }
    if (!shouldHandleSwipeTyping()) {
        return false
    }
    val now = SystemClock.elapsedRealtime()
    activeSwipeTypingSession = SwipeTypingSession(
        ownerView = anchorView,
        rawStartX = rawX,
        rawStartY = rawY,
        tokens = mutableListOf(token),
        dwellDurationsMs = mutableListOf(0L),
        trailPoints = mutableListOf(),
        pathPoints = mutableListOf(SwipePoint(rawX, rawY)),
        lastTokenEnteredAtMs = now,
        lastRawX = rawX,
        lastRawY = rawY,
        lastSampleAtMs = now,
        isSwiping = false
    )
    if (swipeTrailEnabled) {
        appendSwipeTrailPoint(rawX, rawY, force = true)
    } else if (isSwipeTrailViewInitialized()) {
        swipeTrailView.fadeOutTrail()
    }
    return true
}

internal fun NboardImeService.cancelSwipeTyping() {
    if (isSwipeTrailViewInitialized()) {
        swipeTrailView.fadeOutTrail()
    }
    activeSwipeTypingSession = null
}

internal fun NboardImeService.updateSwipeTyping(rawX: Float, rawY: Float): Boolean {
    val session = activeSwipeTypingSession ?: return false
    if (!shouldHandleSwipeTyping()) {
        cancelSwipeTyping()
        return false
    }
    val dx = rawX - session.rawStartX
    val dy = rawY - session.rawStartY
    val distance = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
    if (!session.isSwiping && distance >= dp(SWIPE_TYPING_DEADZONE_DP).toFloat()) {
        session.isSwiping = true
    }
    if (!session.isSwiping) {
        return false
    }

    appendSwipeTrailPoint(rawX, rawY, force = false)

    val now = SystemClock.elapsedRealtime()
    val previousAt = session.lastSampleAtMs
    val samples = interpolateSwipeSegment(
        fromX = session.lastRawX,
        fromY = session.lastRawY,
        toX = rawX,
        toY = rawY,
        maxStep = dp(SWIPE_INTERPOLATION_STEP_DP).toFloat()
    )
    samples.forEachIndexed { index, sample ->
        appendSwipePathPoint(session, sample.x, sample.y)
        val token = findSwipeTokenAt(sample.x, sample.y) ?: return@forEachIndexed
        val sampleAt = previousAt + ((now - previousAt) * sample.fraction).toLong()
        recordSwipeToken(
            session = session,
            token = token,
            sampleAtMs = sampleAt,
            emitHaptic = index == samples.lastIndex
        )
    }
    session.lastRawX = rawX
    session.lastRawY = rawY
    session.lastSampleAtMs = now
    return true
}

private fun NboardImeService.appendSwipePathPoint(
    session: SwipeTypingSession,
    rawX: Float,
    rawY: Float
) {
    val point = SwipePoint(rawX, rawY)
    val previous = session.pathPoints.lastOrNull()
    if (previous != null) {
        val dx = point.x - previous.x
        val dy = point.y - previous.y
        val minimumStep = dp(SWIPE_PATH_MIN_STEP_DP).toFloat()
        if (dx * dx + dy * dy < minimumStep * minimumStep) {
            return
        }
    }
    session.pathPoints.add(point)
    if (session.pathPoints.size > SWIPE_PATH_MAX_POINTS) {
        // Keep the exact gesture endpoints while thinning the oldest interior sample.
        session.pathPoints.removeAt(1)
    }
}

private fun NboardImeService.recordSwipeToken(
    session: SwipeTypingSession,
    token: String,
    sampleAtMs: Long,
    emitHaptic: Boolean
) {
    if (token == session.tokens.lastOrNull()) return
    val lastIndex = session.dwellDurationsMs.lastIndex
    if (lastIndex >= 0) {
        val delta = (sampleAtMs - session.lastTokenEnteredAtMs).coerceAtLeast(0L)
        session.dwellDurationsMs[lastIndex] = session.dwellDurationsMs[lastIndex] + delta
    }
    session.tokens.add(token)
    session.dwellDurationsMs.add(0L)
    session.lastTokenEnteredAtMs = sampleAtMs
    if (emitHaptic) {
        performKeyHaptic(session.ownerView)
    }
}

internal fun NboardImeService.finishSwipeTypingAndCommit(): Boolean {
    val session = activeSwipeTypingSession ?: return false
    activeSwipeTypingSession = null
    if (isSwipeTrailViewInitialized()) {
        swipeTrailView.fadeOutTrail()
    }
    if (!session.isSwiping) {
        return false
    }
    val now = SystemClock.elapsedRealtime()
    val lastIndex = session.dwellDurationsMs.lastIndex
    if (lastIndex >= 0) {
        val delta = (now - session.lastTokenEnteredAtMs).coerceAtLeast(0L)
        session.dwellDurationsMs[lastIndex] = session.dwellDurationsMs[lastIndex] + delta
    }

    val intentTokens = extractSwipeIntentTokens(session)
    if (intentTokens.size < 2) {
        return false
    }
    val resolved = resolveSwipeWord(intentTokens, session).orEmpty()
    if (resolved.isBlank()) {
        return false
    }
    commitSwipeWord(resolved)
    return true
}

internal fun NboardImeService.appendSwipeTrailPoint(rawX: Float, rawY: Float, force: Boolean) {
    if (!swipeTrailEnabled) {
        return
    }
    val session = activeSwipeTypingSession ?: return
    if (!isKeyRowsContainerInitialized() || !isSwipeTrailViewInitialized()) {
        return
    }
    val location = IntArray(2)
    keyRowsContainer.getLocationOnScreen(location)
    val localX = rawX - location[0]
    val localY = rawY - location[1]
    if (!force) {
        val last = session.trailPoints.lastOrNull()
        if (last != null) {
            val dx = localX - last.x
            val dy = localY - last.y
            val minDistance = dp(SWIPE_TRAIL_MIN_STEP_DP).toFloat()
            if ((dx * dx + dy * dy) < (minDistance * minDistance)) {
                return
            }
        }
    }
    session.trailPoints.add(
        SwipeTrailView.TrailPoint(
            x = localX,
            y = localY,
            timestampMs = SystemClock.elapsedRealtime()
        )
    )
    if (session.trailPoints.size > SWIPE_TRAIL_MAX_POINTS) {
        session.trailPoints.removeAt(0)
    }
    swipeTrailView.updateTrail(session.trailPoints)
}

internal fun NboardImeService.findSwipeTokenAt(rawX: Float, rawY: Float): String? {
    val hitSlop = dp(SWIPE_KEY_HIT_SLOP_DP).toFloat()
    var bestToken: String? = null
    var bestDistanceSquared = Float.MAX_VALUE
    val location = IntArray(2)
    swipeLetterKeyByView.forEach { (view, token) ->
        if (!view.isShown || view.width <= 0 || view.height <= 0) {
            return@forEach
        }
        view.getLocationOnScreen(location)
        val left = location[0].toFloat() - hitSlop
        val top = location[1].toFloat() - hitSlop
        val right = location[0].toFloat() + view.width + hitSlop
        val bottom = location[1].toFloat() + view.height + hitSlop
        if (rawX in left..right && rawY in top..bottom) {
            val centerX = location[0] + view.width / 2f
            val centerY = location[1] + view.height / 2f
            val dx = rawX - centerX
            val dy = rawY - centerY
            val distanceSquared = dx * dx + dy * dy
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared
                bestToken = token
            }
        }
    }
    return bestToken
}

internal fun NboardImeService.extractSwipeIntentTokens(session: SwipeTypingSession): List<String> {
    return reduceSwipeIntentTokens(session.tokens, session.dwellDurationsMs)
}

internal fun NboardImeService.resolveSwipeWord(tokens: List<String>, session: SwipeTypingSession): String? {
    return resolveSwipeWordByGeometry(tokens, session)
        ?: resolveSwipeWordByTokens(tokens, session)
}

private data class SwipeKeyboardGeometry(
    val keyCenters: Map<Char, SwipePoint>,
    val keySize: Float
)

private data class ScoredSwipeWord(
    val word: String,
    val geometryScore: Float,
    val adjustedScore: Float
)

private fun NboardImeService.resolveSwipeWordByGeometry(
    tokens: List<String>,
    session: SwipeTypingSession
): String? {
    if (tokens.isEmpty() || session.pathPoints.size < 2) return null
    val pathFirst = tokens.firstNotNullOfOrNull { token ->
        foldWord(normalizeWord(token)).firstOrNull { it.isLetter() }
    } ?: return null
    val keyboardGeometry = currentSwipeKeyboardGeometry() ?: return null
    if (!keyboardGeometry.keyCenters.containsKey(pathFirst)) return null

    val inputConnection = currentInputConnection
    val beforeCursor = inputConnection
        ?.getTextBeforeCursor(PREDICTION_CONTEXT_WINDOW, 0)
        ?.toString()
        .orEmpty()
    val sentenceContext = extractPredictionSentenceContext(beforeCursor)
    val (previousWord2, previousWord1) = extractPreviousWordsForPrediction(sentenceContext, "")
    val contextLanguage = detectContextLanguage(beforeCursor)

    val candidateByFoldedWord = LinkedHashMap<String, String>()
    learnedWordFrequency.entries
        .asSequence()
        .filter { (word, _) -> foldWord(word).firstOrNull() == pathFirst }
        .sortedByDescending { it.value }
        .take(SWIPE_LEARNED_SCAN_LIMIT)
        .forEach { (word, _) ->
            val normalized = normalizeWord(word)
            candidateByFoldedWord.putIfAbsent(foldWord(normalized), normalized)
        }
    listOf(
        KeyboardLanguageMode.FRENCH to frenchLexicon,
        KeyboardLanguageMode.ENGLISH to englishLexicon
    ).forEach { (language, lexicon) ->
        if (!isLanguageEnabled(language)) return@forEach
        lexicon.byFirst[pathFirst]
            .orEmpty()
            .asSequence()
            .take(SWIPE_LEXICON_SCAN_LIMIT)
            .forEach { word ->
                val normalized = normalizeWord(word)
                candidateByFoldedWord.putIfAbsent(foldWord(normalized), normalized)
            }
    }
    if (candidateByFoldedWord.isEmpty()) return null

    val matches = rankSwipeGeometryCandidates(
        trace = session.pathPoints,
        foldedCandidates = candidateByFoldedWord.keys,
        keyCenters = keyboardGeometry.keyCenters,
        keySize = keyboardGeometry.keySize
    )
    if (matches.isEmpty()) return null

    val scored = matches.asSequence()
        .mapNotNull { match ->
            val candidate = candidateByFoldedWord[match.word] ?: return@mapNotNull null
            var adjustment = 0f
            val unigram = learnedWordFrequency[candidate] ?: 0
            if (unigram > 0) {
                adjustment -= (ln(1.0 + unigram) * 0.035).toFloat().coerceAtMost(0.18f)
            }
            if (!previousWord1.isNullOrBlank()) {
                val bigram = learnedBigramFrequency[predictionBigramKey(previousWord1, candidate)] ?: 0
                if (bigram > 0) {
                    adjustment -= (ln(1.0 + bigram) * 0.07).toFloat().coerceAtMost(0.28f)
                }
            }
            if (!previousWord2.isNullOrBlank() && !previousWord1.isNullOrBlank()) {
                val trigram = learnedTrigramFrequency[
                    predictionTrigramKey(previousWord2, previousWord1, candidate)
                ] ?: 0
                if (trigram > 0) {
                    adjustment -= (ln(1.0 + trigram) * 0.09).toFloat().coerceAtMost(0.38f)
                }
            }
            if (FRENCH_WORDS.contains(candidate) || ENGLISH_WORDS.contains(candidate)) {
                adjustment -= 0.025f
            }
            detectWordLanguage(candidate)?.let { language ->
                adjustment += languageBiasPenalty(language, contextLanguage) * 0.035f
            }
            ScoredSwipeWord(
                word = candidate,
                geometryScore = match.score,
                adjustedScore = match.score + adjustment
            )
        }
        .sortedBy { it.adjustedScore }
        .take(2)
        .toList()
    val best = scored.firstOrNull() ?: return null
    val secondScore = scored.getOrNull(1)?.adjustedScore ?: Float.POSITIVE_INFINITY
    val margin = secondScore - best.adjustedScore
    return best.word.takeIf {
        best.geometryScore <= SWIPE_GEOMETRY_CONFIDENT_SCORE ||
            (best.geometryScore <= SWIPE_GEOMETRY_FALLBACK_SCORE &&
                margin >= SWIPE_GEOMETRY_MIN_MARGIN)
    }
}

private fun NboardImeService.currentSwipeKeyboardGeometry(): SwipeKeyboardGeometry? {
    val keyCenters = LinkedHashMap<Char, SwipePoint>()
    val keySizes = mutableListOf<Float>()
    val location = IntArray(2)
    swipeLetterKeyByView.forEach { (view, token) ->
        if (!view.isShown || view.width <= 0 || view.height <= 0) return@forEach
        val foldedToken = foldWord(normalizeWord(token))
        val char = foldedToken.singleOrNull()?.takeIf { it.isLetter() } ?: return@forEach
        view.getLocationOnScreen(location)
        keyCenters.putIfAbsent(
            char,
            SwipePoint(
                x = location[0] + view.width / 2f,
                y = location[1] + view.height / 2f
            )
        )
        keySizes.add(minOf(view.width, view.height).toFloat())
    }
    if (keyCenters.size < 2 || keySizes.isEmpty()) return null
    keySizes.sort()
    return SwipeKeyboardGeometry(
        keyCenters = keyCenters,
        keySize = keySizes[keySizes.size / 2].coerceAtLeast(1f)
    )
}

private fun NboardImeService.resolveSwipeWordByTokens(
    tokens: List<String>,
    session: SwipeTypingSession
): String? {
    if (tokens.isEmpty()) {
        return null
    }
    val normalizedPath = tokens
        .map { normalizeWord(it) }
        .filter { it.length == 1 && it.first().isLetter() }
        .joinToString("")
    if (normalizedPath.length < 2) {
        return null
    }

    val foldedPath = foldWord(normalizedPath)
    val collapsedPath = collapseRepeats(foldedPath, maxRepeat = 1)
    val pathFirst = collapsedPath.firstOrNull() ?: return null
    val pathLast = collapsedPath.lastOrNull() ?: return null

    val inputConnection = currentInputConnection
    val beforeCursor = inputConnection
        ?.getTextBeforeCursor(PREDICTION_CONTEXT_WINDOW, 0)
        ?.toString()
        .orEmpty()
    val sentenceContext = extractPredictionSentenceContext(beforeCursor)
    val (previousWord2, previousWord1) = extractPreviousWordsForPrediction(sentenceContext, "")
    val contextLanguage = detectContextLanguage(beforeCursor)

    val candidates = LinkedHashSet<String>()
    learnedWordFrequency.entries
        .asSequence()
        .filter { (word, _) -> word.firstOrNull() == pathFirst }
        .sortedByDescending { it.value }
        .take(SWIPE_LEARNED_SCAN_LIMIT)
        .forEach { (word, _) -> candidates.add(word) }

    listOf(
        KeyboardLanguageMode.FRENCH to frenchLexicon,
        KeyboardLanguageMode.ENGLISH to englishLexicon
    ).forEach { (language, lexicon) ->
        if (!isLanguageEnabled(language)) {
            return@forEach
        }
        lexicon.byFirst[pathFirst]
            .orEmpty()
            .asSequence()
            .take(SWIPE_LEXICON_SCAN_LIMIT)
            .forEach { candidates.add(it) }
    }

    var bestWord: String? = null
    var bestScore = Int.MAX_VALUE
    var secondBestScore = Int.MAX_VALUE

    candidates.forEach { candidate ->
        val normalizedCandidate = normalizeWord(candidate)
        if (normalizedCandidate.length < 2) {
            return@forEach
        }
        val foldedCandidate = foldWord(normalizedCandidate)
        if (foldedCandidate.isBlank() || foldedCandidate.firstOrNull() != pathFirst) {
            return@forEach
        }

        val collapsedCandidate = collapseRepeats(foldedCandidate, maxRepeat = 1)
        val distanceLimit = (SWIPE_DISTANCE_BASE_LIMIT + collapsedPath.length / 2).coerceAtMost(10)
        val shapeDistance = levenshteinDistanceBounded(collapsedPath, collapsedCandidate, distanceLimit)
        if (shapeDistance == Int.MAX_VALUE) {
            return@forEach
        }

        var score = shapeDistance * 14
        score += kotlin.math.abs(collapsedCandidate.length - collapsedPath.length) * 3
        val rawDistanceLimit = (distanceLimit + 2).coerceAtMost(12)
        val rawDistance = levenshteinDistanceBounded(foldedPath, foldedCandidate, rawDistanceLimit)
        score += if (rawDistance == Int.MAX_VALUE) 28 else rawDistance * 7
        score += swipeBigramMismatchPenalty(collapsedPath, collapsedCandidate)
        score -= commonPrefixLength(collapsedPath, collapsedCandidate) * 3
        if (collapsedCandidate.lastOrNull() != pathLast) {
            score += 12
        }
        if (!isSubsequence(collapsedPath, collapsedCandidate)) {
            score += 16
        }

        val dominantMiddle = dominantSwipeMiddleToken(session)
        if (!dominantMiddle.isNullOrBlank() && !collapsedCandidate.contains(dominantMiddle)) {
            score += 8
        }

        val unigram = learnedWordFrequency[normalizedCandidate] ?: 0
        if (unigram > 0) {
            score -= minOf(90, unigram * 8)
        }

        if (!previousWord1.isNullOrBlank()) {
            val bigram = learnedBigramFrequency[predictionBigramKey(previousWord1, normalizedCandidate)] ?: 0
            if (bigram > 0) {
                score -= minOf(120, bigram * 20)
            }
        }
        if (!previousWord2.isNullOrBlank() && !previousWord1.isNullOrBlank()) {
            val trigram = learnedTrigramFrequency[
                predictionTrigramKey(previousWord2, previousWord1, normalizedCandidate)
            ] ?: 0
            if (trigram > 0) {
                score -= minOf(170, trigram * 24)
            }
        }

        if (FRENCH_WORDS.contains(normalizedCandidate) || ENGLISH_WORDS.contains(normalizedCandidate)) {
            score -= 6
        }
        detectWordLanguage(normalizedCandidate)?.let { language ->
            score += languageBiasPenalty(language, contextLanguage) * 6
        }

        if (score < bestScore) {
            secondBestScore = bestScore
            bestScore = score
            bestWord = normalizedCandidate
        } else if (score < secondBestScore) {
            secondBestScore = score
        }
    }

    if (!bestWord.isNullOrBlank()) {
        if (isSwipeCandidateConfident(bestScore, secondBestScore)) {
            return bestWord
        }
    }
    return null
}

internal fun NboardImeService.dominantSwipeMiddleToken(session: SwipeTypingSession): String? {
    if (session.tokens.size < 3) {
        return null
    }
    val middleRange = 1 until session.tokens.lastIndex
    val bestMiddleIndex = middleRange.maxByOrNull { index ->
        session.dwellDurationsMs.getOrNull(index) ?: 0L
    } ?: return null
    return normalizeWord(session.tokens[bestMiddleIndex])
        .takeIf { it.length == 1 && it.first().isLetter() }
}

internal fun NboardImeService.swipeBigramMismatchPenalty(path: String, candidate: String): Int {
    if (path.length < 2 || candidate.length < 2) {
        return 0
    }
    var penalty = 0
    for (index in 0 until path.lastIndex) {
        val from = path[index]
        val to = path[index + 1]
        val firstPos = candidate.indexOf(from)
        if (firstPos < 0) {
            penalty += 6
            continue
        }
        val secondPos = candidate.indexOf(to, firstPos + 1)
        if (secondPos < 0) {
            penalty += 5
            continue
        }
        val gap = secondPos - firstPos - 1
        penalty += (gap * 2).coerceAtMost(6)
    }
    return penalty
}

internal fun NboardImeService.detectWordLanguage(word: String): KeyboardLanguageMode? {
    val folded = foldWord(word)
    val inFrench = frenchLexicon.words.contains(word) || frenchLexicon.foldedWords.contains(folded)
    val inEnglish = englishLexicon.words.contains(word) || englishLexicon.foldedWords.contains(folded)
    return when {
        inFrench && !inEnglish -> KeyboardLanguageMode.FRENCH
        inEnglish && !inFrench -> KeyboardLanguageMode.ENGLISH
        else -> null
    }
}

internal fun NboardImeService.isSubsequence(pattern: String, source: String): Boolean {
    if (pattern.isEmpty()) {
        return true
    }
    var patternIndex = 0
    source.forEach { char ->
        if (patternIndex < pattern.length && pattern[patternIndex] == char) {
            patternIndex++
        }
    }
    return patternIndex == pattern.length
}
