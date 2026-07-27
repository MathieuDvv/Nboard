package com.nboard.ime

import android.content.Context

enum class BottomKeyMode(val value: String) {
    AI("ai"),
    CLIPBOARD("clipboard"),
    EMOJI("emoji"),
    LANGUAGE("language"),
    APOSTROPHE("apostrophe")
}

enum class KeyboardLayoutMode(val value: String) {
    AZERTY("azerty"),
    QWERTY("qwerty"),
    GBOARD_AZERTY("gboard_azerty"),
    GBOARD_QWERTY("gboard_qwerty");

    fun isQwerty(): Boolean {
        return this == QWERTY || this == GBOARD_QWERTY
    }

    fun isGboard(): Boolean {
        return this == GBOARD_AZERTY || this == GBOARD_QWERTY
    }
}

enum class KeyboardLanguageMode(val value: String) {
    FRENCH("french"),
    ENGLISH("english"),
    BOTH("both"),
    DISABLED("disabled")
}

enum class AppThemeMode(val value: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    AMOLED("amoled")
}

enum class KeyboardFontMode(val value: String) {
    INTER("inter"),
    ROBOTO("roboto")
}

enum class AiProvider(val value: String) {
    GEMINI("gemini"),
    ANTHROPIC("anthropic"),
    OPENAI_COMPATIBLE("openai_compatible")
}

enum class GeminiModel(val modelId: String, val displayName: String) {
    GEMINI_3_6_FLASH("gemini-3.6-flash", "Gemini 3.6 Flash"),
    GEMINI_3_5_FLASH("gemini-3.5-flash", "Gemini 3.5 Flash"),
    GEMINI_3_5_FLASH_LITE("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite");

    companion object {
        fun fromId(raw: String?): GeminiModel {
            return entries.firstOrNull { it.modelId == raw } ?: GEMINI_3_6_FLASH
        }
    }
}

enum class AnthropicModel(val modelId: String, val displayName: String) {
    CLAUDE_SONNET_5("claude-sonnet-5", "Claude Sonnet 5"),
    CLAUDE_HAIKU_4_5("claude-haiku-4-5", "Claude Haiku 4.5"),
    CLAUDE_OPUS_5("claude-opus-5", "Claude Opus 5");

    companion object {
        fun fromId(raw: String?): AnthropicModel {
            return entries.firstOrNull { it.modelId == raw } ?: CLAUDE_SONNET_5
        }
    }
}

enum class OpenAiProviderPreset(
    val value: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String
) {
    OPENAI("openai", "OpenAI", "https://api.openai.com/v1", "gpt-5-mini"),
    DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com", "deepseek-v4-flash"),
    KIMI("kimi", "Kimi", "https://api.moonshot.ai/v1", "kimi-k3"),
    GROQ("groq", "Groq", "https://api.groq.com/openai/v1", "openai/gpt-oss-20b"),
    OPENROUTER("openrouter", "OpenRouter", "https://openrouter.ai/api/v1", "openrouter/auto"),
    CUSTOM("custom", "Custom", "", "");

    companion object {
        fun fromValue(raw: String?): OpenAiProviderPreset? {
            return entries.firstOrNull { it.value == raw }
        }

        fun matchingBaseUrl(raw: String?): OpenAiProviderPreset? {
            val normalized = raw.orEmpty().trim().trimEnd('/')
            return entries.firstOrNull {
                it != CUSTOM && it.defaultBaseUrl.trimEnd('/') == normalized
            }
        }
    }
}

enum class HapticMode(val value: String) {
    OFF("off"),
    SYSTEM("system"),
    LIGHT("light"),
    MEDIUM("medium"),
    STRONG("strong")
}

enum class LanguageProfileSlot(val value: String) {
    A("a"),
    B("b")
}

data class LanguageProfile(
    val customName: String,
    val layoutPackId: String,
    val languageMode: KeyboardLanguageMode,
    val qwertyLike: Boolean,
    val leftKeyModes: List<BottomKeyMode>,
    val rightKeyModes: List<BottomKeyMode>
)

internal fun resolveLanguageProfileDisplayName(
    customName: String,
    layoutDisplayName: String
): String {
    return customName.trim().ifBlank { layoutDisplayName }
}

object KeyboardModeSettings {
    const val PREFS_NAME = "nboard_settings"
    private const val KEY_LEFT_MODE = "left_bottom_mode"
    private const val KEY_RIGHT_MODE = "right_bottom_mode"
    private const val KEY_LEFT_OPTION_PRIMARY = "left_bottom_option_primary"
    private const val KEY_LEFT_OPTION_SECONDARY = "left_bottom_option_secondary"
    private const val KEY_RIGHT_OPTION_PRIMARY = "right_bottom_option_primary"
    private const val KEY_RIGHT_OPTION_SECONDARY = "right_bottom_option_secondary"
    private const val KEY_LAYOUT_MODE = "keyboard_layout_mode"
    private const val KEY_ACTIVE_LAYOUT_PACK_ID = "active_layout_pack_id"
    private const val KEY_LANGUAGE_MODE = "keyboard_language_mode"
    private const val KEY_API_KEY = "gemini_api_key"
    private const val KEY_AI_PROVIDER = "ai_provider"
    private const val KEY_GEMINI_MODEL = "gemini_model"
    private const val KEY_ANTHROPIC_MODEL = "anthropic_model"
    private const val KEY_ANTHROPIC_API_KEY = "anthropic_api_key"
    private const val KEY_OPENAI_PRESET = "openai_compatible_preset"
    private const val KEY_OPENAI_BASE_URL = "openai_compatible_base_url"
    private const val KEY_OPENAI_MODEL = "openai_compatible_model"
    private const val KEY_OPENAI_API_KEY = "openai_compatible_api_key"
    private const val KEY_HAPTIC_MODE = "haptic_mode"
    private const val KEY_LANGUAGE_PROFILES_INITIALIZED = "language_profiles_initialized"
    private const val KEY_LANGUAGE_PROFILE_KEYS_INITIALIZED = "language_profile_keys_initialized"
    private const val KEY_ACTIVE_LANGUAGE_PROFILE = "active_language_profile"
    private const val KEY_PROFILE_A_LAYOUT = "language_profile_a_layout"
    private const val KEY_PROFILE_A_NAME = "language_profile_a_name"
    private const val KEY_PROFILE_A_LANGUAGE = "language_profile_a_language"
    private const val KEY_PROFILE_A_QWERTY = "language_profile_a_qwerty"
    private const val KEY_PROFILE_B_LAYOUT = "language_profile_b_layout"
    private const val KEY_PROFILE_B_NAME = "language_profile_b_name"
    private const val KEY_PROFILE_B_LANGUAGE = "language_profile_b_language"
    private const val KEY_PROFILE_B_QWERTY = "language_profile_b_qwerty"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_FONT_MODE = "font_mode"
    private const val KEY_WORD_PREDICTION_ENABLED = "word_prediction_enabled"
    private const val KEY_SWIPE_TYPING_ENABLED = "swipe_typing_enabled"
    private const val KEY_SWIPE_TRAIL_ENABLED = "swipe_trail_enabled"
    private const val KEY_VOICE_INPUT_ENABLED = "voice_input_enabled"
    private const val KEY_NUMBER_ROW_ENABLED = "number_row_enabled"
    private const val KEY_AUTO_SPACE_AFTER_PUNCTUATION_ENABLED = "auto_space_after_punctuation_enabled"
    private const val KEY_AUTO_CAPITALIZE_AFTER_PUNCTUATION_ENABLED = "auto_capitalize_after_punctuation_enabled"
    private const val KEY_RETURN_TO_LETTERS_AFTER_NUMBER_SPACE_ENABLED =
        "return_to_letters_after_number_space_enabled"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    fun load(context: Context): Pair<BottomKeyMode, BottomKeyMode> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val (leftPrimary, leftSecondary) = loadBottomSlotOptionsInternal(prefs, true)
        val (rightPrimary, rightSecondary) = loadBottomSlotOptionsInternal(prefs, false)
        val leftOptions = setOf(leftPrimary, leftSecondary)
        val rightOptions = setOf(rightPrimary, rightSecondary)

        val left = parseBottomKeyMode(prefs.getString(KEY_LEFT_MODE, null), leftPrimary)
            .let { mode -> if (mode in leftOptions) mode else leftPrimary }

        val right = parseBottomKeyMode(prefs.getString(KEY_RIGHT_MODE, null), rightPrimary)
            .let { mode -> if (mode in rightOptions) mode else rightPrimary }

        return left to right
    }

    fun save(context: Context, left: BottomKeyMode, right: BottomKeyMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putString(KEY_LEFT_MODE, left.value)
            .putString(KEY_RIGHT_MODE, right.value)
        if (prefs.getBoolean(KEY_LANGUAGE_PROFILE_KEYS_INITIALIZED, false)) {
            val slot = loadActiveLanguageProfileSlot(context)
            editor
                .putString(profileCurrentBottomModeKey(slot, isLeftSlot = true), left.value)
                .putString(profileCurrentBottomModeKey(slot, isLeftSlot = false), right.value)
        }
        editor.apply()
    }

    fun loadBottomSlotOptions(context: Context): Pair<List<BottomKeyMode>, List<BottomKeyMode>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val left = loadBottomSlotOptionsInternal(prefs, true)
        val right = loadBottomSlotOptionsInternal(prefs, false)
        return listOf(left.first, left.second) to listOf(right.first, right.second)
    }

    fun saveLeftSlotOptions(context: Context, first: BottomKeyMode, second: BottomKeyMode) {
        saveBottomSlotOptions(context, isLeftSlot = true, first = first, second = second)
    }

    fun saveRightSlotOptions(context: Context, first: BottomKeyMode, second: BottomKeyMode) {
        saveBottomSlotOptions(context, isLeftSlot = false, first = first, second = second)
    }

    private fun saveBottomSlotOptions(
        context: Context,
        isLeftSlot: Boolean,
        first: BottomKeyMode,
        second: BottomKeyMode
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val normalized = normalizeOptionPair(first, second, isLeftSlot)
        val (primaryKey, secondaryKey, currentModeKey) = if (isLeftSlot) {
            Triple(KEY_LEFT_OPTION_PRIMARY, KEY_LEFT_OPTION_SECONDARY, KEY_LEFT_MODE)
        } else {
            Triple(KEY_RIGHT_OPTION_PRIMARY, KEY_RIGHT_OPTION_SECONDARY, KEY_RIGHT_MODE)
        }

        val currentMode = parseBottomKeyMode(
            prefs.getString(currentModeKey, null),
            normalized.first
        )
        val adjustedMode = if (currentMode == normalized.first || currentMode == normalized.second) {
            currentMode
        } else {
            normalized.first
        }

        prefs.edit()
            .putString(primaryKey, normalized.first.value)
            .putString(secondaryKey, normalized.second.value)
            .putString(currentModeKey, adjustedMode.value)
            .also { editor ->
                if (prefs.getBoolean(KEY_LANGUAGE_PROFILE_KEYS_INITIALIZED, false)) {
                    val slot = loadActiveLanguageProfileSlot(context)
                    editor
                        .putString(
                            profileBottomOptionKey(slot, isLeftSlot, isPrimary = true),
                            normalized.first.value
                        )
                        .putString(
                            profileBottomOptionKey(slot, isLeftSlot, isPrimary = false),
                            normalized.second.value
                        )
                        .putString(
                            profileCurrentBottomModeKey(slot, isLeftSlot),
                            adjustedMode.value
                        )
                }
            }
            .apply()
    }

    fun loadLayoutMode(context: Context): KeyboardLayoutMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAYOUT_MODE, KeyboardLayoutMode.AZERTY.value)
        return when (raw) {
            KeyboardLayoutMode.QWERTY.value -> KeyboardLayoutMode.QWERTY
            KeyboardLayoutMode.GBOARD_AZERTY.value -> KeyboardLayoutMode.GBOARD_AZERTY
            KeyboardLayoutMode.GBOARD_QWERTY.value -> KeyboardLayoutMode.GBOARD_QWERTY
            else -> KeyboardLayoutMode.AZERTY
        }
    }

    fun saveLayoutMode(context: Context, mode: KeyboardLayoutMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val packId = LayoutPackManager.defaultPackIdForLegacyMode(mode)
        val editor = prefs.edit()
            .putString(KEY_LAYOUT_MODE, mode.value)
            .putString(KEY_ACTIVE_LAYOUT_PACK_ID, packId)
        if (prefs.getBoolean(KEY_LANGUAGE_PROFILES_INITIALIZED, false)) {
            val slot = loadActiveLanguageProfileSlot(context)
            editor
                .putString(profileLayoutKey(slot), packId)
                .putBoolean(profileQwertyKey(slot), mode.isQwerty())
        }
        editor.apply()
    }

    fun loadActiveLayoutPackId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val explicit = prefs.getString(KEY_ACTIVE_LAYOUT_PACK_ID, null)?.trim().orEmpty()
        if (explicit.isNotBlank()) {
            return explicit
        }
        return LayoutPackManager.defaultPackIdForLegacyMode(loadLayoutMode(context))
    }

    fun saveActiveLayoutPackId(context: Context, packId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val normalized = packId.trim()
        val editor = prefs.edit().putString(KEY_ACTIVE_LAYOUT_PACK_ID, normalized)
        if (prefs.getBoolean(KEY_LANGUAGE_PROFILES_INITIALIZED, false)) {
            val slot = loadActiveLanguageProfileSlot(context)
            val pack = LayoutPackManager.listInstalled(context).firstOrNull { it.id == normalized }
            editor
                .putString(profileLayoutKey(slot), normalized)
                .putBoolean(profileQwertyKey(slot), pack?.isQwertyLike ?: false)
        }
        editor.apply()
    }

    fun loadLanguageMode(context: Context): KeyboardLanguageMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE_MODE, KeyboardLanguageMode.FRENCH.value)
        return when (raw) {
            KeyboardLanguageMode.ENGLISH.value -> KeyboardLanguageMode.ENGLISH
            KeyboardLanguageMode.BOTH.value -> KeyboardLanguageMode.BOTH
            KeyboardLanguageMode.DISABLED.value -> KeyboardLanguageMode.DISABLED
            else -> KeyboardLanguageMode.FRENCH
        }
    }

    fun saveLanguageMode(context: Context, mode: KeyboardLanguageMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit().putString(KEY_LANGUAGE_MODE, mode.value)
        if (prefs.getBoolean(KEY_LANGUAGE_PROFILES_INITIALIZED, false)) {
            editor.putString(profileLanguageKey(loadActiveLanguageProfileSlot(context)), mode.value)
        }
        editor.apply()
    }

    fun loadGeminiApiKey(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_API_KEY, "")
            .orEmpty()
    }

    fun saveGeminiApiKey(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, value.trim())
            .apply()
    }

    fun loadAiProvider(context: Context): AiProvider {
        return when (
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_AI_PROVIDER, AiProvider.GEMINI.value)
        ) {
            AiProvider.ANTHROPIC.value -> AiProvider.ANTHROPIC
            AiProvider.OPENAI_COMPATIBLE.value -> AiProvider.OPENAI_COMPATIBLE
            else -> AiProvider.GEMINI
        }
    }

    fun saveAiProvider(context: Context, provider: AiProvider) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AI_PROVIDER, provider.value)
            .apply()
    }

    fun loadGeminiModel(context: Context): GeminiModel {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GEMINI_MODEL, GeminiModel.GEMINI_3_6_FLASH.modelId)
        return GeminiModel.fromId(raw)
    }

    fun saveGeminiModel(context: Context, model: GeminiModel) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GEMINI_MODEL, model.modelId)
            .apply()
    }

    fun loadAnthropicModel(context: Context): AnthropicModel {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ANTHROPIC_MODEL, AnthropicModel.CLAUDE_SONNET_5.modelId)
        return AnthropicModel.fromId(raw)
    }

    fun saveAnthropicModel(context: Context, model: AnthropicModel) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ANTHROPIC_MODEL, model.modelId)
            .apply()
    }

    fun loadAnthropicApiKey(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ANTHROPIC_API_KEY, "")
            .orEmpty()
    }

    fun saveAnthropicApiKey(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ANTHROPIC_API_KEY, value.trim())
            .apply()
    }

    fun loadOpenAiPreset(context: Context): OpenAiProviderPreset {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        OpenAiProviderPreset.fromValue(prefs.getString(KEY_OPENAI_PRESET, null))?.let {
            return it
        }
        return OpenAiProviderPreset.matchingBaseUrl(prefs.getString(KEY_OPENAI_BASE_URL, null))
            ?: if (prefs.getString(KEY_OPENAI_BASE_URL, "").isNullOrBlank()) {
                OpenAiProviderPreset.OPENAI
            } else {
                OpenAiProviderPreset.CUSTOM
            }
    }

    fun loadOpenAiBaseUrl(context: Context): String {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_OPENAI_BASE_URL, "")
            .orEmpty()
        return stored.ifBlank { loadOpenAiPreset(context).defaultBaseUrl }
    }

    fun loadOpenAiModel(context: Context): String {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_OPENAI_MODEL, "")
            .orEmpty()
        return stored.ifBlank { loadOpenAiPreset(context).defaultModel }
    }

    fun loadOpenAiApiKey(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_OPENAI_API_KEY, "")
            .orEmpty()
    }

    fun saveOpenAiConfiguration(
        context: Context,
        preset: OpenAiProviderPreset,
        baseUrl: String,
        model: String,
        apiKey: String
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_OPENAI_PRESET, preset.value)
            .putString(KEY_OPENAI_BASE_URL, baseUrl.trim().trimEnd('/'))
            .putString(KEY_OPENAI_MODEL, model.trim())
            .putString(KEY_OPENAI_API_KEY, apiKey.trim())
            .apply()
    }

    fun loadHapticMode(context: Context): HapticMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HAPTIC_MODE, HapticMode.SYSTEM.value)
        return HapticMode.entries.firstOrNull { it.value == raw } ?: HapticMode.SYSTEM
    }

    fun saveHapticMode(context: Context, mode: HapticMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HAPTIC_MODE, mode.value)
            .apply()
    }

    fun loadLanguageProfiles(context: Context): Pair<LanguageProfile, LanguageProfile> {
        ensureLanguageProfiles(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return loadLanguageProfile(context, prefs, LanguageProfileSlot.A) to
            loadLanguageProfile(context, prefs, LanguageProfileSlot.B)
    }

    fun saveLanguageProfile(
        context: Context,
        slot: LanguageProfileSlot,
        layoutPackId: String,
        languageMode: KeyboardLanguageMode
    ) {
        ensureLanguageProfiles(context)
        val pack = LayoutPackManager.listInstalled(context).firstOrNull { it.id == layoutPackId }
            ?: return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(profileLayoutKey(slot), pack.id)
            .putString(profileLanguageKey(slot), languageMode.value)
            .putBoolean(profileQwertyKey(slot), pack.isQwertyLike)
            .apply()

        if (loadActiveLanguageProfileSlot(context) == slot) {
            applyLanguageProfile(context, slot)
        }
    }

    fun saveLanguageProfileName(
        context: Context,
        slot: LanguageProfileSlot,
        customName: String
    ) {
        ensureLanguageProfiles(context)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(profileNameKey(slot), customName.trim())
            .apply()
    }

    fun saveLanguageProfileBottomSlotOptions(
        context: Context,
        slot: LanguageProfileSlot,
        isLeftSlot: Boolean,
        first: BottomKeyMode,
        second: BottomKeyMode
    ) {
        ensureLanguageProfiles(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val normalized = normalizeOptionPair(first, second, isLeftSlot)
        val currentKey = profileCurrentBottomModeKey(slot, isLeftSlot)
        val current = parseBottomKeyMode(
            prefs.getString(currentKey, null),
            normalized.first
        ).let { if (it == normalized.first || it == normalized.second) it else normalized.first }
        val editor = prefs.edit()
            .putString(profileBottomOptionKey(slot, isLeftSlot, true), normalized.first.value)
            .putString(profileBottomOptionKey(slot, isLeftSlot, false), normalized.second.value)
            .putString(currentKey, current.value)
        if (loadActiveLanguageProfileSlot(context) == slot) {
            editor
                .putString(
                    if (isLeftSlot) KEY_LEFT_OPTION_PRIMARY else KEY_RIGHT_OPTION_PRIMARY,
                    normalized.first.value
                )
                .putString(
                    if (isLeftSlot) KEY_LEFT_OPTION_SECONDARY else KEY_RIGHT_OPTION_SECONDARY,
                    normalized.second.value
                )
                .putString(if (isLeftSlot) KEY_LEFT_MODE else KEY_RIGHT_MODE, current.value)
        }
        editor.apply()
    }

    fun loadActiveLanguageProfileSlot(context: Context): LanguageProfileSlot {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_LANGUAGE_PROFILE, LanguageProfileSlot.A.value)
        return if (raw == LanguageProfileSlot.B.value) {
            LanguageProfileSlot.B
        } else {
            LanguageProfileSlot.A
        }
    }

    fun switchLanguageProfile(context: Context): LanguageProfileSlot {
        ensureLanguageProfiles(context)
        val target = if (loadActiveLanguageProfileSlot(context) == LanguageProfileSlot.A) {
            LanguageProfileSlot.B
        } else {
            LanguageProfileSlot.A
        }
        applyLanguageProfile(context, target)
        return target
    }

    private fun ensureLanguageProfiles(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_LANGUAGE_PROFILES_INITIALIZED, false)) {
            val currentPack = LayoutPackManager.resolveActive(context)
            val currentLanguage = loadLanguageMode(context)
            val secondaryPackId = if (currentPack.isQwertyLike) {
                LayoutPackManager.BUILTIN_AZERTY_GBOARD_ID
            } else {
                LayoutPackManager.BUILTIN_QWERTY_GBOARD_ID
            }
            val secondaryLanguage = if (currentPack.isQwertyLike) {
                KeyboardLanguageMode.FRENCH
            } else {
                KeyboardLanguageMode.ENGLISH
            }

            prefs.edit()
                .putBoolean(KEY_LANGUAGE_PROFILES_INITIALIZED, true)
                .putString(KEY_ACTIVE_LANGUAGE_PROFILE, LanguageProfileSlot.A.value)
                .putString(KEY_PROFILE_A_LAYOUT, currentPack.id)
                .putString(KEY_PROFILE_A_LANGUAGE, currentLanguage.value)
                .putBoolean(KEY_PROFILE_A_QWERTY, currentPack.isQwertyLike)
                .putString(KEY_PROFILE_B_LAYOUT, secondaryPackId)
                .putString(KEY_PROFILE_B_LANGUAGE, secondaryLanguage.value)
                .putBoolean(KEY_PROFILE_B_QWERTY, !currentPack.isQwertyLike)
                .apply()
        }

        if (!prefs.getBoolean(KEY_LANGUAGE_PROFILE_KEYS_INITIALIZED, false)) {
            val leftOptions = loadBottomSlotOptionsInternal(prefs, isLeftSlot = true)
            val rightOptions = loadBottomSlotOptionsInternal(prefs, isLeftSlot = false)
            val leftCurrent = parseBottomKeyMode(
                prefs.getString(KEY_LEFT_MODE, null),
                leftOptions.first
            )
            val rightCurrent = parseBottomKeyMode(
                prefs.getString(KEY_RIGHT_MODE, null),
                rightOptions.first
            )
            val editor = prefs.edit().putBoolean(KEY_LANGUAGE_PROFILE_KEYS_INITIALIZED, true)
            LanguageProfileSlot.entries.forEach { slot ->
                editor
                    .putString(profileBottomOptionKey(slot, true, true), leftOptions.first.value)
                    .putString(profileBottomOptionKey(slot, true, false), leftOptions.second.value)
                    .putString(profileCurrentBottomModeKey(slot, true), leftCurrent.value)
                    .putString(profileBottomOptionKey(slot, false, true), rightOptions.first.value)
                    .putString(profileBottomOptionKey(slot, false, false), rightOptions.second.value)
                    .putString(profileCurrentBottomModeKey(slot, false), rightCurrent.value)
            }
            editor.apply()
        }
    }

    private fun loadLanguageProfile(
        context: Context,
        prefs: android.content.SharedPreferences,
        slot: LanguageProfileSlot
    ): LanguageProfile {
        val qwertyLike = prefs.getBoolean(profileQwertyKey(slot), slot == LanguageProfileSlot.B)
        val storedId = prefs.getString(profileLayoutKey(slot), "").orEmpty()
        val installed = LayoutPackManager.listInstalled(context)
        val resolvedPack = installed.firstOrNull { it.id == storedId }
            ?: installed.firstOrNull {
                it.id == if (qwertyLike) {
                    LayoutPackManager.BUILTIN_QWERTY_GBOARD_ID
                } else {
                    LayoutPackManager.BUILTIN_AZERTY_GBOARD_ID
                }
            }
            ?: LayoutPackManager.defaultPack()
        val language = parseLanguageMode(
            prefs.getString(profileLanguageKey(slot), null),
            if (qwertyLike) KeyboardLanguageMode.ENGLISH else KeyboardLanguageMode.FRENCH
        )
        if (resolvedPack.id != storedId) {
            prefs.edit()
                .putString(profileLayoutKey(slot), resolvedPack.id)
                .putBoolean(profileQwertyKey(slot), resolvedPack.isQwertyLike)
                .apply()
        }
        val leftKeyModes = loadProfileBottomOptions(prefs, slot, isLeftSlot = true)
        val rightKeyModes = loadProfileBottomOptions(prefs, slot, isLeftSlot = false)
        return LanguageProfile(
            prefs.getString(profileNameKey(slot), "").orEmpty().trim(),
            resolvedPack.id,
            language,
            resolvedPack.isQwertyLike,
            listOf(leftKeyModes.first, leftKeyModes.second),
            listOf(rightKeyModes.first, rightKeyModes.second)
        )
    }

    private fun applyLanguageProfile(context: Context, slot: LanguageProfileSlot) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val profile = loadLanguageProfile(context, prefs, slot)
        prefs.edit()
            .putString(KEY_ACTIVE_LANGUAGE_PROFILE, slot.value)
            .putString(KEY_ACTIVE_LAYOUT_PACK_ID, profile.layoutPackId)
            .putString(KEY_LANGUAGE_MODE, profile.languageMode.value)
            .putString(KEY_LEFT_OPTION_PRIMARY, profile.leftKeyModes[0].value)
            .putString(KEY_LEFT_OPTION_SECONDARY, profile.leftKeyModes[1].value)
            .putString(KEY_RIGHT_OPTION_PRIMARY, profile.rightKeyModes[0].value)
            .putString(KEY_RIGHT_OPTION_SECONDARY, profile.rightKeyModes[1].value)
            .putString(
                KEY_LEFT_MODE,
                loadProfileCurrentBottomMode(prefs, slot, true, profile.leftKeyModes).value
            )
            .putString(
                KEY_RIGHT_MODE,
                loadProfileCurrentBottomMode(prefs, slot, false, profile.rightKeyModes).value
            )
            .apply()
    }

    fun loadThemeMode(context: Context): AppThemeMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.value)
        return when (raw) {
            AppThemeMode.LIGHT.value -> AppThemeMode.LIGHT
            AppThemeMode.DARK.value -> AppThemeMode.DARK
            AppThemeMode.AMOLED.value,
            "dark_classic" -> AppThemeMode.AMOLED
            else -> AppThemeMode.SYSTEM
        }
    }

    fun saveThemeMode(context: Context, mode: AppThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.value)
            .apply()
    }

    fun loadFontMode(context: Context): KeyboardFontMode {
        return when (
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_FONT_MODE, KeyboardFontMode.INTER.value)
        ) {
            KeyboardFontMode.ROBOTO.value -> KeyboardFontMode.ROBOTO
            else -> KeyboardFontMode.INTER
        }
    }

    fun saveFontMode(context: Context, mode: KeyboardFontMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FONT_MODE, mode.value)
            .apply()
    }

    fun loadWordPredictionEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_WORD_PREDICTION_ENABLED, true)
    }

    fun saveWordPredictionEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WORD_PREDICTION_ENABLED, enabled)
            .apply()
    }

    fun loadSwipeTypingEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SWIPE_TYPING_ENABLED, true)
    }

    fun saveSwipeTypingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SWIPE_TYPING_ENABLED, enabled)
            .apply()
    }

    fun loadVoiceInputEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_VOICE_INPUT_ENABLED, true)
    }

    fun loadNumberRowEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NUMBER_ROW_ENABLED, false)
    }

    fun loadSwipeTrailEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SWIPE_TRAIL_ENABLED, true)
    }

    fun loadAutoSpaceAfterPunctuationEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_SPACE_AFTER_PUNCTUATION_ENABLED, true)
    }

    fun saveAutoSpaceAfterPunctuationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_SPACE_AFTER_PUNCTUATION_ENABLED, enabled)
            .apply()
    }

    fun loadAutoCapitalizeAfterPunctuationEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_CAPITALIZE_AFTER_PUNCTUATION_ENABLED, true)
    }

    fun saveAutoCapitalizeAfterPunctuationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_CAPITALIZE_AFTER_PUNCTUATION_ENABLED, enabled)
            .apply()
    }

    fun loadReturnToLettersAfterNumberSpaceEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_RETURN_TO_LETTERS_AFTER_NUMBER_SPACE_ENABLED, true)
    }

    fun saveReturnToLettersAfterNumberSpaceEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_RETURN_TO_LETTERS_AFTER_NUMBER_SPACE_ENABLED, enabled)
            .apply()
    }

    fun saveSwipeTrailEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SWIPE_TRAIL_ENABLED, enabled)
            .apply()
    }

    fun saveVoiceInputEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_VOICE_INPUT_ENABLED, enabled)
            .apply()
    }

    fun saveNumberRowEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NUMBER_ROW_ENABLED, enabled)
            .apply()
    }

    fun loadOnboardingCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun saveOnboardingCompleted(context: Context, completed: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, completed)
            .apply()
    }

    private fun loadBottomSlotOptionsInternal(
        prefs: android.content.SharedPreferences,
        isLeftSlot: Boolean
    ): Pair<BottomKeyMode, BottomKeyMode> {
        val defaultFirst = if (isLeftSlot) BottomKeyMode.AI else BottomKeyMode.CLIPBOARD
        val defaultSecond = BottomKeyMode.EMOJI
        val primaryKey = if (isLeftSlot) KEY_LEFT_OPTION_PRIMARY else KEY_RIGHT_OPTION_PRIMARY
        val secondaryKey = if (isLeftSlot) KEY_LEFT_OPTION_SECONDARY else KEY_RIGHT_OPTION_SECONDARY

        val rawFirst = parseBottomKeyMode(prefs.getString(primaryKey, null), defaultFirst)
        val rawSecond = parseBottomKeyMode(prefs.getString(secondaryKey, null), defaultSecond)
        return normalizeOptionPair(rawFirst, rawSecond, isLeftSlot)
    }

    private fun normalizeOptionPair(
        first: BottomKeyMode,
        second: BottomKeyMode,
        isLeftSlot: Boolean
    ): Pair<BottomKeyMode, BottomKeyMode> {
        if (first != second) {
            return first to second
        }
        val defaultSecond = if (isLeftSlot) BottomKeyMode.EMOJI else BottomKeyMode.EMOJI
        if (defaultSecond != first) {
            return first to defaultSecond
        }
        val fallback = BottomKeyMode.values().firstOrNull { it != first } ?: BottomKeyMode.EMOJI
        return first to fallback
    }

    private fun parseBottomKeyMode(raw: String?, fallback: BottomKeyMode): BottomKeyMode {
        return when (raw) {
            BottomKeyMode.AI.value -> BottomKeyMode.AI
            BottomKeyMode.CLIPBOARD.value -> BottomKeyMode.CLIPBOARD
            BottomKeyMode.EMOJI.value -> BottomKeyMode.EMOJI
            BottomKeyMode.LANGUAGE.value -> BottomKeyMode.LANGUAGE
            BottomKeyMode.APOSTROPHE.value -> BottomKeyMode.APOSTROPHE
            else -> fallback
        }
    }

    private fun parseLanguageMode(
        raw: String?,
        fallback: KeyboardLanguageMode
    ): KeyboardLanguageMode {
        return when (raw) {
            KeyboardLanguageMode.FRENCH.value -> KeyboardLanguageMode.FRENCH
            KeyboardLanguageMode.ENGLISH.value -> KeyboardLanguageMode.ENGLISH
            KeyboardLanguageMode.BOTH.value -> KeyboardLanguageMode.BOTH
            KeyboardLanguageMode.DISABLED.value -> KeyboardLanguageMode.DISABLED
            else -> fallback
        }
    }

    private fun profileLayoutKey(slot: LanguageProfileSlot): String {
        return if (slot == LanguageProfileSlot.A) KEY_PROFILE_A_LAYOUT else KEY_PROFILE_B_LAYOUT
    }

    private fun profileNameKey(slot: LanguageProfileSlot): String {
        return if (slot == LanguageProfileSlot.A) KEY_PROFILE_A_NAME else KEY_PROFILE_B_NAME
    }

    private fun profileLanguageKey(slot: LanguageProfileSlot): String {
        return if (slot == LanguageProfileSlot.A) KEY_PROFILE_A_LANGUAGE else KEY_PROFILE_B_LANGUAGE
    }

    private fun profileQwertyKey(slot: LanguageProfileSlot): String {
        return if (slot == LanguageProfileSlot.A) KEY_PROFILE_A_QWERTY else KEY_PROFILE_B_QWERTY
    }

    private fun profileBottomOptionKey(
        slot: LanguageProfileSlot,
        isLeftSlot: Boolean,
        isPrimary: Boolean
    ): String {
        val side = if (isLeftSlot) "left" else "right"
        val position = if (isPrimary) "primary" else "secondary"
        return "language_profile_${slot.value}_${side}_bottom_option_$position"
    }

    private fun profileCurrentBottomModeKey(
        slot: LanguageProfileSlot,
        isLeftSlot: Boolean
    ): String {
        val side = if (isLeftSlot) "left" else "right"
        return "language_profile_${slot.value}_${side}_bottom_mode"
    }

    private fun loadProfileBottomOptions(
        prefs: android.content.SharedPreferences,
        slot: LanguageProfileSlot,
        isLeftSlot: Boolean
    ): Pair<BottomKeyMode, BottomKeyMode> {
        val defaultFirst = if (isLeftSlot) BottomKeyMode.AI else BottomKeyMode.CLIPBOARD
        val defaultSecond = BottomKeyMode.EMOJI
        return normalizeOptionPair(
            parseBottomKeyMode(
                prefs.getString(profileBottomOptionKey(slot, isLeftSlot, true), null),
                defaultFirst
            ),
            parseBottomKeyMode(
                prefs.getString(profileBottomOptionKey(slot, isLeftSlot, false), null),
                defaultSecond
            ),
            isLeftSlot
        )
    }

    private fun loadProfileCurrentBottomMode(
        prefs: android.content.SharedPreferences,
        slot: LanguageProfileSlot,
        isLeftSlot: Boolean,
        options: List<BottomKeyMode>
    ): BottomKeyMode {
        val fallback = options.first()
        val stored = parseBottomKeyMode(
            prefs.getString(profileCurrentBottomModeKey(slot, isLeftSlot), null),
            fallback
        )
        return if (stored in options) stored else fallback
    }
}
