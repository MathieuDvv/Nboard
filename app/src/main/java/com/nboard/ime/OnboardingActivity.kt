package com.nboard.ime

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {
    private lateinit var pager: ViewPager2
    private lateinit var skipButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var primaryButton: ImageButton
    private lateinit var topArea: View
    private lateinit var welcomeTopBar: View
    private lateinit var titleTopBar: View
    private lateinit var topTitle: TextView
    private lateinit var indicatorRow: View
    private lateinit var dots: List<View>
    private lateinit var navContainer: View
    private lateinit var bottomSpacer: View
    private lateinit var sheetHost: View

    private lateinit var setupSection: View
    private lateinit var aiSection: View
    private lateinit var betaSection: View
    private lateinit var transitionSection: View

    private lateinit var layoutValue: TextView
    private lateinit var aiEnabledValue: TextView
    private lateinit var wordPredictionValue: TextView
    private lateinit var swipeTypingValue: TextView
    private lateinit var voiceRecognitionValue: TextView
    private lateinit var apiKeyInput: EditText
    private lateinit var focusInput: EditText

    private var currentPage = 0
    private var selectedLayoutMode = KeyboardLayoutMode.AZERTY
    private var aiEnabled = false
    private var predictionEnabled = true
    private var swipeEnabled = true
    private var voiceRecognitionEnabled = true
    private var visibleSection: View? = null
    private var isExitAnimating = false
    private var didPersistOnboarding = false

    private val autoFinishHandler = Handler(Looper.getMainLooper())
    private val autoFinishRunnable = Runnable { finishOnboarding() }

    private val pageLayouts = listOf(
        R.layout.item_onboarding_page_welcome,
        R.layout.item_onboarding_page_ai,
        R.layout.item_onboarding_page_beta,
        R.layout.item_onboarding_page_transition
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        val themeMode = KeyboardModeSettings.loadThemeMode(this)
        setTheme(themeStyleFor(themeMode))
        applyThemePreference(themeMode)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        pager = findViewById(R.id.onboardingPager)
        skipButton = findViewById(R.id.onboardingSkipButton)
        backButton = findViewById(R.id.onboardingBackButton)
        primaryButton = findViewById(R.id.onboardingPrimaryButton)
        topArea = findViewById(R.id.onboardingTopArea)
        welcomeTopBar = findViewById(R.id.onboardingWelcomeTopBar)
        titleTopBar = findViewById(R.id.onboardingTitleTopBar)
        topTitle = findViewById(R.id.onboardingTopTitle)
        indicatorRow = findViewById(R.id.onboardingIndicatorRow)
        navContainer = findViewById(R.id.onboardingNavContainer)
        bottomSpacer = findViewById(R.id.onboardingBottomSpacer)
        sheetHost = findViewById(R.id.onboardingSheetHost)
        dots = listOf(
            findViewById(R.id.onboardingDot1),
            findViewById(R.id.onboardingDot2),
            findViewById(R.id.onboardingDot3)
        )

        setupSection = findViewById(R.id.onboardingSetupSection)
        aiSection = findViewById(R.id.onboardingAiSection)
        betaSection = findViewById(R.id.onboardingBetaSection)
        transitionSection = findViewById(R.id.onboardingTransitionSection)

        layoutValue = findViewById(R.id.onboardingLayoutValue)
        aiEnabledValue = findViewById(R.id.onboardingAiEnabledValue)
        wordPredictionValue = findViewById(R.id.onboardingWordPredictionValue)
        swipeTypingValue = findViewById(R.id.onboardingSwipeTypingValue)
        voiceRecognitionValue = findViewById(R.id.onboardingVoiceRecognitionValue)
        apiKeyInput = findViewById(R.id.onboardingApiKeyInput)
        focusInput = findViewById(R.id.onboardingFocusInput)

        applySystemInsets()
        loadInitialValues()
        bindActions()

        pager.adapter = StaticLayoutPagerAdapter(pageLayouts)
        pager.offscreenPageLimit = 1
        pager.setPageTransformer { page, position ->
            val abs = kotlin.math.abs(position)
            page.alpha = (1f - (abs * 0.34f)).coerceIn(0.64f, 1f)
            page.translationX = 0f
        }
        (pager.getChildAt(0) as? RecyclerView)?.overScrollMode = View.OVER_SCROLL_NEVER
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                updateUiForPage(position)
            }
        })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentPage > 0) {
                    pager.currentItem = currentPage - 1
                } else {
                    finishOnboarding()
                }
            }
        })

        updateUiForPage(0)
    }

    private fun loadInitialValues() {
        selectedLayoutMode = KeyboardModeSettings.loadLayoutMode(this)
        predictionEnabled = KeyboardModeSettings.loadWordPredictionEnabled(this)
        swipeEnabled = KeyboardModeSettings.loadSwipeTypingEnabled(this)
        voiceRecognitionEnabled = KeyboardModeSettings.loadVoiceInputEnabled(this)
        val existingApiKey = KeyboardModeSettings.loadGeminiApiKey(this)
        val (leftMode, rightMode) = KeyboardModeSettings.load(this)
        aiEnabled = existingApiKey.isNotBlank() || leftMode == BottomKeyMode.AI || rightMode == BottomKeyMode.AI

        layoutValue.text = formatLayout(selectedLayoutMode)
        aiEnabledValue.text = if (aiEnabled) "Enabled" else "Disabled"
        wordPredictionValue.text = if (predictionEnabled) "Enabled" else "Disabled"
        swipeTypingValue.text = if (swipeEnabled) "Enabled" else "Disabled"
        voiceRecognitionValue.text = if (voiceRecognitionEnabled) "Enabled" else "Disabled"
        apiKeyInput.setText(existingApiKey)
        apiKeyInput.setSelection(apiKeyInput.text?.length ?: 0)
    }

    private fun bindActions() {
        skipButton.setOnClickListener { finishOnboarding() }

        backButton.setOnClickListener {
            if (currentPage > 0) {
                pager.currentItem = currentPage - 1
            }
        }

        primaryButton.setOnClickListener {
            if (currentPage < TRANSITION_PAGE_INDEX) {
                pager.currentItem = (currentPage + 1).coerceAtMost(TRANSITION_PAGE_INDEX)
            } else {
                finishOnboarding()
            }
        }

        findViewById<View>(R.id.onboardingEnableRow).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        findViewById<View>(R.id.onboardingChooseRow).setOnClickListener {
            showKeyboardPicker()
        }

        findViewById<View>(R.id.onboardingLayoutRow).setOnClickListener {
            showLayoutDialog()
        }

        findViewById<View>(R.id.onboardingAiEnabledRow).setOnClickListener {
            showAiEnabledDialog()
        }

        findViewById<View>(R.id.onboardingWordPredictionRow).setOnClickListener {
            showEnabledDialog(
                title = "Word prediction",
                currentEnabled = predictionEnabled
            ) { enabled ->
                predictionEnabled = enabled
                wordPredictionValue.text = if (enabled) "Enabled" else "Disabled"
            }
        }

        findViewById<View>(R.id.onboardingSwipeTypingRow).setOnClickListener {
            showEnabledDialog(
                title = "Swipe typing",
                currentEnabled = swipeEnabled
            ) { enabled ->
                swipeEnabled = enabled
                swipeTypingValue.text = if (enabled) "Enabled" else "Disabled"
            }
        }

        findViewById<View>(R.id.onboardingVoiceRecognitionRow).setOnClickListener {
            showEnabledDialog(
                title = "Voice recognition",
                currentEnabled = voiceRecognitionEnabled
            ) { enabled ->
                voiceRecognitionEnabled = enabled
                voiceRecognitionValue.text = if (enabled) "Enabled" else "Disabled"
            }
        }
    }

    private fun updateUiForPage(index: Int) {
        autoFinishHandler.removeCallbacks(autoFinishRunnable)

        when (index) {
            WELCOME_PAGE_INDEX -> {
                updateSheetHeight(DEFAULT_SHEET_HEIGHT_DP)
                welcomeTopBar.visibility = View.VISIBLE
                titleTopBar.visibility = View.GONE
                indicatorRow.visibility = View.VISIBLE
                setActiveIndicator(0)
                showSection(setupSection, animate = false)
                navContainer.visibility = View.VISIBLE
                bottomSpacer.visibility = View.VISIBLE
                pager.isUserInputEnabled = true
            }

            AI_PAGE_INDEX -> {
                updateSheetHeight(DEFAULT_SHEET_HEIGHT_DP)
                welcomeTopBar.visibility = View.GONE
                titleTopBar.visibility = View.VISIBLE
                topTitle.text = "AI tools"
                indicatorRow.visibility = View.VISIBLE
                setActiveIndicator(1)
                showSection(aiSection)
                navContainer.visibility = View.VISIBLE
                bottomSpacer.visibility = View.VISIBLE
                pager.isUserInputEnabled = true
            }

            BETA_PAGE_INDEX -> {
                updateSheetHeight(DEFAULT_SHEET_HEIGHT_DP)
                welcomeTopBar.visibility = View.GONE
                titleTopBar.visibility = View.VISIBLE
                topTitle.text = "Beta feature"
                indicatorRow.visibility = View.VISIBLE
                setActiveIndicator(2)
                showSection(betaSection)
                navContainer.visibility = View.VISIBLE
                bottomSpacer.visibility = View.VISIBLE
                pager.isUserInputEnabled = true
            }

            else -> {
                updateSheetHeight(TRANSITION_SHEET_HEIGHT_DP)
                welcomeTopBar.visibility = View.GONE
                titleTopBar.visibility = View.GONE
                indicatorRow.visibility = View.GONE
                showSection(transitionSection)
                navContainer.visibility = View.GONE
                bottomSpacer.visibility = View.GONE
                pager.isUserInputEnabled = false
                autoFinishHandler.postDelayed(autoFinishRunnable, TRANSITION_AUTO_SKIP_MS)
            }
        }
    }

    private fun updateSheetHeight(heightDp: Int) {
        val params = sheetHost.layoutParams
        val target = dp(heightDp)
        if (params.height == target) return
        params.height = target
        sheetHost.layoutParams = params
    }

    private fun showSection(section: View, animate: Boolean = true) {
        if (visibleSection === section) return
        val previous = visibleSection
        visibleSection = section

        if (!animate || previous == null) {
            setupSection.visibility = if (section === setupSection) View.VISIBLE else View.GONE
            aiSection.visibility = if (section === aiSection) View.VISIBLE else View.GONE
            betaSection.visibility = if (section === betaSection) View.VISIBLE else View.GONE
            transitionSection.visibility = if (section === transitionSection) View.VISIBLE else View.GONE
            section.alpha = 1f
            return
        }

        previous.animate().cancel()
        section.animate().cancel()

        previous.animate()
            .alpha(0f)
            .setDuration(100L)
            .withEndAction {
                previous.visibility = View.GONE
                section.alpha = 0f
                section.visibility = View.VISIBLE
                section.animate().alpha(1f).setDuration(140L).start()
            }
            .start()
    }

    private fun setActiveIndicator(activeIndex: Int) {
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index == activeIndex) R.drawable.bg_onboarding_dot_active
                else R.drawable.bg_onboarding_dot_inactive
            )
            val lp = dot.layoutParams
            lp.width = if (index == activeIndex) dp(42) else dp(20)
            dot.layoutParams = lp
        }
    }

    private fun showLayoutDialog() {
        val options = arrayOf("AZERTY", "QWERTY", "Gboard AZERTY", "Gboard QWERTY")
        val selected = when (selectedLayoutMode) {
            KeyboardLayoutMode.AZERTY -> 0
            KeyboardLayoutMode.QWERTY -> 1
            KeyboardLayoutMode.GBOARD_AZERTY -> 2
            KeyboardLayoutMode.GBOARD_QWERTY -> 3
        }

        AlertDialog.Builder(this)
            .setTitle("Layout")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                selectedLayoutMode = when (which) {
                    1 -> KeyboardLayoutMode.QWERTY
                    2 -> KeyboardLayoutMode.GBOARD_AZERTY
                    3 -> KeyboardLayoutMode.GBOARD_QWERTY
                    else -> KeyboardLayoutMode.AZERTY
                }
                layoutValue.text = formatLayout(selectedLayoutMode)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAiEnabledDialog() {
        val options = arrayOf("Enabled", "Disabled")
        val selected = if (aiEnabled) 0 else 1

        AlertDialog.Builder(this)
            .setTitle("AI integration (Gemini default)")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                aiEnabled = which == 0
                aiEnabledValue.text = if (aiEnabled) "Enabled" else "Disabled"
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEnabledDialog(title: String, currentEnabled: Boolean, onSelected: (Boolean) -> Unit) {
        val options = arrayOf("Enabled", "Disabled")
        val selected = if (currentEnabled) 0 else 1
        AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(options, selected) { dialog, which ->
                onSelected(which == 0)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showKeyboardPicker() {
        val imm = getSystemService(InputMethodManager::class.java)
        if (imm == null) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            return
        }

        focusInput.visibility = View.VISIBLE
        focusInput.requestFocus()
        imm.showSoftInput(focusInput, InputMethodManager.SHOW_IMPLICIT)
        focusInput.postDelayed({
            runCatching {
                imm.showInputMethodPicker()
            }.onFailure {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
            focusInput.visibility = View.INVISIBLE
            focusInput.clearFocus()
        }, 120L)
    }

    private fun applySystemInsets() {
        val topArea = findViewById<View>(R.id.onboardingTopArea)
        val sheetContent = findViewById<View>(R.id.onboardingSheetContent)
        val topBase = topArea.paddingTop
        val sheetBottomBase = sheetContent.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(topArea) { view, insets ->
            val system = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
            )
            view.setPadding(
                view.paddingLeft,
                topBase + system.top,
                view.paddingRight,
                view.paddingBottom
            )
            sheetContent.setPadding(
                sheetContent.paddingLeft,
                sheetContent.paddingTop,
                sheetContent.paddingRight,
                sheetBottomBase + system.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(topArea)
    }

    private fun finishOnboarding() {
        autoFinishHandler.removeCallbacks(autoFinishRunnable)
        persistOnboardingSelections()

        if (currentPage == TRANSITION_PAGE_INDEX) {
            if (isExitAnimating) return
            isExitAnimating = true
            val liftDistance = -resources.displayMetrics.heightPixels.toFloat()
            topArea.animate().cancel()
            sheetHost.animate().cancel()
            topArea.animate()
                .alpha(0f)
                .setDuration(360L)
                .start()
            sheetHost.animate()
                .translationY(liftDistance)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setDuration(420L)
                .withEndAction {
                    finish()
                    overridePendingTransition(0, 0)
                }
                .start()
            return
        }
        finish()
    }

    private fun persistOnboardingSelections() {
        if (didPersistOnboarding) return
        didPersistOnboarding = true
        val apiKey = apiKeyInput.text?.toString().orEmpty()
        KeyboardModeSettings.saveLayoutMode(this, selectedLayoutMode)
        KeyboardModeSettings.saveWordPredictionEnabled(this, predictionEnabled)
        KeyboardModeSettings.saveSwipeTypingEnabled(this, swipeEnabled)
        KeyboardModeSettings.saveVoiceInputEnabled(this, voiceRecognitionEnabled)

        if (aiEnabled) {
            if (apiKey.isNotBlank()) {
                KeyboardModeSettings.saveGeminiApiKey(this, apiKey)
            }
        } else {
            KeyboardModeSettings.saveLeftSlotOptions(this, BottomKeyMode.CLIPBOARD, BottomKeyMode.EMOJI)
            KeyboardModeSettings.saveRightSlotOptions(this, BottomKeyMode.EMOJI, BottomKeyMode.CLIPBOARD)
            KeyboardModeSettings.save(this, BottomKeyMode.CLIPBOARD, BottomKeyMode.EMOJI)
        }

        KeyboardModeSettings.saveOnboardingCompleted(this, true)
    }

    private fun formatLayout(mode: KeyboardLayoutMode): String {
        return when (mode) {
            KeyboardLayoutMode.AZERTY -> "AZERTY"
            KeyboardLayoutMode.QWERTY -> "QWERTY"
            KeyboardLayoutMode.GBOARD_AZERTY -> "Gboard AZERTY"
            KeyboardLayoutMode.GBOARD_QWERTY -> "Gboard QWERTY"
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun applyThemePreference(mode: AppThemeMode) {
        val nightMode = when (mode) {
            AppThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppThemeMode.DARK,
            AppThemeMode.AMOLED -> AppCompatDelegate.MODE_NIGHT_YES
        }
        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }

    private fun themeStyleFor(mode: AppThemeMode): Int {
        return if (mode == AppThemeMode.AMOLED) {
            R.style.Theme_Nboard_Amoled
        } else {
            R.style.Theme_Nboard
        }
    }

    override fun onDestroy() {
        autoFinishHandler.removeCallbacks(autoFinishRunnable)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_REPLAY = "extra_replay"
        private const val WELCOME_PAGE_INDEX = 0
        private const val AI_PAGE_INDEX = 1
        private const val BETA_PAGE_INDEX = 2
        private const val TRANSITION_PAGE_INDEX = 3
        private const val TRANSITION_AUTO_SKIP_MS = 2600L
        private const val DEFAULT_SHEET_HEIGHT_DP = 420
        private const val TRANSITION_SHEET_HEIGHT_DP = 330
    }
}

private class StaticLayoutPagerAdapter(
    private val layouts: List<Int>
) : RecyclerView.Adapter<StaticLayoutPagerAdapter.LayoutViewHolder>() {

    override fun getItemViewType(position: Int): Int = layouts[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayoutViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return LayoutViewHolder(view)
    }

    override fun onBindViewHolder(holder: LayoutViewHolder, position: Int) = Unit

    override fun getItemCount(): Int = layouts.size

    class LayoutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
