package com.nboard.ime

const val TAG = "NboardImeService"

const val KEY_REPEAT_START_DELAY_MS = 260L
const val KEY_REPEAT_INTERVAL_MS = 45L
const val KEY_PRESS_SCALE = 1.05f
const val KEY_PRESSED_ALPHA = 0.74f
const val MIN_PRESSED_ALPHA = 0.4f
const val HOLD_SELECTION_DEADZONE_DP = 10
const val KEY_PRESS_ANIM_MS = 55L
const val KEY_RELEASE_ANIM_MS = 70L
const val KEY_HEIGHT_DP = 54
const val KEY_HORIZONTAL_GAP_DP = 2
const val VARIANT_LONG_PRESS_TIMEOUT_MS = 240L
const val SHIFT_DOUBLE_TAP_TIMEOUT_MS = 320L
const val AUTO_SHIFT_CONTEXT_WINDOW = 80
const val AUTOCORRECT_CONTEXT_WINDOW = 40
const val AUTOCORRECT_SLOW_LOG_THRESHOLD_MS = 50L
const val PREDICTION_CONTEXT_WINDOW = 280
const val MAX_WORD_PREDICTIONS = 3
const val MAX_PREDICTION_CANDIDATES = 3
const val WORD_PREDICTION_SCAN_LIMIT = 2200
const val MAX_PREDICTION_UNIGRAM_BOOST = 140
const val MAX_PREDICTION_BIGRAM_BOOST = 220
const val MAX_PREDICTION_TRIGRAM_BOOST = 260
const val MAX_PREDICTION_BIGRAM_CANDIDATES = 80
const val MAX_PREDICTION_TRIGRAM_CANDIDATES = 42
const val MAX_PREDICTION_CONTEXT_CHAIN_CANDIDATES = 18
const val MAX_PREDICTION_LEARNED_CANDIDATES = 100
const val PREDICTION_CONTEXT_CHAIN_WINDOW = 6
const val PREDICTION_TOKEN_WINDOW = 24
const val CONTEXT_LANGUAGE_WORD_WINDOW = 6
const val LEARNING_CONTEXT_WINDOW = 240
const val LEARNING_TOKEN_WINDOW = 14
const val LEARNING_SAVE_BATCH_SIZE = 8
const val MAX_LEARNED_WORDS = 2600
const val MAX_LEARNED_BIGRAMS = 5200
const val MAX_LEARNED_TRIGRAMS = 6800
const val LEARNED_TRIM_MARGIN = 180
const val MAX_LEARNING_COUNT = 50_000
const val MAX_EMOJI_SEARCH_SUGGESTIONS = 5
const val RECENT_CLIPBOARD_WINDOW_MS = 45_000L
const val RECENT_CLIPBOARD_PREVIEW_CHAR_LIMIT = 30
const val GRAPHEME_DELETE_CONTEXT_WINDOW = 64
const val EMOJI_GRID_INITIAL_BATCH = 120
const val EMOJI_GRID_CHUNK_SIZE = 90
const val SPACEBAR_CURSOR_STEP_DP = 18
const val SPACEBAR_CURSOR_DEADZONE_DP = 26
const val SWIPE_TYPING_DEADZONE_DP = 18
const val SWIPE_DWELL_COMMIT_MS = 18L
const val SWIPE_LEXICON_SCAN_LIMIT = 3400
const val SWIPE_LEARNED_SCAN_LIMIT = 420
const val SWIPE_DISTANCE_BASE_LIMIT = 5
const val SWIPE_CONFIDENT_SCORE = 38
const val SWIPE_MIN_SCORE_MARGIN = 7
const val SWIPE_TRAIL_MIN_STEP_DP = 2
const val SWIPE_TRAIL_MAX_POINTS = 140
const val SWIPE_KEY_HIT_SLOP_DP = 8
const val SWIPE_INTERPOLATION_STEP_DP = 5
const val SWIPE_PATH_MIN_STEP_DP = 2
const val SWIPE_PATH_MAX_POINTS = 192
const val SWIPE_GEOMETRY_ENDPOINT_LIMIT = 2.35f
const val SWIPE_GEOMETRY_CONFIDENT_SCORE = 1.15f
const val SWIPE_GEOMETRY_FALLBACK_SCORE = 1.65f
const val SWIPE_GEOMETRY_MIN_MARGIN = 0.055f
const val AUTOCORRECT_REVERT_DISABLE_THRESHOLD = 2
const val AUTOCORRECT_LEARNED_WORD_SKIP_THRESHOLD = 3
const val AUTOCORRECT_REVERT_LEARN_BOOST = 4
const val MAX_AUTOCORRECT_VARIANTS = 14

const val MAX_CLIPBOARD_GRID_ITEMS = 4
const val MAX_RECENT_EMOJIS = 30
const val AI_PILL_CHAR_LIMIT = 320
const val AI_REPLY_CHAR_LIMIT = 420
const val VOICE_RESTART_DELAY_MS = 80L
const val VOICE_RELEASE_GRACE_MS = 220L
const val VOICE_FINALIZE_FALLBACK_MS = 1400L

const val KEY_EMOJI_COUNTS_JSON = "emoji_usage_counts"
const val KEY_EMOJI_RECENTS_JSON = "emoji_recents"
const val KEY_AUTOCORRECT_REJECTED_JSON = "autocorrect_rejected"
const val KEY_LEARNED_WORD_COUNTS_JSON = "learned_word_counts"
const val KEY_LEARNED_BIGRAM_COUNTS_JSON = "learned_bigram_counts"
const val KEY_LEARNED_TRIGRAM_COUNTS_JSON = "learned_trigram_counts"

const val AI_PROMPT_SYSTEM_INSTRUCTION =
    "You are a concise writing assistant. Reply only with the final text. Keep responses short and practical. " +
        "When transforming user text, preserve the original language and do not translate unless the user explicitly asks."

const val AI_QUICK_ACTION_SYSTEM_INSTRUCTION =
    "You are a concise text-rewrite assistant. Return only the rewritten output without explanation. Keep it brief. " +
        "Preserve the original language of the provided text and do not translate unless explicitly requested."
