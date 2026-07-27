package com.nboard.ime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var languageValue: TextView
    private lateinit var languageProfilesValue: TextView
    private lateinit var keyboardValue: TextView
    private lateinit var uploadedLayoutNoticeCard: View
    private lateinit var uploadedLayoutNoticeText: TextView
    private lateinit var numberRowSettingValue: TextView
    private lateinit var autoSpaceAfterPunctuationValue: TextView
    private lateinit var autoCapitalizeAfterPunctuationValue: TextView
    private lateinit var returnToLettersAfterNumberSpaceValue: TextView
    private lateinit var wordPredictionValue: TextView
    private lateinit var swipeTypingValue: TextView
    private lateinit var swipeTrailValue: TextView
    private lateinit var voiceInputValue: TextView
    private lateinit var hapticModeValue: TextView
    private lateinit var aiProviderValue: TextView
    private lateinit var aiModelLabel: TextView
    private lateinit var geminiModelValue: TextView
    private lateinit var geminiModelRow: View
    private lateinit var aiModelDivider: View
    private lateinit var swipeTrailRow: View
    private lateinit var swipeTrailDivider: View
    private lateinit var leftKeyModesRow: View
    private lateinit var rightKeyModesRow: View
    private lateinit var leftKeyModesValue: TextView
    private lateinit var rightKeyModesValue: TextView
    private lateinit var deleteLayoutPackRow: View
    private lateinit var deleteLayoutPackValue: TextView
    private lateinit var themeValue: TextView
    private lateinit var fontValue: TextView
    private val importLayoutPackLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importLayoutPackFromUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val themeMode = KeyboardModeSettings.loadThemeMode(this)
        setTheme(themeStyleFor(themeMode))
        applyThemePreference(themeMode)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        languageValue = findViewById(R.id.languageValue)
        languageProfilesValue = findViewById(R.id.languageProfilesValue)
        keyboardValue = findViewById(R.id.keyboardValue)
        uploadedLayoutNoticeCard = findViewById(R.id.uploadedLayoutNoticeCard)
        uploadedLayoutNoticeText = findViewById(R.id.uploadedLayoutNoticeText)
        numberRowSettingValue = findViewById(R.id.numberRowSettingValue)
        autoSpaceAfterPunctuationValue = findViewById(R.id.autoSpaceAfterPunctuationValue)
        autoCapitalizeAfterPunctuationValue = findViewById(R.id.autoCapitalizeAfterPunctuationValue)
        returnToLettersAfterNumberSpaceValue = findViewById(R.id.returnToLettersAfterNumberSpaceValue)
        wordPredictionValue = findViewById(R.id.wordPredictionValue)
        swipeTypingValue = findViewById(R.id.swipeTypingValue)
        swipeTrailValue = findViewById(R.id.swipeTrailValue)
        voiceInputValue = findViewById(R.id.voiceInputValue)
        hapticModeValue = findViewById(R.id.hapticModeValue)
        aiProviderValue = findViewById(R.id.aiProviderValue)
        aiModelLabel = findViewById(R.id.aiModelLabel)
        geminiModelValue = findViewById(R.id.geminiModelValue)
        geminiModelRow = findViewById(R.id.geminiModelRow)
        aiModelDivider = findViewById(R.id.aiModelDivider)
        swipeTrailRow = findViewById(R.id.swipeTrailRow)
        swipeTrailDivider = findViewById(R.id.swipeTrailDivider)
        leftKeyModesRow = findViewById(R.id.leftKeyModesRow)
        rightKeyModesRow = findViewById(R.id.rightKeyModesRow)
        leftKeyModesValue = findViewById(R.id.leftKeyModesValue)
        rightKeyModesValue = findViewById(R.id.rightKeyModesValue)
        deleteLayoutPackRow = findViewById(R.id.deleteLayoutPackRow)
        deleteLayoutPackValue = findViewById(R.id.deleteLayoutPackValue)
        themeValue = findViewById(R.id.themeValue)
        fontValue = findViewById(R.id.fontValue)

        applyStatusBarInset()
        bindActions()
        refreshValues()
        maybeShowFirstLaunchOnboarding()
    }

    override fun onResume() {
        super.onResume()
        refreshValues()
    }

    private fun applyStatusBarInset() {
        val content = findViewById<View>(R.id.settingsContent)
        val baseTop = content.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, baseTop + topInset, view.paddingRight, view.paddingBottom)
            insets
        }
        ViewCompat.requestApplyInsets(content)
    }

    private fun bindActions() {
        findViewById<View>(R.id.makeDefaultRow).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        findViewById<View>(R.id.replayOnboardingRow).setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java).apply {
                putExtra(OnboardingActivity.EXTRA_REPLAY, true)
            })
        }

        findViewById<View>(R.id.keyboardRow).setOnClickListener {
            showKeyboardLayoutDialog()
        }

        findViewById<View>(R.id.moreLayoutsRow).setOnClickListener {
            openLink("https://github.com/MathieuDvv/Nboard/tree/main/Community%20Layouts")
        }

        findViewById<View>(R.id.importLayoutPackRow).setOnClickListener {
            launchLayoutPackImportPicker()
        }

        deleteLayoutPackRow.setOnClickListener {
            showDeleteLayoutPackDialog()
        }

        findViewById<View>(R.id.numberRowSettingRow).setOnClickListener {
            showNumberRowDialog()
        }

        findViewById<View>(R.id.autoSpaceAfterPunctuationRow).setOnClickListener {
            showAutoSpaceAfterPunctuationDialog()
        }

        findViewById<View>(R.id.autoCapitalizeAfterPunctuationRow).setOnClickListener {
            showAutoCapitalizeAfterPunctuationDialog()
        }

        findViewById<View>(R.id.returnToLettersAfterNumberSpaceRow).setOnClickListener {
            showReturnToLettersAfterNumberSpaceDialog()
        }

        findViewById<View>(R.id.languageRow).setOnClickListener {
            showLanguageDialog()
        }

        findViewById<View>(R.id.languageProfilesRow).setOnClickListener {
            startActivity(Intent(this, LanguageProfilesActivity::class.java))
        }

        findViewById<View>(R.id.hapticModeRow).setOnClickListener {
            showHapticModeDialog()
        }

        findViewById<View>(R.id.aiProviderRow).setOnClickListener {
            showAiProviderDialog()
        }

        geminiModelRow.setOnClickListener {
            showProviderModelDialog()
        }

        findViewById<View>(R.id.apiKeyRow).setOnClickListener {
            showProviderConfigurationDialog()
        }

        findViewById<View>(R.id.wordPredictionRow).setOnClickListener {
            showWordPredictionDialog()
        }

        findViewById<View>(R.id.swipeTypingRow).setOnClickListener {
            showSwipeTypingDialog()
        }

        swipeTrailRow.setOnClickListener {
            showSwipeTrailDialog()
        }

        findViewById<View>(R.id.voiceInputRow).setOnClickListener {
            showVoiceInputDialog()
        }

        leftKeyModesRow.setOnClickListener {
            if (LayoutPackManager.resolveActive(this).isGboardStyle()) {
                Toast.makeText(this, "Tool slots are managed by Gboard layout", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showBottomKeyOptionsDialog(isLeftSlot = true)
        }

        rightKeyModesRow.setOnClickListener {
            if (LayoutPackManager.resolveActive(this).isGboardStyle()) {
                Toast.makeText(this, "Tool slots are managed by Gboard layout", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showBottomKeyOptionsDialog(isLeftSlot = false)
        }

        findViewById<View>(R.id.themeRow).setOnClickListener {
            showThemeDialog()
        }

        findViewById<View>(R.id.fontRow).setOnClickListener {
            showFontDialog()
        }

        findViewById<View>(R.id.koFiButton).setOnClickListener {
            openLink("https://ko-fi.com/dotslimy")
        }

        findViewById<View>(R.id.librariesRow).setOnClickListener {
            showLibrariesDialog()
        }

        findViewById<View>(R.id.authorRow).setOnClickListener {
            openLink("https://github.com/MathieuDvv")
        }
    }

    private fun showKeyboardLayoutDialog() {
        val installed = LayoutPackManager.listInstalled(this)
        if (installed.isEmpty()) {
            Toast.makeText(this, "No layout packs found", Toast.LENGTH_SHORT).show()
            return
        }
        val activePackId = KeyboardModeSettings.loadActiveLayoutPackId(this)
        val labels = installed.map { formatLayoutPackLabel(it) }.toTypedArray()
        val selected = installed.indexOfFirst { it.id == activePackId }.coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Keyboard layout")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                val selectedPack = installed[which]
                LayoutPackManager.setActive(this, selectedPack.id)
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteLayoutPackDialog() {
        val imported = LayoutPackManager.listImported(this)
        if (imported.isEmpty()) {
            Toast.makeText(this, "No imported layout packs to delete", Toast.LENGTH_SHORT).show()
            return
        }

        val labels = imported.map { "${it.displayName} (${it.id})" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Delete layout pack")
            .setItems(labels) { _, which ->
                val selected = imported[which]
                AlertDialog.Builder(this)
                    .setTitle("Delete ${selected.displayName}?")
                    .setMessage("This removes the imported file from this device.")
                    .setPositiveButton("Delete") { _, _ ->
                        val deleted = LayoutPackManager.deleteImportedPack(this, selected.id)
                        if (deleted) {
                            refreshValues()
                            Toast.makeText(this, "Layout pack deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Could not delete this pack", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchLayoutPackImportPicker() {
        importLayoutPackLauncher.launch(arrayOf("text/xml", "application/xml", "*/*"))
    }

    private fun importLayoutPackFromUri(uri: Uri) {
        val imported = runCatching {
            LayoutPackManager.importFromUri(this, uri)
        }.getOrElse { error ->
            val message = error.message?.takeIf { it.isNotBlank() } ?: "Invalid layout file"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            return
        }

        LayoutPackManager.setActive(this, imported.id)
        refreshValues()
        Toast.makeText(this, "Imported: ${imported.displayName}", Toast.LENGTH_SHORT).show()
    }

    private fun formatLayoutPackLabel(pack: LayoutPack): String {
        val sourceLabel = if (pack.source == LayoutPackSource.BUILTIN) "Built-in" else "Imported"
        val bottomLabel = if (pack.isGboardStyle()) "Gboard" else "Classic"
        return "${pack.displayName} ($bottomLabel, $sourceLabel)"
    }

    private fun keyboardSummary(pack: LayoutPack): String {
        val bottom = if (pack.isGboardStyle()) "Gboard" else "Classic"
        return "${pack.displayName} • $bottom"
    }

    private fun showLanguageDialog() {
        val current = KeyboardModeSettings.loadLanguageMode(this)
        val options = arrayOf("French", "English", "French + English", "Disabled")
        val selected = when (current) {
            KeyboardLanguageMode.FRENCH -> 0
            KeyboardLanguageMode.ENGLISH -> 1
            KeyboardLanguageMode.BOTH -> 2
            KeyboardLanguageMode.DISABLED -> 3
        }

        AlertDialog.Builder(this)
            .setTitle("Basic autocorrect")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                val mode = when (which) {
                    1 -> KeyboardLanguageMode.ENGLISH
                    2 -> KeyboardLanguageMode.BOTH
                    3 -> KeyboardLanguageMode.DISABLED
                    else -> KeyboardLanguageMode.FRENCH
                }
                KeyboardModeSettings.saveLanguageMode(this, mode)
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showProviderConfigurationDialog() {
        when (KeyboardModeSettings.loadAiProvider(this)) {
            AiProvider.GEMINI -> showGeminiApiKeyDialog()
            AiProvider.ANTHROPIC -> showAnthropicApiKeyDialog()
            AiProvider.OPENAI_COMPATIBLE -> showOpenAiConfigurationDialog()
        }
    }

    private fun showGeminiApiKeyDialog() {
        val input = EditText(this).apply {
            setText(KeyboardModeSettings.loadGeminiApiKey(this@MainActivity))
            hint = "Paste Gemini API key"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Set Gemini API key")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                KeyboardModeSettings.saveGeminiApiKey(this, input.text?.toString().orEmpty())
                refreshValues()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAiProviderDialog() {
        val providers = AiProvider.entries
        val labels = arrayOf("Gemini", "Anthropic", "OpenAI-compatible")
        val current = KeyboardModeSettings.loadAiProvider(this)
        AlertDialog.Builder(this)
            .setTitle("Choose provider")
            .setSingleChoiceItems(labels, providers.indexOf(current)) { dialog, which ->
                KeyboardModeSettings.saveAiProvider(this, providers[which])
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showProviderModelDialog() {
        when (KeyboardModeSettings.loadAiProvider(this)) {
            AiProvider.GEMINI -> {
                val models = GeminiModel.entries
                val current = KeyboardModeSettings.loadGeminiModel(this)
                AlertDialog.Builder(this)
                    .setTitle("Gemini model")
                    .setSingleChoiceItems(
                        models.map { it.displayName }.toTypedArray(),
                        models.indexOf(current)
                    ) { dialog, which ->
                        KeyboardModeSettings.saveGeminiModel(this, models[which])
                        refreshValues()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            AiProvider.ANTHROPIC -> {
                val models = AnthropicModel.entries
                val current = KeyboardModeSettings.loadAnthropicModel(this)
                AlertDialog.Builder(this)
                    .setTitle("Anthropic model")
                    .setSingleChoiceItems(
                        models.map { it.displayName }.toTypedArray(),
                        models.indexOf(current)
                    ) { dialog, which ->
                        KeyboardModeSettings.saveAnthropicModel(this, models[which])
                        refreshValues()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            AiProvider.OPENAI_COMPATIBLE -> Unit
        }
    }

    private fun showAnthropicApiKeyDialog() {
        val input = EditText(this).apply {
            setText(KeyboardModeSettings.loadAnthropicApiKey(this@MainActivity))
            hint = "Paste Anthropic API key"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle("Set Anthropic API key")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                KeyboardModeSettings.saveAnthropicApiKey(
                    this,
                    input.text?.toString().orEmpty()
                )
                refreshValues()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showOpenAiConfigurationDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, 0, padding, 0)
        }
        val presets = OpenAiProviderPreset.entries
        val currentPreset = KeyboardModeSettings.loadOpenAiPreset(this)
        val presetSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                presets.map { it.displayName }
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(presets.indexOf(currentPreset))
        }
        val baseUrlInput = EditText(this).apply {
            hint = "Base URL (https://…/v1)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(KeyboardModeSettings.loadOpenAiBaseUrl(this@MainActivity))
        }
        val modelInput = EditText(this).apply {
            hint = "Model identifier"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(KeyboardModeSettings.loadOpenAiModel(this@MainActivity))
        }
        val apiKeyInput = EditText(this).apply {
            hint = "API key"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            setText(KeyboardModeSettings.loadOpenAiApiKey(this@MainActivity))
        }
        fun applyPreset(preset: OpenAiProviderPreset, replaceModel: Boolean) {
            val isCustom = preset == OpenAiProviderPreset.CUSTOM
            baseUrlInput.isEnabled = isCustom
            baseUrlInput.alpha = if (isCustom) 1f else 0.65f
            if (!isCustom) {
                baseUrlInput.setText(preset.defaultBaseUrl)
                if (replaceModel || modelInput.text.isNullOrBlank()) {
                    modelInput.setText(preset.defaultModel)
                }
            }
        }
        var initialSelectionDelivered = false
        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                applyPreset(
                    presets[position],
                    replaceModel = initialSelectionDelivered
                )
                initialSelectionDelivered = true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        applyPreset(currentPreset, replaceModel = false)
        container.addView(TextView(this).apply {
            text = "Service"
            setPadding(0, 8, 0, 0)
        })
        container.addView(presetSpinner)
        container.addView(baseUrlInput)
        container.addView(modelInput)
        container.addView(apiKeyInput)

        AlertDialog.Builder(this)
            .setTitle("OpenAI-compatible provider")
            .setMessage("Choose a service to fill its endpoint and recommended model, then add your personal API key.")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val preset = presets[presetSpinner.selectedItemPosition]
                val baseUrl = baseUrlInput.text?.toString().orEmpty().trim().trimEnd('/')
                val model = modelInput.text?.toString().orEmpty().trim()
                val apiKey = apiKeyInput.text?.toString().orEmpty().trim()
                val validHttpsUrl = runCatching {
                    Uri.parse(baseUrl).let { it.scheme == "https" && !it.host.isNullOrBlank() }
                }.getOrDefault(false)
                if (!validHttpsUrl || model.isBlank() || apiKey.isBlank()) {
                    Toast.makeText(
                        this,
                        "Enter an HTTPS base URL, model, and API key",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    KeyboardModeSettings.saveOpenAiConfiguration(
                        this,
                        preset,
                        baseUrl,
                        model,
                        apiKey
                    )
                    refreshValues()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showHapticModeDialog() {
        val modes = HapticMode.entries
        val labels = arrayOf("Off", "System", "Light", "Medium", "Strong")
        val current = KeyboardModeSettings.loadHapticMode(this)
        AlertDialog.Builder(this)
            .setTitle("Key vibration")
            .setSingleChoiceItems(labels, modes.indexOf(current)) { dialog, which ->
                KeyboardModeSettings.saveHapticMode(this, modes[which])
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLanguageProfilesDialog() {
        val (profileA, profileB) = KeyboardModeSettings.loadLanguageProfiles(this)
        val labels = arrayOf(
            "Profile A — ${formatLanguageProfile(profileA)}",
            "Profile B — ${formatLanguageProfile(profileB)}"
        )
        AlertDialog.Builder(this)
            .setTitle("Quick language profiles")
            .setItems(labels) { _, which ->
                showLanguageProfileLayoutDialog(
                    if (which == 0) LanguageProfileSlot.A else LanguageProfileSlot.B
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLanguageProfileLayoutDialog(slot: LanguageProfileSlot) {
        val installed = LayoutPackManager.listInstalled(this)
        if (installed.isEmpty()) return
        val profile = if (slot == LanguageProfileSlot.A) {
            KeyboardModeSettings.loadLanguageProfiles(this).first
        } else {
            KeyboardModeSettings.loadLanguageProfiles(this).second
        }
        val selected = installed.indexOfFirst { it.id == profile.layoutPackId }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Profile ${slot.name}: layout")
            .setSingleChoiceItems(
                installed.map(::formatLayoutPackLabel).toTypedArray(),
                selected
            ) { dialog, which ->
                dialog.dismiss()
                showLanguageProfileCorrectionDialog(slot, installed[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLanguageProfileCorrectionDialog(
        slot: LanguageProfileSlot,
        pack: LayoutPack
    ) {
        val modes = KeyboardLanguageMode.entries
        val labels = arrayOf("French", "English", "French + English", "Disabled")
        val profile = if (slot == LanguageProfileSlot.A) {
            KeyboardModeSettings.loadLanguageProfiles(this).first
        } else {
            KeyboardModeSettings.loadLanguageProfiles(this).second
        }
        AlertDialog.Builder(this)
            .setTitle("Profile ${slot.name}: autocorrect")
            .setSingleChoiceItems(labels, modes.indexOf(profile.languageMode)) { dialog, which ->
                KeyboardModeSettings.saveLanguageProfile(this, slot, pack.id, modes[which])
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showThemeDialog() {
        val current = KeyboardModeSettings.loadThemeMode(this)
        val options = arrayOf("System", "Light", "Dark", "AMOLED Black")
        val selected = when (current) {
            AppThemeMode.SYSTEM -> 0
            AppThemeMode.LIGHT -> 1
            AppThemeMode.DARK -> 2
            AppThemeMode.AMOLED -> 3
        }

        AlertDialog.Builder(this)
            .setTitle("Theme")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                val mode = when (which) {
                    1 -> AppThemeMode.LIGHT
                    2 -> AppThemeMode.DARK
                    3 -> AppThemeMode.AMOLED
                    else -> AppThemeMode.SYSTEM
                }
                KeyboardModeSettings.saveThemeMode(this, mode)
                applyThemePreference(mode)
                refreshValues()
                dialog.dismiss()
                recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBottomKeyOptionsDialog(isLeftSlot: Boolean) {
        val allModes = BottomKeyMode.entries
        val options = allModes.flatMapIndexed { firstIndex, first ->
            allModes.drop(firstIndex + 1).map { second -> BottomKeyPairOption(first, second) }
        }
        val labels = options.map { formatBottomModePairLabel(it.first, it.second) }.toTypedArray()
        val (leftOptions, rightOptions) = KeyboardModeSettings.loadBottomSlotOptions(this)
        val currentSet = if (isLeftSlot) leftOptions.toSet() else rightOptions.toSet()
        val selected = options.indexOfFirst { setOf(it.first, it.second) == currentSet }.coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(if (isLeftSlot) "Left key options" else "Right key options")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                val selectedPair = options[which]
                if (isLeftSlot) {
                    KeyboardModeSettings.saveLeftSlotOptions(this, selectedPair.first, selectedPair.second)
                } else {
                    KeyboardModeSettings.saveRightSlotOptions(this, selectedPair.first, selectedPair.second)
                }
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showWordPredictionDialog() {
        val enabled = KeyboardModeSettings.loadWordPredictionEnabled(this)
        val options = arrayOf("Enabled", "Disabled")
        val selected = if (enabled) 0 else 1

        AlertDialog.Builder(this)
            .setTitle("Word prediction")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                KeyboardModeSettings.saveWordPredictionEnabled(this, which == 0)
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNumberRowDialog() {
        val enabled = KeyboardModeSettings.loadNumberRowEnabled(this)
        val options = arrayOf("Enabled", "Disabled")
        val selected = if (enabled) 0 else 1

        AlertDialog.Builder(this)
            .setTitle("Number row")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                KeyboardModeSettings.saveNumberRowEnabled(this, which == 0)
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAutoSpaceAfterPunctuationDialog() {
        val enabled = KeyboardModeSettings.loadAutoSpaceAfterPunctuationEnabled(this)
        val options = arrayOf("Enabled", "Disabled")
        val selected = if (enabled) 0 else 1

        AlertDialog.Builder(this)
            .setTitle("Auto-space after punctuation")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                KeyboardModeSettings.saveAutoSpaceAfterPunctuationEnabled(this, which == 0)
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAutoCapitalizeAfterPunctuationDialog() {
        val enabled = KeyboardModeSettings.loadAutoCapitalizeAfterPunctuationEnabled(this)
        val options = arrayOf("Enabled", "Disabled")
        val selected = if (enabled) 0 else 1

        AlertDialog.Builder(this)
            .setTitle("Auto-capitalize after punctuation")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                KeyboardModeSettings.saveAutoCapitalizeAfterPunctuationEnabled(this, which == 0)
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReturnToLettersAfterNumberSpaceDialog() {
        val enabled = KeyboardModeSettings.loadReturnToLettersAfterNumberSpaceEnabled(this)
        val options = arrayOf("Enabled", "Disabled")
        val selected = if (enabled) 0 else 1

        AlertDialog.Builder(this)
            .setTitle("Return to letters after numbers")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                KeyboardModeSettings.saveReturnToLettersAfterNumberSpaceEnabled(this, which == 0)
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSwipeTypingDialog() {
        val enabled = KeyboardModeSettings.loadSwipeTypingEnabled(this)
        val options = arrayOf("Enabled", "Disabled")
        val selected = if (enabled) 0 else 1

        AlertDialog.Builder(this)
            .setTitle("Swipe typing")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                KeyboardModeSettings.saveSwipeTypingEnabled(this, which == 0)
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFontDialog() {
        val current = KeyboardModeSettings.loadFontMode(this)
        val options = arrayOf("Inter", "Roboto")
        val selected = if (current == KeyboardFontMode.ROBOTO) 1 else 0

        AlertDialog.Builder(this)
            .setTitle("Keyboard font")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                val mode = if (which == 1) KeyboardFontMode.ROBOTO else KeyboardFontMode.INTER
                KeyboardModeSettings.saveFontMode(this, mode)
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVoiceInputDialog() {
        val enabled = KeyboardModeSettings.loadVoiceInputEnabled(this)
        val options = arrayOf("Enabled", "Disabled")
        val selected = if (enabled) 0 else 1

        AlertDialog.Builder(this)
            .setTitle("Voice input")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                val shouldEnable = which == 0
                KeyboardModeSettings.saveVoiceInputEnabled(this, shouldEnable)
                if (shouldEnable && !hasRecordAudioPermission()) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        REQUEST_RECORD_AUDIO_PERMISSION
                    )
                }
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSwipeTrailDialog() {
        val enabled = KeyboardModeSettings.loadSwipeTrailEnabled(this)
        val options = arrayOf("Enabled", "Disabled")
        val selected = if (enabled) 0 else 1

        AlertDialog.Builder(this)
            .setTitle("Showing the trail")
            .setSingleChoiceItems(options, selected) { dialog, which ->
                KeyboardModeSettings.saveSwipeTrailEnabled(this, which == 0)
                refreshValues()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshValues() {
        val activeLayoutPack = LayoutPackManager.resolveActive(this)
        val isGboardLayout = activeLayoutPack.isGboardStyle()
        val provider = KeyboardModeSettings.loadAiProvider(this)
        aiProviderValue.text = when (provider) {
            AiProvider.GEMINI -> "Gemini"
            AiProvider.ANTHROPIC -> "Anthropic"
            AiProvider.OPENAI_COMPATIBLE ->
                KeyboardModeSettings.loadOpenAiPreset(this).displayName
        }
        val hasDedicatedModelRow = provider != AiProvider.OPENAI_COMPATIBLE
        geminiModelRow.visibility = if (hasDedicatedModelRow) View.VISIBLE else View.GONE
        aiModelDivider.visibility = if (hasDedicatedModelRow) View.VISIBLE else View.GONE
        when (provider) {
            AiProvider.GEMINI -> {
                aiModelLabel.text = "Gemini model"
                geminiModelValue.text = KeyboardModeSettings.loadGeminiModel(this).displayName
            }
            AiProvider.ANTHROPIC -> {
                aiModelLabel.text = "Anthropic model"
                geminiModelValue.text = KeyboardModeSettings.loadAnthropicModel(this).displayName
            }
            AiProvider.OPENAI_COMPATIBLE -> Unit
        }
        val configuredKey = when (provider) {
            AiProvider.GEMINI -> KeyboardModeSettings.loadGeminiApiKey(this)
            AiProvider.ANTHROPIC -> KeyboardModeSettings.loadAnthropicApiKey(this)
            AiProvider.OPENAI_COMPATIBLE -> KeyboardModeSettings.loadOpenAiApiKey(this)
        }
        statusText.text = maskApiKeyForDisplay(configuredKey)
        languageValue.text = when (KeyboardModeSettings.loadLanguageMode(this)) {
            KeyboardLanguageMode.FRENCH -> "French"
            KeyboardLanguageMode.ENGLISH -> "English"
            KeyboardLanguageMode.BOTH -> "French + English"
            KeyboardLanguageMode.DISABLED -> "Disabled"
        }
        val (profileA, profileB) = KeyboardModeSettings.loadLanguageProfiles(this)
        languageProfilesValue.text =
            "A: ${profileDisplayName(profileA)} • B: ${profileDisplayName(profileB)}"
        keyboardValue.text = keyboardSummary(activeLayoutPack)
        val hasImportedActiveLayout = activeLayoutPack.source == LayoutPackSource.IMPORTED
        uploadedLayoutNoticeCard.visibility = if (hasImportedActiveLayout) View.VISIBLE else View.GONE
        if (hasImportedActiveLayout) {
            uploadedLayoutNoticeText.text =
                "Custom layout active (${activeLayoutPack.displayName}). Word prediction and autocorrect still use English/French only; only the layout changed."
        }
        numberRowSettingValue.text = if (KeyboardModeSettings.loadNumberRowEnabled(this)) {
            "Enabled"
        } else {
            "Disabled"
        }
        autoSpaceAfterPunctuationValue.text = if (KeyboardModeSettings.loadAutoSpaceAfterPunctuationEnabled(this)) {
            "Enabled"
        } else {
            "Disabled"
        }
        autoCapitalizeAfterPunctuationValue.text =
            if (KeyboardModeSettings.loadAutoCapitalizeAfterPunctuationEnabled(this)) {
                "Enabled"
            } else {
                "Disabled"
            }
        returnToLettersAfterNumberSpaceValue.text =
            if (KeyboardModeSettings.loadReturnToLettersAfterNumberSpaceEnabled(this)) {
                "Enabled"
            } else {
                "Disabled"
            }
        wordPredictionValue.text = if (KeyboardModeSettings.loadWordPredictionEnabled(this)) {
            "Enabled"
        } else {
            "Disabled"
        }
        val swipeTypingEnabled = KeyboardModeSettings.loadSwipeTypingEnabled(this)
        swipeTypingValue.text = if (swipeTypingEnabled) {
            "Enabled"
        } else {
            "Disabled"
        }
        swipeTrailValue.text = if (KeyboardModeSettings.loadSwipeTrailEnabled(this)) {
            "Enabled"
        } else {
            "Disabled"
        }
        swipeTrailRow.visibility = if (swipeTypingEnabled) View.VISIBLE else View.GONE
        swipeTrailDivider.visibility = if (swipeTypingEnabled) View.VISIBLE else View.GONE
        voiceInputValue.text = if (KeyboardModeSettings.loadVoiceInputEnabled(this)) {
            "Enabled"
        } else {
            "Disabled"
        }
        hapticModeValue.text = when (KeyboardModeSettings.loadHapticMode(this)) {
            HapticMode.OFF -> "Off"
            HapticMode.SYSTEM -> "System"
            HapticMode.LIGHT -> "Light"
            HapticMode.MEDIUM -> "Medium"
            HapticMode.STRONG -> "Strong"
        }
        val (leftOptions, rightOptions) = KeyboardModeSettings.loadBottomSlotOptions(this)
        val importedCount = LayoutPackManager.listImported(this).size
        deleteLayoutPackValue.text = if (importedCount <= 0) "None" else "$importedCount imported"
        deleteLayoutPackRow.isEnabled = importedCount > 0
        deleteLayoutPackRow.alpha = if (importedCount > 0) 1f else 0.5f
        leftKeyModesValue.text = if (isGboardLayout) {
            "Single key (hold for tools)"
        } else {
            formatBottomModePairLabel(leftOptions[0], leftOptions[1])
        }
        rightKeyModesValue.text = if (isGboardLayout) {
            "Disabled in Gboard layout"
        } else {
            formatBottomModePairLabel(rightOptions[0], rightOptions[1])
        }
        leftKeyModesRow.isEnabled = !isGboardLayout
        rightKeyModesRow.isEnabled = !isGboardLayout
        leftKeyModesRow.alpha = if (isGboardLayout) 0.5f else 1f
        rightKeyModesRow.alpha = if (isGboardLayout) 0.5f else 1f
        themeValue.text = when (KeyboardModeSettings.loadThemeMode(this)) {
            AppThemeMode.SYSTEM -> "System"
            AppThemeMode.LIGHT -> "Light"
            AppThemeMode.DARK -> "Dark"
            AppThemeMode.AMOLED -> "AMOLED Black"
        }
        fontValue.text = when (KeyboardModeSettings.loadFontMode(this)) {
            KeyboardFontMode.INTER -> "Inter"
            KeyboardFontMode.ROBOTO -> "Roboto"
        }
    }

    private fun maskApiKeyForDisplay(key: String): String {
        if (key.isBlank()) {
            return "Not set"
        }
        val visible = key.takeLast(4)
        return "••••••••••••$visible"
    }

    private fun formatBottomModePairLabel(first: BottomKeyMode, second: BottomKeyMode): String {
        return "${formatBottomModeLabel(first)} + ${formatBottomModeLabel(second)}"
    }

    private fun formatBottomModeLabel(mode: BottomKeyMode): String {
        return when (mode) {
            BottomKeyMode.AI -> "AI"
            BottomKeyMode.CLIPBOARD -> "Clipboard"
            BottomKeyMode.EMOJI -> "Emoji"
            BottomKeyMode.LANGUAGE -> "Language"
            BottomKeyMode.APOSTROPHE -> "Apostrophe"
        }
    }

    private fun formatLanguageProfile(profile: LanguageProfile): String {
        val pack = LayoutPackManager.listInstalled(this)
            .firstOrNull { it.id == profile.layoutPackId }
            ?.displayName
            ?: profile.layoutPackId
        return "$pack, ${languageModeLabel(profile.languageMode)}"
    }

    private fun profileDisplayName(profile: LanguageProfile): String {
        val layoutName = LayoutPackManager.listInstalled(this)
            .firstOrNull { it.id == profile.layoutPackId }
            ?.displayName
            ?: profile.layoutPackId
        return resolveLanguageProfileDisplayName(profile.customName, layoutName)
    }

    private fun languageModeLabel(mode: KeyboardLanguageMode): String {
        return when (mode) {
            KeyboardLanguageMode.FRENCH -> "French"
            KeyboardLanguageMode.ENGLISH -> "English"
            KeyboardLanguageMode.BOTH -> "French + English"
            KeyboardLanguageMode.DISABLED -> "Disabled"
        }
    }

    private fun showLibrariesDialog() {
        val message = """
            • AndroidX Core KTX — Apache License 2.0
            • AndroidX AppCompat — Apache License 2.0
            • AndroidX RecyclerView — Apache License 2.0
            • Material Components for Android — Apache License 2.0
            • Kotlin Coroutines (Android) — Apache License 2.0
            • OkHttp — Apache License 2.0
            • Kotlin Standard Library — Apache License 2.0

            Design note:
            Nboard is heavily inspired by Nothing and its aesthetic (Nothing Technology Limited).
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Libraries & licences")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

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

    private fun openLink(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun maybeShowFirstLaunchOnboarding() {
        if (!KeyboardModeSettings.loadOnboardingCompleted(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_RECORD_AUDIO_PERMISSION) {
            return
        }
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            KeyboardModeSettings.saveVoiceInputEnabled(this, false)
            refreshValues()
            Toast.makeText(
                this,
                "Microphone permission denied. Voice input disabled.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 1004
    }
}

private data class BottomKeyPairOption(
    val first: BottomKeyMode,
    val second: BottomKeyMode
)
