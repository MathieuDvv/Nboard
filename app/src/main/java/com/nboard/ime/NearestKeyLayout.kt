package com.nboard.ime

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout

data class KeyHitRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

internal fun nearestKeyRectIndex(
    x: Float,
    y: Float,
    rects: List<KeyHitRect>,
    maxDistance: Float
): Int? {
    var bestIndex: Int? = null
    var bestDistanceSquared = maxDistance * maxDistance
    rects.forEachIndexed { index, rect ->
        val dx = when {
            x < rect.left -> rect.left - x
            x > rect.right -> x - rect.right
            else -> 0f
        }
        val dy = when {
            y < rect.top -> rect.top - y
            y > rect.bottom -> y - rect.bottom
            else -> 0f
        }
        val distanceSquared = dx * dx + dy * dy
        if (distanceSquared <= bestDistanceSquared) {
            bestDistanceSquared = distanceSquared
            bestIndex = index
        }
    }
    return bestIndex
}

class NearestKeyLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private var delegatedTarget: View? = null
    private val containerScreenLocation = IntArray(2)
    private val targetScreenLocation = IntArray(2)

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            delegatedTarget = null
            if (super.dispatchTouchEvent(event)) {
                return true
            }

            val target = findNearestKey(event.x, event.y) ?: return false
            delegatedTarget = target
            val handled = dispatchToTarget(target, event)
            if (!handled) {
                delegatedTarget = null
            }
            return handled
        }

        val target = delegatedTarget
        if (target == null) {
            return super.dispatchTouchEvent(event)
        }

        val handled = dispatchToTarget(target, event)
        if (event.actionMasked == MotionEvent.ACTION_UP ||
            event.actionMasked == MotionEvent.ACTION_CANCEL
        ) {
            delegatedTarget = null
        }
        return handled
    }

    private fun findNearestKey(x: Float, y: Float): View? {
        val candidates = mutableListOf<View>()
        collectKeyLeaves(this, candidates)
        if (candidates.isEmpty()) return null

        val rects = candidates.map { candidate ->
            val rect = Rect(0, 0, candidate.width, candidate.height)
            offsetDescendantRectToMyCoords(candidate, rect)
            KeyHitRect(rect.left, rect.top, rect.right, rect.bottom)
        }
        val maxDistance = 24f * resources.displayMetrics.density
        val index = nearestKeyRectIndex(x, y, rects, maxDistance) ?: return null
        return candidates[index]
    }

    private fun collectKeyLeaves(parent: ViewGroup, output: MutableList<View>) {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (!child.isShown || !child.isEnabled || child.width <= 0 || child.height <= 0) {
                continue
            }
            when (child) {
                is ViewGroup -> collectKeyLeaves(child, output)
                is Button,
                is ImageButton -> output.add(child)
            }
        }
    }

    private fun dispatchToTarget(target: View, source: MotionEvent): Boolean {
        getLocationOnScreen(containerScreenLocation)
        target.getLocationOnScreen(targetScreenLocation)
        val forwarded = MotionEvent.obtain(source)
        forwarded.offsetLocation(
            (containerScreenLocation[0] - targetScreenLocation[0]).toFloat(),
            (containerScreenLocation[1] - targetScreenLocation[1]).toFloat()
        )
        return try {
            target.dispatchTouchEvent(forwarded)
        } finally {
            forwarded.recycle()
        }
    }
}
