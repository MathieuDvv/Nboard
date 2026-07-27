package com.nboard.ime

import android.view.View
import androidx.appcompat.widget.AppCompatTextView

internal enum class ShiftMode {
    OFF,
    ONE_SHOT,
    CAPS_LOCK
}

internal enum class InlineInputTarget {
    NONE,
    AI_PROMPT,
    EMOJI_SEARCH
}

internal enum class QuickAiAction {
    SUMMARIZE,
    FIX_GRAMMAR,
    EXPAND
}

internal data class ModeOption(val mode: BottomKeyMode, val iconRes: Int)

internal data class Lexicon(
    val words: Set<String>,
    val foldedWords: Set<String>,
    val byFirst: Map<Char, List<String>>,
    val byPrefix2: Map<String, List<String>>,
    val foldedToWord: Map<String, String>
) {
    companion object {
        fun empty() = Lexicon(
            words = emptySet(),
            foldedWords = emptySet(),
            byFirst = emptyMap(),
            byPrefix2 = emptyMap(),
            foldedToWord = emptyMap()
        )
    }
}

internal data class VariantSelectionSession(
    val options: List<String>,
    val optionViews: List<AppCompatTextView>,
    val replacePreviousChar: Boolean,
    val shiftAware: Boolean,
    var selectedIndex: Int
)

internal data class SwipePopupSession(
    val optionViews: List<View>,
    val optionActions: List<(() -> Unit)?>,
    val optionEnabled: List<Boolean>,
    var selectedIndex: Int
)

internal data class SwipeTypingSession(
    val ownerView: View,
    val rawStartX: Float,
    val rawStartY: Float,
    val tokens: MutableList<String>,
    val dwellDurationsMs: MutableList<Long>,
    val trailPoints: MutableList<SwipeTrailView.TrailPoint>,
    val pathPoints: MutableList<SwipePoint>,
    var lastTokenEnteredAtMs: Long,
    var lastRawX: Float,
    var lastRawY: Float,
    var lastSampleAtMs: Long,
    var isSwiping: Boolean
)

data class SwipeSamplePoint(
    val x: Float,
    val y: Float,
    val fraction: Float
)

internal data class AutoCorrectionVariant(
    val word: String,
    val penalty: Int
)

internal data class DictionaryCorrectionCandidate(
    val word: String,
    val score: Int,
    val language: KeyboardLanguageMode?,
    val variantPenalty: Int,
    val editDistance: Int,
    val prefixLength: Int
)

internal data class AutoCorrectionResult(
    val originalWord: String,
    val correctedWord: String
)

internal data class AutoCorrectionUndo(
    val originalWord: String,
    val correctedWord: String,
    val committedSuffix: String
)

internal data class PredictionRenderCache(
    val contextKey: String,
    val words: List<String>
)
