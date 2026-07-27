# Nboard

![Version](https://img.shields.io/badge/version-1.5.1-yellow)
![Android](https://img.shields.io/badge/android-8.0%2B%20(API%2026)-grey)
![License](https://img.shields.io/badge/license-AGPL--3.0-lightgrey)

<p align="center">
  <img src="docs/media/logo.png" alt="Nboard logo" width="96" height="96" />
</p>

<p align="center">
  Minimal Android keyboard with AI tools, clipboard power features, emoji search, and fast typing UX.
</p>

<p align="center">
  <a href="https://github.com/MathieuDvv/Nboard/releases">Download APK</a>
  ·
  <a href="https://github.com/MathieuDvv/Nboard/releases/latest">Download Latest Release</a>
  ·
  <a href="#build-locally">Build Locally</a>
  ·
  <a href="#features">Features</a>
  ·
  <a href="#contributing--feedback">Contributing</a>
</p>

## About

Built for Nothing Phone users who wanted a keyboard matching their device's minimal aesthetic — works great on any Android phone.

## Project Status

Nboard is **feature-complete** and returns to maintenance mode after the v1.5.0 community celebration release.

I built this keyboard to solve my own problem: wanting a Nothing-inspired keyboard with AI tools. It works great, and I use it daily.

I'll fix critical bugs, but I'm not committing to major new features. The project does what it's meant to do.

If you want additional features, fork it! It's open source (AGPL-3.0) for exactly this reason.

## Screenshots

| Keyboard | AI tools |
|---|---|
| ![Keyboard](docs/promo/keyboard.png) | ![AI tools](docs/promo/ai-tools.png) |

| Clipboard | Emoji |
|---|---|
| ![Clipboard](docs/promo/clipboard.png) | ![Emoji keyboard](docs/promo/emoji-keyboard.png) |

## Features

- AZERTY and QWERTY layouts with classic and Gboard variants
- Smart shift behavior (auto-capitalize, one-shot shift, caps lock)
- Smart typing assists (auto-space after sentence punctuation, auto-capitalize after punctuation, return to letters after `number + space`)
- Local autocorrect + prediction (French / English / Both / Disabled) with dictionary + n-gram scoring and on-device learning
- Long-press variants for letters and punctuation (`!`, `?`, `;`, accents)
- Gboard punctuation row improvements (`','`, `'.'`, and `'`) with adaptive left punctuation key (`/` in URL fields, `@` in email fields)
- Optional number row toggle in settings
- Spacebar cursor swipe
- Configurable key haptics (`Off`, `System`, `Light`, `Medium`, `Strong`) and press animations
- Two quick language profiles with custom names, a dedicated configuration page, per-profile special keys, and a configurable language-switch key
- Clipboard history with pin/delete + recent chip for text/images
- Emoji browser + search mode
- AI tools through Gemini, Anthropic, or preset/custom OpenAI-compatible providers (Summarize, Fix Grammar, Expand, free prompt with selected-text context support, language preserved by default)
- Contextual action icon based on input field type
- Theme options: `System`, `Light`, `Dark`, `AMOLED Black`
- Font options: `Inter` / `Roboto`
- Configurable side mode keys (AI / Clipboard / Emoji / Language / Apostrophe) on classic layouts
- Language and apostrophe actions for configurable side keys
- Gboard tool key behavior with press-and-hold quick access for AI / Clipboard / Emoji / Language / Apostrophe

## Beta Features

- Swipe Typing *(experimental)*
- Voice recognition (hold send) *(experimental)*
- Word prediction *(experimental)*

These features are currently in beta and may have occasional issues.

## Layout Pack Import (XML)

Nboard now supports importing custom keyboard layout packs from a local XML file in settings:

- Open `Settings` → `Layout packs`
- Tap `Import file`
- Select an XML file from storage

Expected XML shape:

```xml
<layout-pack id="community.qwerty.classic" name="Community QWERTY" bottomStyle="classic" qwertyLike="true">
  <row1>q w e r t y u i o p</row1>
  <row2>a s d f g h j k l</row2>
  <row3>z x c v b n m , '</row3>
  <variants>
    <key value="a">á à â ä</key>
    <key value="e">é è ê ë</key>
    <key value="'">’ ` ´</key>
  </variants>
</layout-pack>
```

- `bottomStyle`: `classic` or `gboard`
- `qwertyLike`: `true` or `false` (controls row shaping behavior)
- `variants` (optional): custom long-press options per key
- Variant format: `<key value="base">option1 option2 ...</key>`

Reference file: [`Community Layouts/layout_template.xml`](Community Layouts/layout_template.xml)

In the `Community Layouts/` folder, you will also find layouts contributed by the community (e.g., Arabic, German), as well as AI-generated layouts provided as examples (e.g., Hindi Inscript, Pinyin QWERTY).

## How Autocorrect and Prediction Work

### Autocorrect (local, on-device)

- Nboard loads French and English frequency dictionaries from local assets.
- When a typed word is unknown, it generates close candidates (1 edit away): deletion, swap, replacement, insertion.
- It ranks candidates by frequency first, then keeps the closest/shortest match.
- In bilingual mode, the previous word gives a lightweight language hint (French or English) to prioritize suggestions.
- A trie + in-memory cache are used to keep lookup speed fast while typing.

### Word prediction (local, on-device)

- Nboard uses unigram frequencies (single-word popularity) and bigram frequencies (next-word pairs).
- If there is a previous word, bigram candidates are preferred.
- If there is no strong previous-word match, it falls back to top unigram matches.
- In bilingual mode, French and English candidates are merged with simple frequency-based ranking.
- The prediction bar returns up to 3 suggestions.

## Language Support and APK Size

Nboard currently focuses on **French** and **English**.

- **French** is first-class because I am French, the app is used by French users, and it supports **AZERTY** workflows.
- **English** is included because it is the most widely used language and improves everyday compatibility.

The app is bigger now mainly because of new local dictionary assets used by autocorrect and prediction:

- `app/src/main/assets/dictionaries/english_50k.txt`
- `app/src/main/assets/dictionaries/french_50k.txt`
- `app/src/main/assets/dictionaries/english_bigrams.txt`
- `app/src/main/assets/dictionaries/french_bigrams.txt`

These files increase APK size, but they keep correction/prediction fast and on-device (no network call required for core typing intelligence).

## Support Development

If you want to support Nboard development:

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/dotslimy)

## Install APK (GitHub Releases)

1. Open [Releases](https://github.com/MathieuDvv/Nboard/releases).
2. Download the latest assets (`.apk` + source `.zip`).
3. Install on device:

```bash
adb install -r path/to/NBoard-v1.5.1-release.apk
```

4. On Android, enable **Nboard** in keyboard settings.
5. Select **Nboard** as your current keyboard.

Minimum Android version: **Android 8.0 (API 26)**.

## Build Locally

### Requirements

- Android Studio or Android SDK + JDK 17
- `adb` available in PATH
- Android 8.0+ target device/emulator (API 26+)

### Build

```bash
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

- `app/src/main/java/com/nboard/ime/NboardImeService.kt` — IME lifecycle + composition root
- `app/src/main/java/com/nboard/ime/NboardImeTextInput.kt` — key commit/delete pipeline, shift logic, smart typing integration
- `app/src/main/java/com/nboard/ime/NboardImeAutoCorrection.kt` — dictionary/variant correction flow
- `app/src/main/java/com/nboard/ime/NboardImeEmojiPrediction.kt` — emoji + prediction row rendering
- `app/src/main/java/com/nboard/ime/NboardImeBottomModes.kt` — bottom mode state and UI transitions
- `app/src/main/java/com/nboard/ime/NboardImeVoice.kt` — voice capture and transcript commit flow
- `app/src/main/java/com/nboard/ime/NboardImeClipboard.kt` — clipboard UI/history interactions
- `app/src/main/java/com/nboard/ime/AutoCorrect.kt` and `app/src/main/java/com/nboard/ime/BigramPredictor.kt` — local typing intelligence engines
- `app/src/main/java/com/nboard/ime/MainActivity.kt` — settings app
- `app/src/main/java/com/nboard/ime/OnboardingActivity.kt` — onboarding flow

## Developer Guide

Architecture and contributor onboarding are documented in [`DEVELOPER_GUIDE.md`](DEVELOPER_GUIDE.md).

## AI Provider Setup (Optional)

Nboard supports Gemini, Anthropic, and HTTPS OpenAI-compatible chat-completions providers. Choose the provider in `Settings` → `AI settings`.

For Gemini, choose a supported model and paste your Gemini API key in the app. Local builds may also provide a fallback key:

1. Copy `local.properties.example` to `local.properties`
2. Add your Gemini key:

```properties
GEMINI_API_KEY=YOUR_API_KEY_HERE
```

You can also set/update the key directly from the app settings.

For Anthropic, choose a Claude model and paste an Anthropic API key.

For an OpenAI-compatible provider:

- choose an endpoint preset for OpenAI, DeepSeek, Kimi, Groq, or OpenRouter;
- optionally adjust the preset model identifier;
- paste the API key issued by that service.

Choose `Custom` to enter a different HTTPS base URL and model identifier.

Nboard sends requests to `<base URL>/chat/completions`.

## Privacy & Security

- AI features require internet access and send prompt text to the provider selected in settings.
- Clipboard history is stored locally on device.
- No telemetry or usage tracking is implemented.
- Nboard is open source, so behavior is fully auditable.

## Future updates (Not planned yet)

- Password autofill (AutofillManager integration)
- GIF search

## Contributing & Feedback

- Contribution guidelines: [`CONTRIBUTING.md`](CONTRIBUTING.md)
- For bug fixes and feature PRs, read the scope and acceptance rules before starting work.
- Bug reports: [GitHub Issues](https://github.com/MathieuDvv/Nboard/issues)
- Feedback from real typing usage is very useful and helps prioritize improvements.

## Troubleshooting

- **Keyboard doesn't appear**
  - Open Android settings.
  - Go to keyboard/input method settings.
  - Enable **Nboard** and set it as active keyboard.
  - Re-open the target app input field.
- **AI features not working**
  - Confirm internet connection is available.
  - Verify the selected provider, model, URL, and API key.
  - Check the provider's quota or billing limits.
- **Swipe typing not working**
  - Swipe Typing is still beta.
  - Make sure it is enabled in settings.
  - Update to the latest release and retry.
- **App crashes**
  - Please report with steps and device info in [GitHub Issues](https://github.com/MathieuDvv/Nboard/issues).

## Notes

- Behavior depends on host app editor support.
- Image paste support depends on target input accepting rich content.
- Design is heavily inspired by Nothing and its aesthetic.

## License

Licensed under **AGPL-3.0** — see [`LICENSE`](LICENSE).

Free for personal use. For commercial licensing inquiries, contact: `mathieu.davinha83@gmail.com`.
