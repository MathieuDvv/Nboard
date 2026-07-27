package com.nboard.ime

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.icu.text.BreakIterator
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.nboard.ime.ai.GeminiClient
import com.nboard.ime.ai.AnthropicClient
import com.nboard.ime.ai.OpenAiCompatibleClient
import com.nboard.ime.ai.TextGenerationClient
import com.nboard.ime.clipboard.ClipboardHistoryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

class NboardImeService : InputMethodService() {
    internal val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var keyboardUiContext: Context

    internal var clipboardManager: ClipboardManager? = null
    internal var vibrator: Vibrator? = null
    internal lateinit var clipboardHistoryStore: ClipboardHistoryStore
    internal lateinit var textGenerationClient: TextGenerationClient
    private var interTypeface: Typeface? = null

    internal lateinit var keyboardRoot: LinearLayout
    internal lateinit var voiceInputGlow: View
    internal lateinit var swipeTrailView: SwipeTrailView
    internal lateinit var aiQuickActionsRow: LinearLayout
    internal lateinit var aiSummarizeButton: Button
    internal lateinit var aiFixGrammarButton: Button
    internal lateinit var aiExpandButton: Button
    internal lateinit var aiPromptRow: FrameLayout
    internal lateinit var aiPromptToggleButton: ImageButton
    internal lateinit var aiPromptInput: EditText
    internal lateinit var aiPromptShimmer: View

    internal lateinit var clipboardPanel: LinearLayout
    internal lateinit var clipboardItemsContainer: LinearLayout

    internal lateinit var emojiPanel: LinearLayout
    internal lateinit var emojiSearchPill: LinearLayout
    internal lateinit var emojiSearchIconButton: ImageButton
    internal lateinit var emojiSuggestionsScroll: HorizontalScrollView
    internal lateinit var emojiMostUsedRow: LinearLayout
    internal lateinit var emojiSearchInput: EditText
    internal lateinit var emojiGridScroll: HorizontalScrollView
    internal lateinit var emojiRecentColumn: LinearLayout
    internal lateinit var emojiRecentDivider: View
    internal lateinit var emojiGridRow1: LinearLayout
    internal lateinit var emojiGridRow2: LinearLayout
    internal lateinit var emojiGridRow3: LinearLayout

    internal lateinit var recentClipboardRow: LinearLayout
    internal lateinit var recentClipboardChip: AppCompatButton
    internal lateinit var recentClipboardChevronButton: ImageButton
    internal lateinit var predictionRow: LinearLayout
    internal lateinit var predictionWord1Button: AppCompatButton
    internal lateinit var predictionWord2Button: AppCompatButton
    internal lateinit var predictionWord3Button: AppCompatButton
    internal lateinit var predictionSeparator1: TextView
    internal lateinit var predictionSeparator2: TextView

    internal lateinit var keyRowsContainer: ViewGroup
    private lateinit var row0: LinearLayout
    private lateinit var row1: LinearLayout
    private lateinit var row2: LinearLayout
    private lateinit var row3: LinearLayout
    private lateinit var bottomRow: LinearLayout

    internal lateinit var modeSwitchButton: Button
    internal lateinit var leftPunctuationButton: Button
    internal lateinit var aiModeButton: ImageButton
    internal lateinit var spaceButton: Button
    internal lateinit var rightPunctuationButton: Button
    internal lateinit var clipboardButton: ImageButton
    internal lateinit var actionButton: ImageButton

    internal var isAiMode = false
    internal var isNumbersMode = false
    internal var manualShiftMode = ShiftMode.OFF
    internal var isAutoShiftEnabled = true
    internal var lastShiftTapAtMs = 0L
    internal var isClipboardOpen = false
    internal var isGenerating = false
    internal var isSymbolsSubmenuOpen = false
    internal var isEmojiMode = false
    internal var isEmojiSearchMode = false
    internal var inlineInputTarget = InlineInputTarget.NONE

    internal var leftBottomMode = BottomKeyMode.AI
    internal var rightBottomMode = BottomKeyMode.CLIPBOARD
    internal var leftBottomModeOptions = listOf(BottomKeyMode.AI, BottomKeyMode.EMOJI)
    internal var rightBottomModeOptions = listOf(BottomKeyMode.CLIPBOARD, BottomKeyMode.EMOJI)
    internal var activeLayoutPack = LayoutPackManager.defaultPack()
    internal var keyboardLanguageMode = KeyboardLanguageMode.FRENCH
    private var keyboardFontMode = KeyboardFontMode.INTER
    private var appThemeMode = AppThemeMode.SYSTEM
    internal var wordPredictionEnabled = true
    internal var swipeTypingEnabled = true
    internal var swipeTrailEnabled = true
    internal var voiceInputEnabled = true
    private var isNumberRowEnabled = false
    internal var autoSpaceAfterPunctuationEnabled = true
    internal var autoCapitalizeAfterPunctuationEnabled = true
    internal var returnToLettersAfterNumberSpaceEnabled = true
    internal var hapticMode = HapticMode.SYSTEM

    internal val emojiUsageCounts = mutableMapOf<String, Int>()
    internal val emojiRecents = ArrayDeque<String>()
    internal val emojiSearchIndex = mutableMapOf<String, String>()
    internal val allEmojiCatalog = mutableListOf<String>()
    internal var emojiGridLoadedCount = 0
    internal val rejectedCorrections = mutableMapOf<String, Int>()
    internal val learnedWordFrequency = mutableMapOf<String, Int>()
    internal val learnedBigramFrequency = mutableMapOf<String, Int>()
    internal val learnedTrigramFrequency = mutableMapOf<String, Int>()
    internal var learningDirtyUpdates = 0
    @Volatile
    internal var englishLexicon = Lexicon.empty()
    @Volatile
    internal var frenchLexicon = Lexicon.empty()
    internal lateinit var autoCorrectEngine: AutoCorrect
    internal lateinit var bigramPredictor: BigramPredictor

    internal var activePopupWindow: PopupWindow? = null
    internal var activeVariantSession: VariantSelectionSession? = null
    internal var activeSwipePopupSession: SwipePopupSession? = null
    internal var pendingAutoCorrection: AutoCorrectionUndo? = null
    internal var activeVoiceRecognizer: SpeechRecognizer? = null
    internal var aiPillShimmerAnimator: ValueAnimator? = null
    internal var aiTextPulseAnimator: ValueAnimator? = null
    internal var voiceGlowAnimator: ValueAnimator? = null
    internal var voiceActionPulseAnimator: ValueAnimator? = null
    internal var voiceActionPulseForStopping = false
    internal var latestClipboardText: String? = null
    internal var latestClipboardImageUri: Uri? = null
    internal var latestClipboardImageMimeType: String? = null
    internal var latestClipboardImagePreview: Bitmap? = null
    internal var latestClipboardAtMs: Long = 0L
    internal var latestClipboardDismissed = false
    internal var recentClipboardExpiryJob: Job? = null
    internal var hasPredictionSuggestions = false
    internal var predictionRenderCache: PredictionRenderCache? = null
    internal val swipeLetterKeyByView = LinkedHashMap<View, String>()
    internal var activeSwipeTypingSession: SwipeTypingSession? = null
    internal var isVoiceListening = false
    internal var isVoiceStopping = false
    internal var voiceShouldAutoRestart = false
    internal var voiceLastTranscript = ""
    internal var voiceLeadingPrefix = ""
    internal var voiceHasActiveComposition = false
    internal var voiceReleaseStopJob: Job? = null
    internal var voiceFinalizeFallbackJob: Job? = null
    private var activeEditorPackage: String? = null
    internal var smartTypingBehavior = SmartTypingBehavior(0)
    internal var pendingAutoInsertedSentenceSpace = false

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        captureClipboardPrimary()
    }

    override fun onCreate() {
        super.onCreate()
        keyboardUiContext = this
        clipboardHistoryStore = ClipboardHistoryStore(this)
        reloadTypingSettings()
        autoCorrectEngine = AutoCorrect(this)
        autoCorrectEngine.setModeFromKeyboardMode(keyboardLanguageMode)
        bigramPredictor = BigramPredictor(this)
        bigramPredictor.setModeFromKeyboardMode(keyboardLanguageMode)
        serviceScope.launch(Dispatchers.Default) {
            autoCorrectEngine.preload()
            bigramPredictor.preload()
        }

        reloadBottomModesFromSettings()

        loadEmojiUsage()
        loadRejectedCorrections()
        loadPredictionLearning()
        resetLexicons()
        preloadLexiconsFromAssets()
        allEmojiCatalog.clear()
        allEmojiCatalog.addAll(ALL_EMOJIS)
        preloadExtendedEmojiCatalog()

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }

        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)
        captureClipboardPrimary()
    }

    override fun onDestroy() {
        dismissActivePopup()
        stopAiProcessingAnimations()
        stopVoiceInput(forceCancel = true)
        destroyVoiceRecognizer()
        savePredictionLearning(force = true)
        recentClipboardExpiryJob?.cancel()
        clipboardManager?.removePrimaryClipChangedListener(clipboardListener)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        return try {
            reloadTypingSettings()
            reloadBottomModesFromSettings()
            predictionRenderCache = null
            keyboardUiContext = createKeyboardUiContext()
            val root = LayoutInflater.from(keyboardUiContext).inflate(R.layout.keyboard_view, null)
            bindViews(root)
            applyTypographyAndIcons()
            setupEmojiPanel()
            bindListeners()
            renderKeyRows()
            renderClipboardItems()
            renderEmojiGrid()
            renderEmojiSuggestions()
            renderRecentClipboardRow()
            refreshUi()
            root
        } catch (error: Exception) {
            Log.e(TAG, "Failed to create input view", error)
            toast("Nboard failed to load full keyboard, using fallback")
            buildFallbackInputView()
        }
    }

    override fun onStartInput(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInput(editorInfo, restarting)
        manualShiftMode = ShiftMode.OFF
        isAutoShiftEnabled = true
        lastShiftTapAtMs = 0L
        updateSmartTypingBehavior(editorInfo)
        pendingAutoInsertedSentenceSpace = false
        isGenerating = false
        stopAiProcessingAnimations()
        stopVoiceInput(forceCancel = true)
        pendingAutoCorrection = null
        activeSwipeTypingSession = null
        predictionRenderCache = null
        inlineInputTarget = InlineInputTarget.NONE
        val newPackage = editorInfo?.packageName
        if (newPackage != activeEditorPackage) {
            isEmojiSearchMode = false
        }
        activeEditorPackage = newPackage
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        updateSmartTypingBehavior(info)
        reloadTypingSettings()
        reloadBottomModesFromSettings()
        keyboardUiContext = createKeyboardUiContext()
        if (!voiceInputEnabled) {
            stopVoiceInput(forceCancel = true)
        } else {
            ensureVoiceRecognizer()
        }
        if (!swipeTrailEnabled && ::swipeTrailView.isInitialized) {
            swipeTrailView.fadeOutTrail()
        }
        refreshAutoShiftFromContext()
        if (::aiModeButton.isInitialized) {
            applyTypographyAndIcons()
            renderKeyRows()
            renderEmojiGrid()
            renderEmojiSuggestions()
            renderRecentClipboardRow()
            refreshUi()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        dismissActivePopup()
        stopAiProcessingAnimations()
        stopVoiceInput(forceCancel = true)
        setGenerating(false)
        isAiMode = false
        isClipboardOpen = false
        isNumbersMode = false
        isSymbolsSubmenuOpen = false
        isEmojiMode = false
        isEmojiSearchMode = false
        manualShiftMode = ShiftMode.OFF
        isAutoShiftEnabled = true
        lastShiftTapAtMs = 0L
        pendingAutoCorrection = null
        activeSwipeTypingSession = null
        hasPredictionSuggestions = false
        predictionRenderCache = null
        pendingAutoInsertedSentenceSpace = false
        if (::emojiSearchInput.isInitialized) {
            emojiSearchInput.text?.clear()
        }
        if (::aiPromptInput.isInitialized) {
            aiPromptInput.text?.clear()
        }
        clearInlinePromptFocus()
    }

    private fun updateSmartTypingBehavior(editorInfo: EditorInfo?) {
        smartTypingBehavior = SmartTypingBehavior(editorInfo)
    }

    private fun reloadTypingSettings() {
        appThemeMode = KeyboardModeSettings.loadThemeMode(this)
        activeLayoutPack = LayoutPackManager.resolveActive(this)
        keyboardLanguageMode = KeyboardModeSettings.loadLanguageMode(this)
        if (::autoCorrectEngine.isInitialized) {
            autoCorrectEngine.setModeFromKeyboardMode(keyboardLanguageMode)
        }
        if (::bigramPredictor.isInitialized) {
            bigramPredictor.setModeFromKeyboardMode(keyboardLanguageMode)
        }
        keyboardFontMode = KeyboardModeSettings.loadFontMode(this)
        wordPredictionEnabled = KeyboardModeSettings.loadWordPredictionEnabled(this)
        swipeTypingEnabled = KeyboardModeSettings.loadSwipeTypingEnabled(this)
        swipeTrailEnabled = KeyboardModeSettings.loadSwipeTrailEnabled(this)
        voiceInputEnabled = KeyboardModeSettings.loadVoiceInputEnabled(this)
        hapticMode = KeyboardModeSettings.loadHapticMode(this)
        isNumberRowEnabled = KeyboardModeSettings.loadNumberRowEnabled(this)
        autoSpaceAfterPunctuationEnabled = KeyboardModeSettings.loadAutoSpaceAfterPunctuationEnabled(this)
        autoCapitalizeAfterPunctuationEnabled = KeyboardModeSettings.loadAutoCapitalizeAfterPunctuationEnabled(this)
        returnToLettersAfterNumberSpaceEnabled = KeyboardModeSettings.loadReturnToLettersAfterNumberSpaceEnabled(this)
        interTypeface = when (keyboardFontMode) {
            KeyboardFontMode.INTER -> ResourcesCompat.getFont(this, R.font.inter_variable)
            KeyboardFontMode.ROBOTO -> Typeface.create("sans-serif", Typeface.NORMAL)
        }

        textGenerationClient = when (KeyboardModeSettings.loadAiProvider(this)) {
            AiProvider.GEMINI -> {
                val storedKey = KeyboardModeSettings.loadGeminiApiKey(this)
                val apiKey = storedKey.ifBlank { BuildConfig.GEMINI_API_KEY }
                GeminiClient(
                    apiKey = apiKey,
                    model = KeyboardModeSettings.loadGeminiModel(this).modelId
                )
            }
            AiProvider.ANTHROPIC -> AnthropicClient(
                apiKey = KeyboardModeSettings.loadAnthropicApiKey(this),
                model = KeyboardModeSettings.loadAnthropicModel(this).modelId
            )
            AiProvider.OPENAI_COMPATIBLE -> OpenAiCompatibleClient(
                baseUrl = KeyboardModeSettings.loadOpenAiBaseUrl(this),
                model = KeyboardModeSettings.loadOpenAiModel(this),
                apiKey = KeyboardModeSettings.loadOpenAiApiKey(this)
            )
        }
    }

    private fun createKeyboardUiContext(): Context {
        val themeMode = appThemeMode
        val baseContext = when (themeMode) {
            AppThemeMode.SYSTEM -> this
            AppThemeMode.LIGHT -> createUiModeContext(Configuration.UI_MODE_NIGHT_NO)
            AppThemeMode.DARK,
            AppThemeMode.AMOLED -> createUiModeContext(Configuration.UI_MODE_NIGHT_YES)
        }
        val style = if (themeMode == AppThemeMode.AMOLED) {
            R.style.Theme_Nboard_Amoled
        } else {
            R.style.Theme_Nboard
        }
        return ContextThemeWrapper(baseContext, style)
    }

    private fun createUiModeContext(uiNightMode: Int): Context {
        val config = Configuration(resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or uiNightMode
        return createConfigurationContext(config)
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart,
            oldSelEnd,
            newSelStart,
            newSelEnd,
            candidatesStart,
            candidatesEnd
        )
        if (isAiPromptInputActive() || isEmojiSearchInputActive()) {
            clearInlinePromptFocus()
        }
        if (::row1.isInitialized) {
            refreshAutoShiftFromContextAndRerender()
        }
    }

    internal fun reloadBottomModesFromSettings() {
        if (isGboardLayoutActive()) {
            leftBottomModeOptions = BottomKeyMode.entries
            rightBottomModeOptions = emptyList()

            val (left, right) = KeyboardModeSettings.load(this)
            leftBottomMode = if (left in leftBottomModeOptions) left else BottomKeyMode.AI
            rightBottomMode = right
        } else {
            val (leftOptions, rightOptions) = KeyboardModeSettings.loadBottomSlotOptions(this)
            leftBottomModeOptions = leftOptions.distinct().ifEmpty { listOf(BottomKeyMode.AI, BottomKeyMode.EMOJI) }
            rightBottomModeOptions = rightOptions.distinct().ifEmpty { listOf(BottomKeyMode.CLIPBOARD, BottomKeyMode.EMOJI) }

            val (left, right) = KeyboardModeSettings.load(this)
            leftBottomMode = if (left in leftBottomModeOptions) left else leftBottomModeOptions.first()
            rightBottomMode = if (right in rightBottomModeOptions) right else rightBottomModeOptions.first()
            if (left != leftBottomMode || right != rightBottomMode) {
                KeyboardModeSettings.save(this, leftBottomMode, rightBottomMode)
            }
        }

        if (leftBottomMode != BottomKeyMode.AI) {
            isAiMode = false
        }
        val clipboardModeAvailable = if (isGboardLayoutActive()) {
            leftBottomMode == BottomKeyMode.CLIPBOARD
        } else {
            leftBottomMode == BottomKeyMode.CLIPBOARD || rightBottomMode == BottomKeyMode.CLIPBOARD
        }
        if (!clipboardModeAvailable) {
            isClipboardOpen = false
        }
        if (!hasEmojiSlot()) {
            isEmojiMode = false
        }
    }

    private fun bindViews(root: View) {
        keyboardRoot = root.findViewById(R.id.keyboardRoot)
        voiceInputGlow = root.findViewById(R.id.voiceInputGlow)
        swipeTrailView = root.findViewById(R.id.swipeTrailView)

        aiQuickActionsRow = root.findViewById(R.id.aiQuickActionsRow)
        aiSummarizeButton = root.findViewById(R.id.aiSummarizeButton)
        aiFixGrammarButton = root.findViewById(R.id.aiFixGrammarButton)
        aiExpandButton = root.findViewById(R.id.aiExpandButton)
        aiPromptRow = root.findViewById(R.id.aiPromptRow)
        aiPromptToggleButton = root.findViewById(R.id.aiPromptToggleButton)
        aiPromptInput = root.findViewById(R.id.aiPromptInput)
        aiPromptShimmer = root.findViewById(R.id.aiPromptShimmer)

        clipboardPanel = root.findViewById(R.id.clipboardPanel)
        clipboardItemsContainer = root.findViewById(R.id.clipboardItemsContainer)

        emojiPanel = root.findViewById(R.id.emojiPanel)
        emojiSearchPill = root.findViewById(R.id.emojiSearchPill)
        emojiSearchIconButton = root.findViewById(R.id.emojiSearchIconButton)
        emojiSuggestionsScroll = root.findViewById(R.id.emojiSuggestionsScroll)
        emojiMostUsedRow = root.findViewById(R.id.emojiMostUsedRow)
        emojiSearchInput = root.findViewById(R.id.emojiSearchInput)
        emojiGridScroll = root.findViewById(R.id.emojiGridScroll)
        emojiRecentColumn = root.findViewById(R.id.emojiRecentColumn)
        emojiRecentDivider = root.findViewById(R.id.emojiRecentDivider)
        emojiGridRow1 = root.findViewById(R.id.emojiGridRow1)
        emojiGridRow2 = root.findViewById(R.id.emojiGridRow2)
        emojiGridRow3 = root.findViewById(R.id.emojiGridRow3)

        recentClipboardRow = root.findViewById(R.id.recentClipboardRow)
        recentClipboardChip = root.findViewById(R.id.recentClipboardChip)
        recentClipboardChevronButton = root.findViewById(R.id.recentClipboardChevronButton)
        predictionRow = root.findViewById(R.id.predictionRow)
        predictionWord1Button = root.findViewById(R.id.predictionWord1Button)
        predictionWord2Button = root.findViewById(R.id.predictionWord2Button)
        predictionWord3Button = root.findViewById(R.id.predictionWord3Button)
        predictionSeparator1 = root.findViewById(R.id.predictionSeparator1)
        predictionSeparator2 = root.findViewById(R.id.predictionSeparator2)

        keyRowsContainer = root.findViewById(R.id.keyRowsContainer)
        row0 = root.findViewById(R.id.row0)
        row1 = root.findViewById(R.id.row1)
        row2 = root.findViewById(R.id.row2)
        row3 = root.findViewById(R.id.row3)
        bottomRow = root.findViewById(R.id.bottomRow)

        modeSwitchButton = root.findViewById(R.id.modeSwitchButton)
        leftPunctuationButton = root.findViewById(R.id.leftPunctuationButton)
        aiModeButton = root.findViewById(R.id.aiModeButton)
        spaceButton = root.findViewById(R.id.spaceButton)
        rightPunctuationButton = root.findViewById(R.id.rightPunctuationButton)
        clipboardButton = root.findViewById(R.id.clipboardButton)
        actionButton = root.findViewById(R.id.actionButton)

        keyboardRoot.clipChildren = false
        keyRowsContainer.clipChildren = false
        row0.clipChildren = false
        row1.clipChildren = false
        row2.clipChildren = false
        row3.clipChildren = false
        bottomRow.clipChildren = false

        keyRowsContainer.isMotionEventSplittingEnabled = true
        row0.isMotionEventSplittingEnabled = true
        row1.isMotionEventSplittingEnabled = true
        row2.isMotionEventSplittingEnabled = true
        row3.isMotionEventSplittingEnabled = true
        bottomRow.isMotionEventSplittingEnabled = true

        emojiGridLoadedCount = 0
    }

    private fun applyTypographyAndIcons() {
        keyboardRoot.background = uiDrawable(R.drawable.bg_keyboard_container)
        aiPromptRow.background = uiDrawable(R.drawable.bg_ai_pill)
        aiQuickActionsRow.background = null
        aiSummarizeButton.background = uiDrawable(R.drawable.bg_ai_quick_action)
        aiFixGrammarButton.background = uiDrawable(R.drawable.bg_ai_quick_action)
        aiExpandButton.background = uiDrawable(R.drawable.bg_ai_quick_action)
        aiPromptToggleButton.background = uiDrawable(R.drawable.bg_ai_button)
        modeSwitchButton.background = uiDrawable(R.drawable.bg_special_key)
        leftPunctuationButton.background = uiDrawable(R.drawable.bg_key)
        aiModeButton.background = uiDrawable(R.drawable.bg_ai_button)
        spaceButton.background = uiDrawable(R.drawable.bg_space_key)
        rightPunctuationButton.background = uiDrawable(R.drawable.bg_key)
        clipboardButton.background = uiDrawable(R.drawable.bg_special_key)
        actionButton.background = uiDrawable(R.drawable.bg_special_key)
        emojiSearchPill.background = uiDrawable(R.drawable.bg_ai_pill)
        emojiSearchIconButton.background = null
        recentClipboardChip.background = uiDrawable(R.drawable.bg_chip)
        recentClipboardChevronButton.background = null
        predictionWord1Button.background = uiDrawable(R.drawable.bg_prediction_side_chip)
        predictionWord2Button.background = uiDrawable(R.drawable.bg_chip)
        predictionWord3Button.background = uiDrawable(R.drawable.bg_prediction_side_chip)

        applySerifTypeface(modeSwitchButton)
        modeSwitchButton.textSize = 15f
        applyInterTypeface(leftPunctuationButton)
        leftPunctuationButton.textSize = 18f
        applyInterTypeface(spaceButton)
        applyInterTypeface(rightPunctuationButton)
        rightPunctuationButton.textSize = 18f
        applySerifTypeface(aiSummarizeButton)
        applySerifTypeface(aiFixGrammarButton)
        applySerifTypeface(aiExpandButton)
        applyInterTypeface(aiPromptInput)
        applyInterTypeface(emojiSearchInput)
        applyInterTypeface(recentClipboardChip)
        applyInterTypeface(predictionWord1Button)
        applyInterTypeface(predictionWord2Button)
        applyInterTypeface(predictionWord3Button)
        aiPromptInput.filters = arrayOf(InputFilter.LengthFilter(AI_PILL_CHAR_LIMIT))
        aiSummarizeButton.setTextColor(uiColor(R.color.ai_text))
        aiFixGrammarButton.setTextColor(uiColor(R.color.ai_text))
        aiExpandButton.setTextColor(uiColor(R.color.ai_text))
        modeSwitchButton.setTextColor(uiColor(R.color.key_text))
        leftPunctuationButton.setTextColor(uiColor(R.color.key_text))
        spaceButton.setTextColor(uiColor(R.color.space_text))
        rightPunctuationButton.setTextColor(uiColor(R.color.key_text))
        leftPunctuationButton.gravity = Gravity.CENTER
        rightPunctuationButton.gravity = Gravity.CENTER
        leftPunctuationButton.setPadding(0, 0, 0, 0)
        rightPunctuationButton.setPadding(0, 0, 0, 0)
        leftPunctuationButton.minimumHeight = 0
        rightPunctuationButton.minimumHeight = 0
        leftPunctuationButton.minHeight = 0
        rightPunctuationButton.minHeight = 0
        leftPunctuationButton.translationY = dp(1).toFloat()
        rightPunctuationButton.translationY = dp(1).toFloat()
        recentClipboardChip.setTextColor(uiColor(R.color.key_text))
        predictionWord1Button.setTextColor(uiColor(R.color.key_text))
        predictionWord2Button.setTextColor(uiColor(R.color.key_text))
        predictionWord3Button.setTextColor(uiColor(R.color.key_text))
        predictionSeparator1.setTextColor(uiColor(R.color.key_text))
        predictionSeparator2.setTextColor(uiColor(R.color.key_text))
        aiPromptInput.setTextColor(uiColor(R.color.ai_text))
        aiPromptInput.setHintTextColor(uiColor(R.color.ai_hint))
        emojiSearchInput.setTextColor(uiColor(R.color.ai_text))
        emojiSearchInput.setHintTextColor(uiColor(R.color.ai_hint))

        modeSwitchButton.contentDescription = "Switch layout"
        leftPunctuationButton.contentDescription = "Adaptive punctuation key"
        aiModeButton.contentDescription = "Left mode key"
        rightPunctuationButton.contentDescription = "Period key"
        clipboardButton.contentDescription = "Right mode key"
        actionButton.contentDescription = "Action"
        aiPromptToggleButton.contentDescription = "Toggle AI mode"
        emojiSearchIconButton.contentDescription = "Search emoji"
        recentClipboardChip.contentDescription = "Paste recent clipboard"
        recentClipboardChevronButton.contentDescription = "Dismiss recent clipboard"

        setIcon(aiPromptToggleButton, R.drawable.ic_ai_custom, R.color.key_text)
        setIcon(emojiSearchIconButton, R.drawable.ic_smile_lucide, R.color.ai_text)
        setIcon(recentClipboardChevronButton, R.drawable.ic_chevron_down_lucide, R.color.key_text)
        recentClipboardChevronButton.setPadding(0, 0, 0, 0)
        val clipboardIcon = uiDrawable(R.drawable.ic_clipboard_lucide)?.mutate()
        clipboardIcon?.setTint(uiColor(R.color.key_text))
        recentClipboardChip.setCompoundDrawablesRelativeWithIntrinsicBounds(clipboardIcon, null, null, null)
        recentClipboardChip.compoundDrawablePadding = dp(6)
        recentClipboardChip.isSingleLine = true
        recentClipboardChip.ellipsize = TextUtils.TruncateAt.END
        recentClipboardChip.minWidth = 0
        recentClipboardChip.minimumWidth = 0
        updateBottomModeIcons()

        flattenView(aiQuickActionsRow)
        flattenView(aiSummarizeButton)
        flattenView(aiFixGrammarButton)
        flattenView(aiExpandButton)
        flattenView(modeSwitchButton)
        flattenView(leftPunctuationButton)
        flattenView(spaceButton)
        flattenView(rightPunctuationButton)
        flattenView(aiPromptInput)
        flattenView(aiPromptToggleButton)
        flattenView(aiModeButton)
        flattenView(clipboardButton)
        flattenView(actionButton)
        flattenView(emojiSearchIconButton)
        flattenView(recentClipboardChip)
        flattenView(recentClipboardChevronButton)
        flattenView(predictionWord1Button)
        flattenView(predictionWord2Button)
        flattenView(predictionWord3Button)
    }

    private fun setupEmojiPanel() {
        emojiSearchInput.doAfterTextChanged {
            if (isEmojiSearchMode) {
                renderEmojiSuggestions()
            }
        }
        emojiSearchInput.setOnFocusChangeListener { _, hasFocus ->
            when {
                hasFocus && isEmojiSearchActive() -> inlineInputTarget = InlineInputTarget.EMOJI_SEARCH
                !hasFocus && inlineInputTarget == InlineInputTarget.EMOJI_SEARCH -> inlineInputTarget = InlineInputTarget.NONE
            }
        }
        emojiSearchInput.setOnClickListener {
            if (isEmojiSearchActive()) {
                inlineInputTarget = InlineInputTarget.EMOJI_SEARCH
            }
        }
        emojiGridScroll.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            val body = emojiGridScroll.getChildAt(0) ?: return@setOnScrollChangeListener
            if (scrollX + emojiGridScroll.width >= body.width - dp(96)) {
                appendEmojiGridChunk(EMOJI_GRID_CHUNK_SIZE)
            }
        }

        bindPressAction(recentClipboardChip) {
            val text = latestClipboardText.orEmpty()
            val imageUri = latestClipboardImageUri
            val pasted = when {
                text.isNotBlank() -> {
                    currentInputConnection?.commitText(text, 1)
                    true
                }
                imageUri != null -> commitRecentClipboardImage(imageUri, latestClipboardImageMimeType)
                else -> false
            }

            if (pasted) {
                latestClipboardDismissed = true
                recentClipboardExpiryJob?.cancel()
                renderRecentClipboardRow()
                refreshUi()
            }
        }

        bindPressAction(recentClipboardChevronButton) {
            latestClipboardDismissed = true
            recentClipboardExpiryJob?.cancel()
            renderRecentClipboardRow()
            refreshUi()
        }

        renderEmojiGrid()
        renderEmojiSuggestions()
        renderRecentClipboardRow()
    }

    private fun bindListeners() {
        bindPressAction(modeSwitchButton) {
            dismissActivePopup()
            if (isEmojiMode) {
                isEmojiMode = false
                isEmojiSearchMode = false
                isNumbersMode = false
                isSymbolsSubmenuOpen = false
                clearInlinePromptFocus()
                emojiSearchInput.text?.clear()
            } else if (isNumbersMode) {
                isNumbersMode = false
                isSymbolsSubmenuOpen = false
            } else {
                isNumbersMode = true
            }
            renderKeyRows()
            refreshUi()
        }

        bindPressAction(leftPunctuationButton) {
            if (!isGboardLayoutActive()) {
                return@bindPressAction
            }
            commitKeyText(resolveLeadingPunctuationLabel())
        }

        configureKeyTouch(
            view = rightPunctuationButton,
            repeatOnHold = false,
            longPressAction = { anchor, rawX, rawY ->
                if (!isGboardLayoutActive()) {
                    return@configureKeyTouch
                }
                showVariantPopup(
                    anchor = anchor,
                    variants = VARIANT_MAP["."].orEmpty(),
                    shiftAware = false,
                    replacePreviousChar = false,
                    touchRawX = rawX,
                    touchRawY = rawY
                )
            },
            tapOnDown = false
        ) {
            if (!isGboardLayoutActive()) {
                return@configureKeyTouch
            }
            commitKeyText(".")
        }

        configureKeyTouch(
            view = aiModeButton,
            repeatOnHold = false,
            longPressAction = { anchor, x, y ->
                if (!isEmojiMode) {
                    showBottomModePopup(anchor, x, y, true)
                }
            },
            tapOnDown = false
        ) {
            if (isEmojiMode) {
                isEmojiSearchMode = !isEmojiSearchMode
                if (isEmojiSearchMode) {
                    inlineInputTarget = InlineInputTarget.NONE
                } else {
                    clearInlinePromptFocus()
                    emojiSearchInput.text?.clear()
                }
                renderEmojiSuggestions()
                refreshUi()
                return@configureKeyTouch
            }
            performBottomModeTap(leftBottomMode)
        }

        bindPressAction(aiPromptToggleButton) {
            if (!isAiAllowedInCurrentContext()) {
                return@bindPressAction
            }
            toggleAiMode()
        }

        bindPressAction(aiSummarizeButton) {
            if (!isAiAllowedInCurrentContext()) {
                return@bindPressAction
            }
            runQuickAiAction(QuickAiAction.SUMMARIZE)
        }

        bindPressAction(aiFixGrammarButton) {
            if (!isAiAllowedInCurrentContext()) {
                return@bindPressAction
            }
            runQuickAiAction(QuickAiAction.FIX_GRAMMAR)
        }

        bindPressAction(aiExpandButton) {
            if (!isAiAllowedInCurrentContext()) {
                return@bindPressAction
            }
            runQuickAiAction(QuickAiAction.EXPAND)
        }

        configureSpacebarTouch()

        configureKeyTouch(
            view = clipboardButton,
            repeatOnHold = false,
            longPressAction = { anchor, x, y ->
                if (!isEmojiMode && !isGboardLayoutActive()) {
                    showBottomModePopup(anchor, x, y, false)
                }
            },
            tapOnDown = false
        ) {
            if (isEmojiMode) {
                toggleEmojiMode()
                return@configureKeyTouch
            }
            if (isGboardLayoutActive()) {
                commitKeyText(".")
                return@configureKeyTouch
            }
            performBottomModeTap(rightBottomMode)
        }

        configureKeyTouch(
            view = actionButton,
            repeatOnHold = false,
            longPressAction = { _, _, _ ->
                if (isVoiceInputLongPressAvailable()) {
                    startVoiceInput()
                }
            },
            tapOnDown = false,
            onLongPressEnd = {
                stopVoiceInput(forceCancel = false)
            }
        ) {
            if (isVoiceListening || isVoiceStopping) {
                return@configureKeyTouch
            }
            if (isAiPromptInputActive()) {
                submitAiPrompt()
            } else if (isClipboardOpen) {
                deleteOneCharacter()
            } else if (isEmojiMode) {
                deleteOneCharacter()
            } else {
                sendOrEnter()
            }
        }

        bindPressAction(predictionWord1Button) {
            commitWordPrediction(predictionWord1Button.text?.toString().orEmpty())
        }
        bindPressAction(predictionWord2Button) {
            commitWordPrediction(predictionWord2Button.text?.toString().orEmpty())
        }
        bindPressAction(predictionWord3Button) {
            commitWordPrediction(predictionWord3Button.text?.toString().orEmpty())
        }

        aiPromptInput.setOnFocusChangeListener { _, hasFocus ->
            when {
                hasFocus && isAiMode -> inlineInputTarget = InlineInputTarget.AI_PROMPT
                !hasFocus && inlineInputTarget == InlineInputTarget.AI_PROMPT -> inlineInputTarget = InlineInputTarget.NONE
            }
        }
        aiPromptInput.setOnClickListener {
            if (isAiMode) {
                inlineInputTarget = InlineInputTarget.AI_PROMPT
            }
        }
        aiPromptInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitAiPrompt()
                true
            } else {
                false
            }
        }
    }

    internal fun renderKeyRows() {
        isNumberRowEnabled = KeyboardModeSettings.loadNumberRowEnabled(this)
        dismissActivePopup()
        activeSwipeTypingSession = null
        swipeLetterKeyByView.clear()

        row0.removeAllViews()
        row1.removeAllViews()
        row2.removeAllViews()
        row3.removeAllViews()
        row0.isVisible = false

        if (isNumbersMode) {
            if (isSymbolsSubmenuOpen) {
                addTextKeys(row1, listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆"))
                addTextKeys(row2, listOf("£", "¥", "₩", "^", "°", "{", "}", "\\", "%", "©"))
                addSpecialKey(
                    row = row3,
                    label = "=/<",
                    iconRes = null,
                    iconTintRes = R.color.key_text,
                    backgroundRes = R.drawable.bg_special_key,
                    weight = 1.25f,
                    textSizeSp = 16.5f,
                    useSerifTypeface = true,
                    isLast = false
                ) {
                    isSymbolsSubmenuOpen = false
                    renderKeyRows()
                }
                addTextKeys(row3, listOf("<", ">", "[", "]", "_", "+", "!", "?"), includeEndSpacing = true)
            } else {
                addTextKeys(row1, listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"))
                addTextKeys(row2, listOf("@", "#", "€", "-", "&", "_", "+", "(", ")", "/"))
                addSpecialKey(
                    row = row3,
                    label = "=/<",
                    iconRes = null,
                    iconTintRes = R.color.key_text,
                    backgroundRes = R.drawable.bg_special_key,
                    weight = 1.25f,
                    textSizeSp = 16.5f,
                    useSerifTypeface = true,
                    isLast = false
                ) {
                    isSymbolsSubmenuOpen = true
                    renderKeyRows()
                }
                addTextKeys(row3, listOf("*", "\"", "'", ";", ":", ",", ".", "="), includeEndSpacing = true)
            }

            addSpecialKey(
                row = row3,
                label = null,
                iconRes = R.drawable.ic_move_left_lucide,
                iconTintRes = R.color.key_text,
                backgroundRes = R.drawable.bg_special_key,
                weight = 1.2f,
                repeatOnHold = true,
                isLast = true
            ) {
                deleteOneCharacter()
            }
            return
        }

        val alphaRow1 = activeLayoutPack.row1
        val alphaRow2 = activeLayoutPack.row2
        val alphaRow3 = activeLayoutPack.row3.filterNot {
            it == "'" && hasApostropheBottomSlot()
        }
        val alphaRow3Weight = if (isQwertyLayoutActive()) 0.875f else 1f

        val topNumberVariants = if (isNumberRowEnabled) {
            emptyMap()
        } else {
            alphaRow1.mapIndexed { index, label ->
                label to ((index + 1) % 10).toString()
            }.toMap()
        }

        if (isNumberRowEnabled) {
            row0.isVisible = true
            addTextKeys(row0, listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"))
        }

        row2.translationX = 0f
        addTextKeys(row1, alphaRow1, shiftAware = true, topNumberVariants = topNumberVariants)
        if (isQwertyLayoutActive()) {
            val insetPx = resolveQwertySecondRowInsetPx()
            addRowInsetSpacer(row2, insetPx, addTrailingGap = true)
            addTextKeys(row2, alphaRow2, shiftAware = true, includeEndSpacing = true)
            addRowInsetSpacer(row2, insetPx, addTrailingGap = false)
        } else {
            addTextKeys(row2, alphaRow2, shiftAware = true)
        }

        addSpecialKey(
            row = row3,
            label = null,
            iconRes = if (isShiftActive()) R.drawable.ic_arrow_down_lucide else R.drawable.ic_arrow_up_lucide,
            iconTintRes = R.color.key_text,
            backgroundRes = if (manualShiftMode == ShiftMode.CAPS_LOCK) {
                R.drawable.bg_mode_special_selected
            } else {
                R.drawable.bg_special_key
            },
            weight = 1.2f,
            isLast = false
        ) {
            handleShiftTap()
            renderKeyRows()
        }

        addTextKeys(
            row = row3,
            labels = alphaRow3,
            shiftAware = true,
            includeEndSpacing = true,
            keyWeight = alphaRow3Weight
        )

        addSpecialKey(
            row = row3,
            label = null,
            iconRes = R.drawable.ic_move_left_lucide,
            iconTintRes = R.color.key_text,
            backgroundRes = R.drawable.bg_special_key,
            weight = 1.2f,
            repeatOnHold = true,
            isLast = true
        ) {
            deleteOneCharacter()
        }
    }

    private fun addTextKeys(
        row: LinearLayout,
        labels: List<String>,
        shiftAware: Boolean = false,
        includeEndSpacing: Boolean = false,
        topNumberVariants: Map<String, String> = emptyMap(),
        keyWeight: Float = 1f
    ) {
        labels.forEachIndexed { index, original ->
            val isLast = if (includeEndSpacing) false else index == labels.lastIndex
            val displayLabel = if (shiftAware && isShiftActive()) original.uppercase(Locale.US) else original
            val baseToken = original.lowercase(Locale.US)
            val swipeToken = if (
                shiftAware &&
                baseToken.length == 1 &&
                baseToken.first().isLetter()
            ) {
                baseToken
            } else {
                null
            }
            val forcedTopVariant = if (isNumberRowEnabled) {
                null
            } else {
                topNumberVariants[original.lowercase(Locale.US)]
            }
            val longPress = buildVariantLongPressAction(
                original = original,
                shiftAware = shiftAware,
                forcedVariant = forcedTopVariant,
                allowForcedDigitVariant = !isNumberRowEnabled
            )
            val keyTapOnDown = when {
                longPress != null -> false
                swipeTypingEnabled && swipeToken != null -> false
                else -> true
            }

            val keyView = addSpecialKey(
                row = row,
                label = displayLabel,
                iconRes = null,
                iconTintRes = R.color.key_text,
                backgroundRes = R.drawable.bg_key,
                weight = keyWeight,
                longPressAction = longPress,
                swipeToken = swipeToken,
                tapOnDown = keyTapOnDown,
                isLast = isLast
            ) {
                val committed = if (shiftAware && isShiftActive()) {
                    original.uppercase(Locale.US)
                } else {
                    original
                }
                commitKeyText(committed)
            }
            if (swipeToken != null) {
                swipeLetterKeyByView[keyView] = swipeToken
            }
        }
    }

    private fun addRowInsetSpacer(row: LinearLayout, widthPx: Int, addTrailingGap: Boolean) {
        if (widthPx <= 0) {
            return
        }
        val spacer = View(this).apply {
            isClickable = false
            isFocusable = false
            layoutParams = LinearLayout.LayoutParams(widthPx, dp(KEY_HEIGHT_DP)).also { params ->
                if (addTrailingGap) {
                    params.marginEnd = dp(KEY_HORIZONTAL_GAP_DP)
                }
            }
        }
        row.addView(spacer)
    }

    private fun resolveQwertySecondRowInsetPx(): Int {
        val rowWidthPx = when {
            ::row1.isInitialized && row1.width > 0 -> row1.width
            ::keyboardRoot.isInitialized && keyboardRoot.width > 0 -> {
                keyboardRoot.width - keyboardRoot.paddingStart - keyboardRoot.paddingEnd
            }
            else -> resources.displayMetrics.widthPixels - dp(16)
        }.coerceAtLeast(dp(220))

        val horizontalGapPx = dp(KEY_HORIZONTAL_GAP_DP)
        val row1KeyWidth = ((rowWidthPx - (horizontalGapPx * 9)).toFloat() / 10f)
            .coerceAtLeast(dp(22).toFloat())
        return (row1KeyWidth / 2f).toInt()
    }

    private fun buildVariantLongPressAction(
        original: String,
        shiftAware: Boolean,
        forcedVariant: String? = null,
        allowForcedDigitVariant: Boolean = false
    ): ((View, Float, Float) -> Unit)? {
        val key = original.lowercase(Locale.US)
        val layoutVariants = activeLayoutPack.variants[key]
        val variants = linkedSetOf<String>()
        if (allowForcedDigitVariant) {
            forcedVariant?.let { variants.add(it) }
        }
        val sourceVariants = layoutVariants ?: VARIANT_MAP[key].orEmpty()
        sourceVariants.forEach { variants.add(it) }
        if (variants.isEmpty()) {
            return null
        }
        return { anchor, rawX, rawY ->
            showVariantPopup(
                anchor,
                variants.toList(),
                shiftAware,
                replacePreviousChar = false,
                touchRawX = rawX,
                touchRawY = rawY
            )
        }
    }

    private fun showVariantPopup(
        anchor: View,
        variants: List<String>,
        shiftAware: Boolean,
        replacePreviousChar: Boolean,
        touchRawX: Float,
        touchRawY: Float
    ) {
        dismissActivePopup()

        val options = if (shiftAware && isShiftActive()) {
            variants.map { value ->
                if (value.firstOrNull()?.isLetter() == true) value.uppercase(Locale.US) else value
            }
        } else {
            variants
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = uiDrawable(R.drawable.bg_variant_popup)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            clipChildren = false
        }

        val optionViews = mutableListOf<AppCompatTextView>()
        options.forEach { value ->
            val option = AppCompatTextView(this).apply {
                text = value
                gravity = Gravity.CENTER
                textSize = 17f
                applyInterTypeface(this)
                setTextColor(uiColor(R.color.key_text))
                background = uiDrawable(R.drawable.bg_popup_option)
                minWidth = dp(40)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)).also {
                    it.marginEnd = dp(4)
                }
            }
            row.addView(option)
            optionViews.add(option)
        }

        val popup = PopupWindow(
            row,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            isOutsideTouchable = false
            isTouchable = false
            isClippingEnabled = false
            setBackgroundDrawable(ColorDrawable(0x00000000))
            elevation = dp(6).toFloat()
            setOnDismissListener {
                if (activePopupWindow === this) {
                    activePopupWindow = null
                    activeVariantSession = null
                }
            }
        }

        activePopupWindow = popup
        activeVariantSession = VariantSelectionSession(
            options = options,
            optionViews = optionViews,
            replacePreviousChar = replacePreviousChar,
            shiftAware = shiftAware,
            selectedIndex = 0
        )

        showPopupNearTouch(anchor, popup, row, touchRawX, touchRawY)
        highlightVariantSelection(0)
    }

    internal fun updateVariantSelection(rawX: Float, rawY: Float) {
        val session = activeVariantSession ?: return

        var selected = -1
        session.optionViews.forEachIndexed { index, view ->
            val loc = IntArray(2)
            view.getLocationOnScreen(loc)
            val left = loc[0]
            val top = loc[1]
            val right = left + view.width
            val bottom = top + view.height
            if (rawX.toInt() in left..right && rawY.toInt() in top..bottom) {
                selected = index
            }
        }

        if (selected == -1) {
            var nearestDistance = Float.MAX_VALUE
            session.optionViews.forEachIndexed { index, view ->
                val loc = IntArray(2)
                view.getLocationOnScreen(loc)
                val centerX = loc[0] + view.width / 2f
                val distance = kotlin.math.abs(centerX - rawX)
                if (distance < nearestDistance) {
                    nearestDistance = distance
                    selected = index
                }
            }
        }

        if (selected >= 0 && selected != session.selectedIndex) {
            session.selectedIndex = selected
            highlightVariantSelection(selected)
        }
    }

    private fun highlightVariantSelection(selectedIndex: Int) {
        val session = activeVariantSession ?: return
        session.optionViews.forEachIndexed { index, view ->
            val bg = if (index == selectedIndex) {
                R.drawable.bg_popup_option_selected
            } else {
                R.drawable.bg_popup_option
            }
            view.background = uiDrawable(bg)
        }
    }

    internal fun commitSelectedVariant() {
        val session = activeVariantSession ?: return
        val selected = session.options.getOrNull(session.selectedIndex) ?: return

        if (session.replacePreviousChar) {
            deleteOneCharacter()
        }
        commitKeyText(selected)
        dismissActivePopup()
    }

    internal fun updateSwipePopupSelection(rawX: Float, rawY: Float) {
        val session = activeSwipePopupSession ?: return

        fun isEnabled(index: Int): Boolean {
            return session.optionEnabled.getOrNull(index) == true
        }

        var selected = -1
        session.optionViews.forEachIndexed { index, view ->
            if (!isEnabled(index)) {
                return@forEachIndexed
            }
            val loc = IntArray(2)
            view.getLocationOnScreen(loc)
            val left = loc[0]
            val top = loc[1]
            val right = left + view.width
            val bottom = top + view.height
            if (rawX.toInt() in left..right && rawY.toInt() in top..bottom) {
                selected = index
            }
        }

        if (selected == -1) {
            var nearestDistance = Float.MAX_VALUE
            session.optionViews.forEachIndexed { index, view ->
                if (!isEnabled(index)) {
                    return@forEachIndexed
                }
                val loc = IntArray(2)
                view.getLocationOnScreen(loc)
                val centerX = loc[0] + view.width / 2f
                val distance = kotlin.math.abs(centerX - rawX)
                if (distance < nearestDistance) {
                    nearestDistance = distance
                    selected = index
                }
            }
        }

        if (selected >= 0 && selected != session.selectedIndex) {
            session.selectedIndex = selected
            highlightSwipePopupSelection(selected)
        }
    }

    internal fun highlightSwipePopupSelection(selectedIndex: Int) {
        val session = activeSwipePopupSession ?: return
        session.optionViews.forEachIndexed { index, view ->
            val selected = index == selectedIndex && session.optionEnabled.getOrNull(index) == true
            view.background = uiDrawable(
                if (selected) R.drawable.bg_popup_option_selected else R.drawable.bg_popup_option
            )
        }
    }

    internal fun commitSelectedSwipePopup() {
        val session = activeSwipePopupSession ?: return
        val index = session.selectedIndex
        val action = if (session.optionEnabled.getOrNull(index) == true) {
            session.optionActions.getOrNull(index)
        } else {
            null
        }
        dismissActivePopup()
        action?.invoke()
    }

    private fun showBottomModePopup(anchor: View, touchRawX: Float, touchRawY: Float, isLeftSlot: Boolean) {
        dismissActivePopup()

        if (isGboardLayoutActive() && !isLeftSlot) {
            return
        }

        val modeOptions = when {
            isGboardLayoutActive() -> BottomKeyMode.entries
            isLeftSlot -> leftBottomModeOptions
            else -> rightBottomModeOptions
        }
        val options = modeOptions.map { mode ->
            ModeOption(mode, iconResForBottomMode(mode))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = uiDrawable(R.drawable.bg_variant_popup)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val optionViews = mutableListOf<View>()
        val optionActions = mutableListOf<(() -> Unit)?>()
        val optionEnabled = mutableListOf<Boolean>()

        options.forEach { option ->
            val isSelected = if (isGboardLayoutActive() || isLeftSlot) {
                leftBottomMode == option.mode
            } else {
                rightBottomMode == option.mode
            }
            val item = AppCompatImageButton(this).apply {
                background = uiDrawable(
                    if (isSelected) R.drawable.bg_popup_option_selected else R.drawable.bg_popup_option
                )
                setIcon(this, option.iconRes, R.color.key_text)
                flattenView(this)
                contentDescription = option.mode.value
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).also {
                    if (option != options.last()) {
                        it.marginEnd = dp(6)
                    }
                }
            }
            val action: () -> Unit = {
                if (isGboardLayoutActive() || isLeftSlot) {
                    leftBottomMode = option.mode
                    if (leftBottomMode != BottomKeyMode.AI) {
                        isAiMode = false
                    }
                } else {
                    rightBottomMode = option.mode
                }

                val clipboardModeAvailable = if (isGboardLayoutActive()) {
                    leftBottomMode == BottomKeyMode.CLIPBOARD
                } else {
                    leftBottomMode == BottomKeyMode.CLIPBOARD || rightBottomMode == BottomKeyMode.CLIPBOARD
                }
                if (!clipboardModeAvailable) {
                    isClipboardOpen = false
                }

                if (!hasEmojiSlot()) {
                    isEmojiMode = false
                }

                KeyboardModeSettings.save(this, leftBottomMode, rightBottomMode)
                renderKeyRows()
                refreshUi()
            }
            optionViews.add(item)
            optionActions.add(action)
            optionEnabled.add(true)

            row.addView(item)
        }

        val popup = PopupWindow(
            row,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            isOutsideTouchable = false
            isTouchable = false
            isClippingEnabled = false
            setBackgroundDrawable(ColorDrawable(0x00000000))
            elevation = dp(6).toFloat()
            setOnDismissListener {
                if (activePopupWindow === this) {
                    activePopupWindow = null
                    activeSwipePopupSession = null
                }
            }
        }

        activePopupWindow = popup
        showPopupNearTouch(anchor, popup, row, touchRawX, touchRawY)
        activeSwipePopupSession = SwipePopupSession(
            optionViews = optionViews,
            optionActions = optionActions,
            optionEnabled = optionEnabled,
            selectedIndex = 0
        )
        highlightSwipePopupSelection(0)
    }

    internal fun showPopupNearTouch(
        anchor: View,
        popup: PopupWindow,
        content: View,
        touchRawX: Float,
        touchRawY: Float
    ) {
        content.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = content.measuredWidth
        val popupHeight = content.measuredHeight

        val anchorLoc = IntArray(2)
        anchor.getLocationOnScreen(anchorLoc)
        val anchorLeft = anchorLoc[0]
        val anchorTop = anchorLoc[1]

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val minX = dp(4)
        val maxX = (screenWidth - popupWidth - dp(4)).coerceAtLeast(minX)
        val absoluteX = (touchRawX.toInt() - popupWidth / 2).coerceIn(minX, maxX)

        val minY = dp(4)
        val maxY = (screenHeight - popupHeight - dp(4)).coerceAtLeast(minY)
        val preferredAbove = touchRawY.toInt() - popupHeight - dp(10)
        val preferredBelow = touchRawY.toInt() + dp(10)
        val absoluteY = when {
            preferredAbove >= minY -> preferredAbove
            preferredBelow <= maxY -> preferredBelow
            else -> preferredAbove.coerceIn(minY, maxY)
        }

        val xOff = absoluteX - anchorLeft
        val yOff = absoluteY - (anchorTop + anchor.height)
        popup.showAsDropDown(anchor, xOff, yOff, Gravity.START)
    }

    internal fun dismissActivePopup() {
        activePopupWindow?.dismiss()
        activePopupWindow = null
        activeVariantSession = null
        activeSwipePopupSession = null
        activeSwipeTypingSession = null
    }

    private fun hasEmojiSlot(): Boolean {
        if (isGboardLayoutActive()) {
            return true
        }
        return leftBottomMode == BottomKeyMode.EMOJI || rightBottomMode == BottomKeyMode.EMOJI
    }

    internal fun hasApostropheBottomSlot(): Boolean {
        return if (isGboardLayoutActive()) {
            leftBottomMode == BottomKeyMode.APOSTROPHE
        } else {
            BottomKeyMode.APOSTROPHE in leftBottomModeOptions ||
                BottomKeyMode.APOSTROPHE in rightBottomModeOptions
        }
    }

    internal fun isEmojiSearchActive(): Boolean {
        return isEmojiMode && isEmojiSearchMode
    }

    internal fun isAiPromptInputActive(): Boolean {
        return isAiMode && inlineInputTarget == InlineInputTarget.AI_PROMPT
    }

    internal fun isEmojiSearchInputActive(): Boolean {
        return isEmojiSearchActive() && inlineInputTarget == InlineInputTarget.EMOJI_SEARCH
    }

    internal fun clearInlinePromptFocus() {
        inlineInputTarget = InlineInputTarget.NONE
        if (::aiPromptInput.isInitialized) {
            aiPromptInput.clearFocus()
        }
        if (::emojiSearchInput.isInitialized) {
            emojiSearchInput.clearFocus()
        }
        if (::keyboardRoot.isInitialized) {
            keyboardRoot.isFocusableInTouchMode = true
            keyboardRoot.requestFocus()
        }
    }

    internal fun applyInterTypeface(view: TextView) {
        interTypeface?.let { view.typeface = it }
    }

    internal fun applySerifTypeface(view: TextView) {
        view.typeface = Typeface.SERIF
    }

    internal fun uiColor(colorRes: Int): Int {
        val themeAttr = colorAttrFor(colorRes)
        if (themeAttr != null) {
            resolveThemeColor(themeAttr)?.let { return it }
        }
        return ContextCompat.getColor(keyboardUiContext, colorRes)
    }

    internal fun uiDrawable(drawableRes: Int) =
        AppCompatResources.getDrawable(keyboardUiContext, drawableRes)?.mutate()

    private fun colorAttrFor(colorRes: Int): Int? {
        return when (colorRes) {
            R.color.keyboard_bg -> R.attr.colorKeyboardBg
            R.color.key_bg -> R.attr.colorKeyBg
            R.color.key_special_bg -> R.attr.colorKeySpecialBg
            R.color.key_text -> R.attr.colorKeyText
            R.color.space_text -> R.attr.colorSpaceText
            R.color.send_bg -> R.attr.colorSendBg
            R.color.send_text -> R.attr.colorSendText
            R.color.popup_shadow -> R.attr.colorPopupShadow
            else -> null
        }
    }

    private fun resolveThemeColor(attr: Int): Int? {
        val outValue = TypedValue()
        val resolved = keyboardUiContext.theme.resolveAttribute(attr, outValue, true)
        if (!resolved) {
            return null
        }
        return if (outValue.resourceId != 0) {
            ContextCompat.getColor(keyboardUiContext, outValue.resourceId)
        } else {
            outValue.data
        }
    }

    private fun buildFallbackInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(uiColor(R.color.keyboard_bg))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            clipChildren = false
        }

        listOf("A", "B", "C", "Space", "<-").forEachIndexed { index, key ->
            val button = AppCompatButton(this).apply {
                text = key
                setAllCaps(false)
                applyInterTypeface(this)
                background = uiDrawable(R.drawable.bg_key)
                flattenView(this)
            }
            configureKeyTouch(
                view = button,
                repeatOnHold = key == "<-",
                longPressAction = null,
                tapOnDown = true
            ) {
                when (key) {
                    "Space" -> currentInputConnection?.commitText(" ", 1)
                    "<-" -> currentInputConnection?.let { deletePreviousGrapheme(it) }
                    else -> currentInputConnection?.commitText(key.lowercase(Locale.US), 1)
                }
            }
            button.layoutParams = LinearLayout.LayoutParams(0, dp(KEY_HEIGHT_DP), 1f).also {
                if (index < 4) {
                    it.marginEnd = dp(KEY_HORIZONTAL_GAP_DP)
                }
            }
            root.addView(button)
        }
        return root
    }

    internal fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    internal fun isActionButtonInitialized(): Boolean = this::actionButton.isInitialized

    internal fun isVoiceInputGlowInitialized(): Boolean = this::voiceInputGlow.isInitialized

    internal fun isAiPromptInputInitialized(): Boolean = this::aiPromptInput.isInitialized

    internal fun isAiPromptShimmerInitialized(): Boolean = this::aiPromptShimmer.isInitialized

    internal fun isAiModeButtonInitialized(): Boolean = this::aiModeButton.isInitialized

    internal fun isRecentClipboardChipInitialized(): Boolean = this::recentClipboardChip.isInitialized

    internal fun isClipboardItemsContainerInitialized(): Boolean = this::clipboardItemsContainer.isInitialized

    internal fun isKeyboardRootInitialized(): Boolean = this::keyboardRoot.isInitialized

    internal fun isEmojiGridInitialized(): Boolean = this::emojiGridRow1.isInitialized

    internal fun isEmojiMostUsedRowInitialized(): Boolean = this::emojiMostUsedRow.isInitialized

    internal fun isPredictionRowInitialized(): Boolean = this::predictionRow.isInitialized

    internal fun isBigramPredictorInitialized(): Boolean = this::bigramPredictor.isInitialized

    internal fun isSwipeTrailViewInitialized(): Boolean = this::swipeTrailView.isInitialized

    internal fun isKeyRowsContainerInitialized(): Boolean = this::keyRowsContainer.isInitialized

    internal fun arePunctuationButtonsInitialized(): Boolean {
        return this::leftPunctuationButton.isInitialized && this::rightPunctuationButton.isInitialized
    }

    internal fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
