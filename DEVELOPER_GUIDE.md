# Developer Guide (v1.3.0)

This guide explains the post-refactor architecture so contributors can add features without digging through one giant file.

## Quick Start

1. Build debug:
   ```bash
   ./gradlew assembleDebug
   ```
2. Run unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```
3. Install debug APK:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## High-Level Architecture

`NboardImeService` is the IME lifecycle owner and shared state container.
Most behavior is implemented in extension modules under `app/src/main/java/com/nboard/ime/`.

### Core Modules

- `NboardImeService.kt`
  - Lifecycle hooks (`onCreate`, `onStartInput`, `onCreateInputView`, etc.)
  - Shared mutable state and view references
  - Wiring between modules

- `NboardImeTextInput.kt`
  - Character commit/delete flow
  - Shift states (`OFF`, `ONE_SHOT`, `CAPS_LOCK`)
  - Smart typing integration and sentence handling

- `NboardImeAutoCorrection.kt`
  - Context extraction for autocorrect
  - Dictionary candidate generation/ranking
  - Correction undo integration

- `NboardImeLanguageEngine.kt`
  - Word normalization helpers
  - Learned frequency persistence and trimming
  - Tokenization and edit-distance helpers

- `NboardImeEmojiPrediction.kt`
  - Prediction row rendering and word suggestions
  - Emoji panel rendering/search

- `NboardImeBottomModes.kt`
  - AI/Clipboard/Emoji/Language/Apostrophe mode switching
  - Bottom-row UI state and visibility transitions

- `NboardImeClipboard.kt`
  - Clipboard history UI and recent chip behavior

- `NboardImeVoice.kt`
  - Voice input lifecycle and transcript commit behavior

- `NboardImeSwipeTyping.kt` and `NboardImeKeyTouch.kt`
  - Gesture/touch handling
  - Swipe session state and commit behavior

### Standalone Engines

- `AutoCorrect.kt`
  - Dictionary-trie based autocorrect engine
  - Bilingual mode and language hinting

- `BigramPredictor.kt`
  - Unigram + bigram ranking for next-word prediction
  - Bilingual merge logic

- `SmartTypingBehavior.kt`
  - `InputType`-aware decisions for:
    - auto-space/auto-capitalize
    - return-to-letters after number input

## Data and Settings

- `KeyboardModeSettings.kt`
  - Source of truth for persisted toggles and layout/language/theme settings
- `clipboard/ClipboardHistoryStore.kt`
  - Clipboard history persistence
- Shared prefs keys for learned prediction/autocorrect data are managed by `NboardImeLanguageEngine.kt`

## Typical Change Entry Points

### Add a new typing rule

- Decision logic: `SmartTypingBehavior.kt`
- Commit behavior integration: `NboardImeTextInput.kt`
- User toggle: `KeyboardModeSettings.kt` + settings UI in `MainActivity.kt`

### Improve autocorrect quality

- Engine logic: `AutoCorrect.kt`
- IME integration and context gating: `NboardImeAutoCorrection.kt`
- Shared normalization helpers: `NboardImeLanguageEngine.kt`

### Improve word prediction

- Engine logic: `BigramPredictor.kt`
- UI rendering and commit actions: `NboardImeEmojiPrediction.kt` + `NboardImeAutoCorrection.kt`

### Add a bottom mode action

- Mode state transitions: `NboardImeBottomModes.kt`
- Touch bindings: `NboardImeService.kt` / `NboardImeKeyTouch.kt`

## Testing Strategy

Unit tests are in `app/src/test/java/com/nboard/ime/`.

Prioritize fast JVM tests for:
- `SmartTypingBehavior`
- `AutoCorrect`
- `BigramPredictor`
- `DictionaryTrie`
- pure enum/helper behavior

Run all unit tests:

```bash
./gradlew testDebugUnitTest
```

## Performance Notes

Typing-path code should stay cheap (<5ms per key event in typical paths).
Avoid allocations in hot paths when possible.

Areas to keep lightweight:
- `commitKeyText` flow in `NboardImeTextInput.kt`
- candidate evaluation in `NboardImeAutoCorrection.kt`
- prediction generation in `BigramPredictor.kt`

## Privacy Model

- Core typing intelligence (autocorrect/prediction/smart typing): local/on-device.
- Remote calls only for optional AI prompt features (`TextGenerationClient`, with Gemini, Anthropic Messages, and OpenAI-compatible implementations) and voice services depending on platform recognizer behavior.

## Release Checklist

1. Bump `versionName` and `versionCode` in `app/build.gradle.kts`.
2. Update `CHANGELOG.md` and `README.md`.
3. Build + sign release APK.
4. Generate source ZIP.
5. Publish release notes and both assets.
