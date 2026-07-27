# Changelog

All notable changes to this project are documented in this file.

## [1.5.1] - 2026-07-28

### Added
- Added a dedicated language-profile settings page for configuring both profiles in one place.
- Added optional custom profile names, with the selected layout name used automatically when blank.
- Added per-profile left/right special-key mappings and active modes, restored automatically when switching profiles.
- Added Anthropic with Claude Sonnet 5, Claude Haiku 4.5, and Claude Opus 5 model choices.
- Added OpenAI-compatible service presets for OpenAI, DeepSeek, Kimi, Groq, and OpenRouter, while retaining a custom endpoint option.

### Changed
- Fused AI provider, model, and configuration rows into one settings card.
- Replaced token-only swipe decoding with keyboard-aware path geometry, endpoint scoring, and language-model reranking, while retaining the previous decoder as a fallback.
- Swipe paths are now sampled independently from the optional visual trail, preventing fast gestures and words beginning with `H` from silently producing no result.

## [1.5.0] - 2026-07-28

### Added
- Added Gemini and custom HTTPS OpenAI-compatible AI providers with separate saved configurations.
- Added a Gemini model picker with Gemini 3.6 Flash as the default, plus Gemini 3.5 Flash and Gemini 3.5 Flash-Lite.
- Added two quick language profiles that pair a layout pack with an autocorrect language.
- Added Language and Apostrophe actions to the configurable special keys.
- Added `Off`, `System`, `Light`, `Medium`, and `Strong` key-vibration settings.

### Fixed
- Removed dead touch zones between keys by routing gap touches to the nearest key ([#9](https://github.com/MathieuDvv/Nboard/issues/9)).
- Improved fast swipe gesture sampling so intermediate keys are not lost, including the reported `h`/`hui` failure ([#11](https://github.com/MathieuDvv/Nboard/issues/11)).
- Made keyboard vibration user-configurable instead of forcing feedback ([#6](https://github.com/MathieuDvv/Nboard/issues/6)).

### Notes
- Thank you to the Nboard community for reaching 100 GitHub stars.

## [1.4.1] - 2026-03-02

### Added
- Added `Community Layouts/` folder explicitly linked within the app settings via the "More layout here" button.
- Added contributor layouts: `arabic-qwerty-gboard`, `german-qwertz-gboard`.
- Added example AI layouts: `hindi-inscript-classic`, `japanese-romaji-gboard`, `chinese-pinyin-gboard`.

### Changed
- Improved settings iconography by replacing custom placeholders with official Lucide SVGs (`rotate-ccw`, `coffee`).
- Polished app branding icons across the launcher and onboarding UI.

### Notes
- Thank you for the incredible community support and hitting 50 stars on GitHub!

## [1.4.0] - 2026-02-16

### Added

- Added community layout pack support through local XML import in settings.
- Added optional per-key long-press variant configuration in layout XML (`<variants><key value="...">...</key></variants>`).
- Added a reusable `layout_template.xml` at the project root for easier community layout creation.
- Added `Community Layouts/` folder and contribution guidance for submitting layout files via PR.
- Added an AMOLED Black built-in theme mode.

### Changed

- Renamed built-in layout labels to: `Azerty (legacy)`, `Azerty (gboard)`, `Qwerty (legacy)`, `Qwerty (gboard)`.
- Removed the debug-only layout export option from settings.
- Refined docs for layout packs across README, docs example, and contributing guidelines.
- Updated app-wide theming to use theme tokens/attributes so AMOLED and future themes apply consistently across settings, onboarding, and keyboard UI.

### Notes

- With this implementation, major feature updates are expected to slow down. Next releases will primarily focus on maintenance and bug fixing while development attention shifts to other projects.

## [1.3.0] - 2026-02-15

### Added

- Added Smart Typing assists: auto-space after sentence punctuation, auto-capitalize for the next letter, and auto-return from number layout to letters after `number + space`.
- Added stricter context-aware typing behavior gates using `EditorInfo.inputType` to disable assists in email, URL, password, username, number-only, and phone fields.
- Added upgraded local typing intelligence with stronger dictionary and n-gram (bigram/trigram) handling for prediction and autocorrect quality.
- Added a root developer architecture guide for contributors.

### Changed

- Refactored the IME implementation into focused modules (`NboardImeTextInput`, `NboardImeBottomModes`, `NboardImeAutoCorrection`, `NboardImeVoice`, `NboardImeClipboard`, `NboardImeEmojiPrediction`, etc.) instead of a large monolith.
- Reworked local autocorrect and prediction flow to improve readability, maintainability, and future extension points while keeping behavior on-device.
- Expanded automated test coverage across smart typing, autocorrect, prediction, trie logic, and layout mode behavior.

## [1.2.0] - 2026-02-14

### Added

- Added Gboard-style AZERTY and QWERTY layouts.
- Added Gboard punctuation keys (`','`, `'.'`, and `'`) with variants for `!`, `?`, and `;`.
- Added adaptive left punctuation key in Gboard layouts (`/` for URL fields, `@` for email fields).
- Added a settings toggle for the top number row.

### Changed

- Changed Gboard bottom-row proportions: narrower spacebar and balanced punctuation/tool key sizing.
- Changed Gboard tool access to a single key with hold menu for AI, Clipboard, and Emoji actions.
- Changed AI text transformation prompts to preserve the selected text language by default.

## [1.1.0] - 2026-02-13

### Added

- Added hold-to-talk voice recognition with live dictation and settings toggle.
- Added smoother, more precise swipe typing with an optional yellow trail.
- Added AI prompt selected-text context mode: prompt instructions can now apply directly to selected input text.
- Added voice recognition control to onboarding beta features.

### Changed

- Reorganized settings with a dedicated Beta Features section and conditional trail option visibility.
- Updated project roadmap to maintenance mode with future updates marked as not planned.

## [1.0.1] - 2026-02-13

### Fixed

- Fixed a keyboard crash when deleting items from clipboard history.
- Fixed emoji picker search insertion so selected emoji is committed to the app input field.
- Fixed prompt/search focus routing so typing can return to the host app text input instead of staying stuck in the inline pill.
- Added a `Disabled` mode for basic autocorrect in settings.

## [1.0.0] - 2026-02-13

### Added

- Initial public release of Nboard.
- AZERTY/QWERTY keyboard layouts.
- Local autocorrect for French/English/Both.
- AI tools (Summarize, Fix Grammar, Expand, free prompt).
- Clipboard history with pin/delete and recent clipboard chip.
- Emoji browser and emoji search mode.
- Onboarding flow and in-app settings.
- Theme modes: System, Light, Dark, Dark (Classic).
- Optional beta features: Word Prediction and Swipe Typing.

### Notes

- Word Prediction and Swipe Typing are marked beta in v1.0.0.
