package com.nboard.ime

import android.os.Build
import android.os.VibrationEffect
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.ViewCompat

data class HapticEffectSpec(
    val durationMs: Long,
    val amplitude: Int
)

internal fun hapticEffectSpec(mode: HapticMode): HapticEffectSpec? {
    return when (mode) {
        HapticMode.LIGHT -> HapticEffectSpec(6L, 60)
        HapticMode.MEDIUM -> HapticEffectSpec(10L, 120)
        HapticMode.STRONG -> HapticEffectSpec(16L, 200)
        HapticMode.OFF,
        HapticMode.SYSTEM -> null
    }
}

internal fun NboardImeService.bindPressAction(view: View, onTap: () -> Unit) {
    configureKeyTouch(view, repeatOnHold = false, longPressAction = null, tapOnDown = false, onTap = onTap)
}

internal fun NboardImeService.configureSpacebarTouch() {
    var lastRawX = 0f
    var downRawX = 0f
    var downRawY = 0f
    var accumulatedDx = 0f
    var movedCursor = false
    var cursorDragEnabled = false
    var cursorModeActive = false
    var baseAlpha = 1f
    val stepPx = dp(SPACEBAR_CURSOR_STEP_DP).toFloat()
    val deadzonePx = dp(SPACEBAR_CURSOR_DEADZONE_DP).toFloat()

    spaceButton.setOnTouchListener { touchedView, event ->
        if (!touchedView.isEnabled) {
            return@setOnTouchListener false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cursorDragEnabled = !isAiPromptInputActive() && !isEmojiSearchInputActive() && !isClipboardOpen
                cursorModeActive = false
                lastRawX = event.rawX
                downRawX = event.rawX
                downRawY = event.rawY
                accumulatedDx = 0f
                movedCursor = false
                baseAlpha = touchedView.alpha

                touchedView.isPressed = true
                touchedView.alpha = (baseAlpha * KEY_PRESSED_ALPHA).coerceAtLeast(MIN_PRESSED_ALPHA)
                touchedView.animate().cancel()
                touchedView.animate()
                    .scaleX(KEY_PRESS_SCALE)
                    .scaleY(KEY_PRESS_SCALE)
                    .setDuration(KEY_PRESS_ANIM_MS)
                    .start()
                performKeyHaptic(touchedView)
                true
            }

            MotionEvent.ACTION_MOVE -> {
                if (cursorDragEnabled) {
                    if (!cursorModeActive) {
                        val totalDx = event.rawX - downRawX
                        val totalDy = event.rawY - downRawY
                        if (kotlin.math.abs(totalDx) >= deadzonePx &&
                            kotlin.math.abs(totalDx) > kotlin.math.abs(totalDy)
                        ) {
                            cursorModeActive = true
                            lastRawX = event.rawX
                            accumulatedDx = 0f
                        }
                    } else {
                        val dx = event.rawX - lastRawX
                        lastRawX = event.rawX
                        accumulatedDx += dx

                        while (accumulatedDx >= stepPx) {
                            moveCursorRight()
                            accumulatedDx -= stepPx
                            movedCursor = true
                        }
                        while (accumulatedDx <= -stepPx) {
                            moveCursorLeft()
                            accumulatedDx += stepPx
                            movedCursor = true
                        }
                    }
                }
                true
            }

            MotionEvent.ACTION_UP -> {
                touchedView.isPressed = false
                touchedView.alpha = baseAlpha
                touchedView.animate().cancel()
                touchedView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(KEY_RELEASE_ANIM_MS)
                    .start()

                if (!movedCursor) {
                    handleSpaceTap()
                }
                true
            }

            MotionEvent.ACTION_CANCEL -> {
                touchedView.isPressed = false
                touchedView.alpha = baseAlpha
                touchedView.animate().cancel()
                touchedView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(KEY_RELEASE_ANIM_MS)
                    .start()
                true
            }

            else -> false
        }
    }
}

internal fun NboardImeService.handleSpaceTap() {
    if (isAiPromptInputActive()) {
        appendPromptText(" ")
    } else if (isEmojiSearchInputActive()) {
        appendEmojiSearchText(" ")
    } else {
        val inputConnection = currentInputConnection
        val charBeforeSpace = inputConnection
            ?.getTextBeforeCursor(1, 0)
            ?.toString()
            ?.lastOrNull()
        commitKeyText(" ")
        if (isNumbersMode &&
            charBeforeSpace?.isDigit() == true &&
            returnToLettersAfterNumberSpaceEnabled &&
            smartTypingBehavior.shouldReturnToLettersAfterNumberSpace()
        ) {
            isNumbersMode = false
            isSymbolsSubmenuOpen = false
            renderKeyRows()
            refreshUi()
        }
    }
}


internal fun NboardImeService.setIcon(button: ImageButton, drawableRes: Int, tintColorRes: Int) {
    val drawable = uiDrawable(drawableRes)?.mutate() ?: return
    val resolvedTint = if (drawableRes == R.drawable.ic_ai_custom) {
        R.color.ai_text
    } else {
        tintColorRes
    }
    drawable.setTint(uiColor(resolvedTint))
    button.setImageDrawable(drawable)
    button.scaleType = ImageView.ScaleType.CENTER_INSIDE
    button.setPadding(dp(8), dp(8), dp(8), dp(8))
}

internal fun NboardImeService.configureKeyTouch(
    view: View,
    repeatOnHold: Boolean,
    longPressAction: ((View, Float, Float) -> Unit)?,
    swipeToken: String? = null,
    tapOnDown: Boolean = true,
    onLongPressEnd: (() -> Unit)? = null,
    onTap: () -> Unit
) {
    var longPressTriggered = false
    var currentRawX = 0f
    var currentRawY = 0f
    var longPressStartX = 0f
    var longPressStartY = 0f
    var baseAlpha = 1f
    var repeatRunnable: Runnable? = null
    var longPressRunnable: Runnable? = null
    var swipeActiveForThisPointer = false

    view.setOnTouchListener { touchedView, event ->
        if (!touchedView.isEnabled) {
            return@setOnTouchListener false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                currentRawX = event.rawX
                currentRawY = event.rawY
                longPressStartX = event.rawX
                longPressStartY = event.rawY
                longPressTriggered = false
                baseAlpha = touchedView.alpha
                swipeActiveForThisPointer = swipeToken != null &&
                    shouldHandleSwipeTyping() &&
                    beginSwipeTyping(touchedView, swipeToken, event.rawX, event.rawY)

                touchedView.isPressed = true
                touchedView.alpha = (baseAlpha * KEY_PRESSED_ALPHA).coerceAtLeast(MIN_PRESSED_ALPHA)
                touchedView.animate().cancel()
                touchedView.animate()
                    .scaleX(KEY_PRESS_SCALE)
                    .scaleY(KEY_PRESS_SCALE)
                    .setDuration(KEY_PRESS_ANIM_MS)
                    .start()
                performKeyHaptic(touchedView)

                if (repeatOnHold) {
                    onTap()
                    repeatRunnable = object : Runnable {
                        override fun run() {
                            onTap()
                            touchedView.postDelayed(this, KEY_REPEAT_INTERVAL_MS)
                        }
                    }
                    touchedView.postDelayed(repeatRunnable!!, KEY_REPEAT_START_DELAY_MS)
                } else if (longPressAction != null) {
                    if (tapOnDown) {
                        onTap()
                    }
                    longPressRunnable = Runnable {
                        longPressTriggered = true
                        swipeActiveForThisPointer = false
                        cancelSwipeTyping()
                        longPressAction.invoke(touchedView, currentRawX, currentRawY)
                    }
                    val longPressDelay = minOf(
                        ViewConfiguration.getLongPressTimeout().toLong(),
                        VARIANT_LONG_PRESS_TIMEOUT_MS
                    )
                    touchedView.postDelayed(longPressRunnable!!, longPressDelay)
                } else if (tapOnDown) {
                    onTap()
                }
                true
            }

            MotionEvent.ACTION_MOVE -> {
                currentRawX = event.rawX
                currentRawY = event.rawY
                if (swipeActiveForThisPointer) {
                    val isSwipingNow = updateSwipeTyping(currentRawX, currentRawY)
                    if (isSwipingNow) {
                        longPressRunnable?.let { touchedView.removeCallbacks(it) }
                        longPressRunnable = null
                    }
                }
                if (longPressTriggered) {
                    val dx = currentRawX - longPressStartX
                    val dy = currentRawY - longPressStartY
                    val distance = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    if (distance < dp(HOLD_SELECTION_DEADZONE_DP).toFloat()) {
                        return@setOnTouchListener true
                    }
                    if (activeVariantSession != null) {
                        updateVariantSelection(currentRawX, currentRawY)
                    } else if (activeSwipePopupSession != null) {
                        updateSwipePopupSelection(currentRawX, currentRawY)
                    }
                }
                true
            }

            MotionEvent.ACTION_UP -> {
                touchedView.isPressed = false
                touchedView.alpha = baseAlpha
                touchedView.animate().cancel()
                touchedView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(KEY_RELEASE_ANIM_MS)
                    .start()

                repeatRunnable?.let { touchedView.removeCallbacks(it) }
                repeatRunnable = null
                longPressRunnable?.let { touchedView.removeCallbacks(it) }
                longPressRunnable = null

                if (swipeActiveForThisPointer && !longPressTriggered) {
                    swipeActiveForThisPointer = false
                    updateSwipeTyping(event.rawX, event.rawY)
                    if (finishSwipeTypingAndCommit()) {
                        return@setOnTouchListener true
                    }
                }

                if (longPressTriggered) {
                    onLongPressEnd?.invoke()
                    if (activeVariantSession != null) {
                        commitSelectedVariant()
                        return@setOnTouchListener true
                    }
                    if (activeSwipePopupSession != null) {
                        commitSelectedSwipePopup()
                        return@setOnTouchListener true
                    }
                }

                if (!repeatOnHold && !tapOnDown && !longPressTriggered) {
                    onTap()
                }
                true
            }

            MotionEvent.ACTION_CANCEL -> {
                touchedView.isPressed = false
                touchedView.alpha = baseAlpha
                touchedView.animate().cancel()
                touchedView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(KEY_RELEASE_ANIM_MS)
                    .start()

                repeatRunnable?.let { touchedView.removeCallbacks(it) }
                repeatRunnable = null
                longPressRunnable?.let { touchedView.removeCallbacks(it) }
                longPressRunnable = null
                if (swipeActiveForThisPointer) {
                    swipeActiveForThisPointer = false
                    cancelSwipeTyping()
                }
                if (longPressTriggered) {
                    onLongPressEnd?.invoke()
                    if (activeVariantSession != null) {
                        commitSelectedVariant()
                    } else if (activeSwipePopupSession != null) {
                        commitSelectedSwipePopup()
                    } else {
                        dismissActivePopup()
                    }
                }
                true
            }

            else -> false
        }
    }
}

internal fun NboardImeService.performKeyHaptic(view: View) {
    when (hapticMode) {
        HapticMode.OFF -> return
        HapticMode.SYSTEM -> {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            return
        }
        else -> Unit
    }

    val effect = hapticEffectSpec(hapticMode) ?: return
    try {
        val deviceVibrator = vibrator
        if (deviceVibrator?.hasVibrator() == true) {
            deviceVibrator.vibrate(
                VibrationEffect.createOneShot(effect.durationMs, effect.amplitude)
            )
        }
    } catch (_: Exception) {
        // Keep typing responsive.
    }
}

internal fun NboardImeService.addSpecialKey(
    row: LinearLayout,
    label: String?,
    iconRes: Int?,
    iconTintRes: Int,
    backgroundRes: Int,
    weight: Float,
    textSizeSp: Float = 15f,
    iconRotation: Float = 0f,
    repeatOnHold: Boolean = false,
    longPressAction: ((View, Float, Float) -> Unit)? = null,
    swipeToken: String? = null,
    tapOnDown: Boolean = true,
    keyHeightDp: Int = KEY_HEIGHT_DP,
    useSerifTypeface: Boolean = false,
    isLast: Boolean,
    onTap: () -> Unit
): View {
    val keyView: View = if (iconRes != null) {
        AppCompatImageButton(this).apply {
            background = uiDrawable(backgroundRes)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setIcon(this, iconRes, iconTintRes)
            applyReducedKeyIconOffset(this, backgroundRes == R.drawable.bg_mode_special_selected)
            rotation = iconRotation
            flattenView(this)
        }
    } else {
        AppCompatButton(this).apply {
            text = label.orEmpty()
            textSize = textSizeSp
            if (useSerifTypeface) {
                applySerifTypeface(this)
            } else {
                applyInterTypeface(this)
            }
            setTextColor(uiColor(R.color.key_text))
            background = uiDrawable(backgroundRes)
            setAllCaps(false)
            gravity = Gravity.CENTER
            flattenView(this)
        }
    }

    configureKeyTouch(
        view = keyView,
        repeatOnHold = repeatOnHold,
        longPressAction = longPressAction,
        swipeToken = swipeToken,
        tapOnDown = tapOnDown,
        onTap = onTap
    )

    val params = LinearLayout.LayoutParams(0, dp(keyHeightDp), weight)
    if (!isLast) {
        params.marginEnd = dp(KEY_HORIZONTAL_GAP_DP)
    }
    keyView.layoutParams = params
    row.addView(keyView)
    return keyView
}

internal fun NboardImeService.flattenView(view: View) {
    view.stateListAnimator = null
    view.elevation = 0f
    view.translationZ = 0f
    view.isSoundEffectsEnabled = false
    ViewCompat.setBackgroundTintList(view, null)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        view.foreground = null
    }
    if (view is TextView) {
        view.includeFontPadding = false
        view.setShadowLayer(0f, 0f, 0f, 0)
    }
}
