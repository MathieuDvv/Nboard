package com.nboard.ime

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * A point in screen coordinates. The decoder intentionally uses the same coordinate
 * space as the rendered keys so location, not only the gesture's abstract shape,
 * helps disambiguate nearby words.
 */
internal data class SwipePoint(
    val x: Float,
    val y: Float
)

internal data class SwipeGeometryMatch(
    val word: String,
    val score: Float,
    val startDistance: Float,
    val endDistance: Float
)

private data class PreparedSwipePath(
    val points: List<SwipePoint>,
    val length: Float
)

private const val SWIPE_RESAMPLE_POINT_COUNT = 24
private const val SWIPE_DTW_BAND_RADIUS = 5
private const val SWIPE_POINT_EPSILON = 0.01f

internal fun resampleSwipePath(
    source: List<SwipePoint>,
    pointCount: Int = SWIPE_RESAMPLE_POINT_COUNT
): List<SwipePoint> {
    if (source.isEmpty() || pointCount <= 0) return emptyList()
    if (pointCount == 1) return listOf(source.first())

    val points = ArrayList<SwipePoint>(source.size)
    source.forEach { point ->
        val previous = points.lastOrNull()
        if (previous == null || distance(previous, point) >= SWIPE_POINT_EPSILON) {
            points.add(point)
        }
    }
    if (points.size == 1) {
        return List(pointCount) { points.first() }
    }

    val cumulative = FloatArray(points.size)
    for (index in 1 until points.size) {
        cumulative[index] = cumulative[index - 1] + distance(points[index - 1], points[index])
    }
    val totalLength = cumulative.last()
    if (totalLength < SWIPE_POINT_EPSILON) {
        return List(pointCount) { points.first() }
    }

    val result = ArrayList<SwipePoint>(pointCount)
    var segmentIndex = 1
    repeat(pointCount) { sampleIndex ->
        val targetDistance = totalLength * sampleIndex / (pointCount - 1)
        while (segmentIndex < cumulative.lastIndex && cumulative[segmentIndex] < targetDistance) {
            segmentIndex++
        }
        val fromIndex = (segmentIndex - 1).coerceAtLeast(0)
        val segmentLength = cumulative[segmentIndex] - cumulative[fromIndex]
        val fraction = if (segmentLength < SWIPE_POINT_EPSILON) {
            0f
        } else {
            (targetDistance - cumulative[fromIndex]) / segmentLength
        }
        val from = points[fromIndex]
        val to = points[segmentIndex]
        result.add(
            SwipePoint(
                x = from.x + (to.x - from.x) * fraction,
                y = from.y + (to.y - from.y) * fraction
            )
        )
    }
    return result
}

internal fun buildSwipeWordTemplate(
    foldedWord: String,
    keyCenters: Map<Char, SwipePoint>
): List<SwipePoint>? {
    if (foldedWord.length < 2) return null
    val result = ArrayList<SwipePoint>(foldedWord.length)
    var previousChar: Char? = null
    foldedWord.forEach { char ->
        if (!char.isLetter()) return null
        if (char != previousChar) {
            result.add(keyCenters[char] ?: return null)
            previousChar = char
        }
    }
    return result.takeIf { it.size >= 2 }
}

internal fun rankSwipeGeometryCandidates(
    trace: List<SwipePoint>,
    foldedCandidates: Collection<String>,
    keyCenters: Map<Char, SwipePoint>,
    keySize: Float,
    endpointLimit: Float = SWIPE_GEOMETRY_ENDPOINT_LIMIT
): List<SwipeGeometryMatch> {
    if (trace.size < 2 || keyCenters.isEmpty() || keySize <= 0f) return emptyList()
    val preparedTrace = preparePath(trace) ?: return emptyList()
    val traceStart = preparedTrace.points.first()
    val traceEnd = preparedTrace.points.last()

    return foldedCandidates.mapNotNull { foldedWord ->
        val template = buildSwipeWordTemplate(foldedWord, keyCenters) ?: return@mapNotNull null
        val startDistance = distance(traceStart, template.first()) / keySize
        val endDistance = distance(traceEnd, template.last()) / keySize
        if (startDistance > endpointLimit || endDistance > endpointLimit) {
            return@mapNotNull null
        }
        val preparedTemplate = preparePath(template) ?: return@mapNotNull null
        SwipeGeometryMatch(
            word = foldedWord,
            score = geometryScore(
                trace = preparedTrace,
                template = preparedTemplate,
                keySize = keySize,
                startDistance = startDistance,
                endDistance = endDistance
            ),
            startDistance = startDistance,
            endDistance = endDistance
        )
    }.sortedBy { it.score }
}

private fun preparePath(source: List<SwipePoint>): PreparedSwipePath? {
    if (source.size < 2) return null
    val resampled = resampleSwipePath(source)
    if (resampled.size < 2) return null
    return PreparedSwipePath(
        points = resampled,
        length = pathLength(source)
    )
}

private fun geometryScore(
    trace: PreparedSwipePath,
    template: PreparedSwipePath,
    keySize: Float,
    startDistance: Float,
    endDistance: Float
): Float {
    val alignedDistance = trace.points.indices
        .sumOf { index ->
            distance(trace.points[index], template.points[index]).toDouble()
        }
        .toFloat() / trace.points.size / keySize

    val dtwDistance = bandedDtwDistance(trace.points, template.points, keySize)
    val directionDifference = directionDifference(trace.points, template.points)
    val longerLength = max(trace.length, template.length).coerceAtLeast(keySize)
    val lengthDifference = abs(trace.length - template.length) / longerLength

    return alignedDistance * 0.36f +
        dtwDistance * 0.26f +
        directionDifference * 0.14f +
        startDistance * 0.12f +
        endDistance * 0.18f +
        lengthDifference * 0.08f
}

private fun bandedDtwDistance(
    trace: List<SwipePoint>,
    template: List<SwipePoint>,
    keySize: Float
): Float {
    val width = template.size
    var previous = FloatArray(width + 1) { Float.POSITIVE_INFINITY }
    previous[0] = 0f

    trace.indices.forEach { traceIndex ->
        val current = FloatArray(width + 1) { Float.POSITIVE_INFINITY }
        val row = traceIndex + 1
        val fromColumn = max(1, row - SWIPE_DTW_BAND_RADIUS)
        val toColumn = min(width, row + SWIPE_DTW_BAND_RADIUS)
        for (column in fromColumn..toColumn) {
            val localCost = distance(trace[traceIndex], template[column - 1]) / keySize
            current[column] = localCost + minOf(
                previous[column],
                current[column - 1],
                previous[column - 1]
            )
        }
        previous = current
    }
    return previous[width] / max(trace.size, template.size)
}

private fun directionDifference(
    trace: List<SwipePoint>,
    template: List<SwipePoint>
): Float {
    var total = 0f
    var samples = 0
    for (index in 1 until min(trace.size, template.size)) {
        val traceDx = trace[index].x - trace[index - 1].x
        val traceDy = trace[index].y - trace[index - 1].y
        val templateDx = template[index].x - template[index - 1].x
        val templateDy = template[index].y - template[index - 1].y
        val traceLength = hypot(traceDx.toDouble(), traceDy.toDouble()).toFloat()
        val templateLength = hypot(templateDx.toDouble(), templateDy.toDouble()).toFloat()
        if (traceLength < SWIPE_POINT_EPSILON || templateLength < SWIPE_POINT_EPSILON) {
            continue
        }
        val cosine = ((traceDx * templateDx + traceDy * templateDy) /
            (traceLength * templateLength)).coerceIn(-1f, 1f)
        total += (1f - cosine) / 2f
        samples++
    }
    return if (samples == 0) 1f else total / samples
}

private fun pathLength(points: List<SwipePoint>): Float {
    var total = 0f
    for (index in 1 until points.size) {
        total += distance(points[index - 1], points[index])
    }
    return total
}

private fun distance(first: SwipePoint, second: SwipePoint): Float {
    return hypot(
        (second.x - first.x).toDouble(),
        (second.y - first.y).toDouble()
    ).toFloat()
}
