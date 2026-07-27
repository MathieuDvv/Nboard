package com.nboard.ime

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LanguageProfilesActivity : AppCompatActivity() {
    private lateinit var profileAViews: ProfileViews
    private lateinit var profileBViews: ProfileViews

    override fun onCreate(savedInstanceState: Bundle?) {
        val themeMode = KeyboardModeSettings.loadThemeMode(this)
        setTheme(themeStyleFor(themeMode))
        applyThemePreference(themeMode)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_profiles)

        profileAViews = bindProfileViews(
            findViewById(R.id.profileACard),
            LanguageProfileSlot.A
        )
        profileBViews = bindProfileViews(
            findViewById(R.id.profileBCard),
            LanguageProfileSlot.B
        )

        findViewById<View>(R.id.profilesBackButton).setOnClickListener { finish() }
        applyStatusBarInset()
        bindProfileActions(profileAViews)
        bindProfileActions(profileBViews)
        refreshProfiles()
    }

    override fun onResume() {
        super.onResume()
        refreshProfiles()
    }

    private fun bindProfileViews(root: View, slot: LanguageProfileSlot): ProfileViews {
        return ProfileViews(
            slot = slot,
            title = root.findViewById(R.id.profileTitle),
            activeBadge = root.findViewById(R.id.profileActiveBadge),
            nameRow = root.findViewById(R.id.profileNameRow),
            nameValue = root.findViewById(R.id.profileNameValue),
            layoutRow = root.findViewById(R.id.profileLayoutRow),
            layoutValue = root.findViewById(R.id.profileLayoutValue),
            languageRow = root.findViewById(R.id.profileLanguageRow),
            languageValue = root.findViewById(R.id.profileLanguageValue),
            leftKeysRow = root.findViewById(R.id.profileLeftKeysRow),
            leftKeysValue = root.findViewById(R.id.profileLeftKeysValue),
            rightKeysRow = root.findViewById(R.id.profileRightKeysRow),
            rightKeysValue = root.findViewById(R.id.profileRightKeysValue)
        )
    }

    private fun bindProfileActions(views: ProfileViews) {
        views.nameRow.setOnClickListener { showNameDialog(views.slot) }
        views.layoutRow.setOnClickListener { showLayoutDialog(views.slot) }
        views.languageRow.setOnClickListener { showLanguageDialog(views.slot) }
        views.leftKeysRow.setOnClickListener {
            showBottomKeyOptionsDialog(views.slot, isLeftSlot = true)
        }
        views.rightKeysRow.setOnClickListener {
            showBottomKeyOptionsDialog(views.slot, isLeftSlot = false)
        }
    }

    private fun showNameDialog(slot: LanguageProfileSlot) {
        val profile = loadProfile(slot)
        val input = EditText(this).apply {
            setText(profile.customName)
            hint = "Leave empty to use layout name"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Profile ${slot.name}: name")
            .setMessage("A blank name automatically follows the selected layout name.")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                KeyboardModeSettings.saveLanguageProfileName(
                    this,
                    slot,
                    input.text?.toString().orEmpty()
                )
                refreshProfiles()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLayoutDialog(slot: LanguageProfileSlot) {
        val installed = LayoutPackManager.listInstalled(this)
        if (installed.isEmpty()) return
        val profile = loadProfile(slot)
        val selected = installed.indexOfFirst { it.id == profile.layoutPackId }.coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Profile ${slot.name}: layout")
            .setSingleChoiceItems(
                installed.map(::formatLayoutPackLabel).toTypedArray(),
                selected
            ) { dialog, which ->
                KeyboardModeSettings.saveLanguageProfile(
                    this,
                    slot,
                    installed[which].id,
                    profile.languageMode
                )
                refreshProfiles()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLanguageDialog(slot: LanguageProfileSlot) {
        val modes = KeyboardLanguageMode.entries
        val profile = loadProfile(slot)
        AlertDialog.Builder(this)
            .setTitle("Profile ${slot.name}: autocorrect")
            .setSingleChoiceItems(
                arrayOf("French", "English", "French + English", "Disabled"),
                modes.indexOf(profile.languageMode)
            ) { dialog, which ->
                KeyboardModeSettings.saveLanguageProfile(
                    this,
                    slot,
                    profile.layoutPackId,
                    modes[which]
                )
                refreshProfiles()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBottomKeyOptionsDialog(
        slot: LanguageProfileSlot,
        isLeftSlot: Boolean
    ) {
        val allModes = BottomKeyMode.entries
        val options = allModes.flatMapIndexed { firstIndex, first ->
            allModes.drop(firstIndex + 1).map { second -> first to second }
        }
        val profile = loadProfile(slot)
        val currentSet = if (isLeftSlot) {
            profile.leftKeyModes.toSet()
        } else {
            profile.rightKeyModes.toSet()
        }
        val selected = options.indexOfFirst {
            setOf(it.first, it.second) == currentSet
        }.coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle(
                "Profile ${slot.name}: ${if (isLeftSlot) "left" else "right"} key"
            )
            .setSingleChoiceItems(
                options.map { formatBottomModePair(it.first, it.second) }.toTypedArray(),
                selected
            ) { dialog, which ->
                KeyboardModeSettings.saveLanguageProfileBottomSlotOptions(
                    this,
                    slot,
                    isLeftSlot,
                    options[which].first,
                    options[which].second
                )
                refreshProfiles()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshProfiles() {
        val (profileA, profileB) = KeyboardModeSettings.loadLanguageProfiles(this)
        val active = KeyboardModeSettings.loadActiveLanguageProfileSlot(this)
        renderProfile(profileAViews, profileA, active)
        renderProfile(profileBViews, profileB, active)
    }

    private fun renderProfile(
        views: ProfileViews,
        profile: LanguageProfile,
        active: LanguageProfileSlot
    ) {
        val pack = LayoutPackManager.listInstalled(this)
            .firstOrNull { it.id == profile.layoutPackId }
        val layoutName = pack?.displayName ?: profile.layoutPackId
        val displayName = resolveLanguageProfileDisplayName(profile.customName, layoutName)
        views.title.text = displayName
        views.activeBadge.visibility = if (views.slot == active) View.VISIBLE else View.GONE
        views.nameValue.text = if (profile.customName.isBlank()) {
            "Uses layout name"
        } else {
            profile.customName
        }
        views.layoutValue.text = layoutName
        views.languageValue.text = languageModeLabel(profile.languageMode)
        views.leftKeysValue.text =
            formatBottomModePair(profile.leftKeyModes[0], profile.leftKeyModes[1])
        views.rightKeysValue.text =
            formatBottomModePair(profile.rightKeyModes[0], profile.rightKeyModes[1])
    }

    private fun loadProfile(slot: LanguageProfileSlot): LanguageProfile {
        val profiles = KeyboardModeSettings.loadLanguageProfiles(this)
        return if (slot == LanguageProfileSlot.A) profiles.first else profiles.second
    }

    private fun formatLayoutPackLabel(pack: LayoutPack): String {
        val source = if (pack.source == LayoutPackSource.BUILTIN) "Built-in" else "Imported"
        val style = if (pack.isGboardStyle()) "Gboard" else "Classic"
        return "${pack.displayName} ($style, $source)"
    }

    private fun formatBottomModePair(first: BottomKeyMode, second: BottomKeyMode): String {
        return "${bottomModeLabel(first)} + ${bottomModeLabel(second)}"
    }

    private fun bottomModeLabel(mode: BottomKeyMode): String {
        return when (mode) {
            BottomKeyMode.AI -> "AI"
            BottomKeyMode.CLIPBOARD -> "Clipboard"
            BottomKeyMode.EMOJI -> "Emoji"
            BottomKeyMode.LANGUAGE -> "Language"
            BottomKeyMode.APOSTROPHE -> "Apostrophe"
        }
    }

    private fun languageModeLabel(mode: KeyboardLanguageMode): String {
        return when (mode) {
            KeyboardLanguageMode.FRENCH -> "French"
            KeyboardLanguageMode.ENGLISH -> "English"
            KeyboardLanguageMode.BOTH -> "French + English"
            KeyboardLanguageMode.DISABLED -> "Disabled"
        }
    }

    private fun applyStatusBarInset() {
        val content = findViewById<View>(R.id.profilesContent)
        val baseTop = content.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(
                view.paddingLeft,
                baseTop + topInset,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(content)
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

    private data class ProfileViews(
        val slot: LanguageProfileSlot,
        val title: TextView,
        val activeBadge: TextView,
        val nameRow: View,
        val nameValue: TextView,
        val layoutRow: View,
        val layoutValue: TextView,
        val languageRow: View,
        val languageValue: TextView,
        val leftKeysRow: View,
        val leftKeysValue: TextView,
        val rightKeysRow: View,
        val rightKeysValue: TextView
    )
}
