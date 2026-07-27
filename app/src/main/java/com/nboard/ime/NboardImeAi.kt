package com.nboard.ime

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import androidx.core.view.isVisible
import kotlinx.coroutines.launch

internal fun NboardImeService.syncAiProcessingAnimations() {
    val shouldAnimate = isGenerating && isAiMode && aiPromptRow.isVisible
    if (shouldAnimate) {
        startAiProcessingAnimations()
    } else {
        stopAiProcessingAnimations()
    }
}

internal fun NboardImeService.startAiProcessingAnimations() {
    if (!isAiPromptShimmerInitialized() || !isAiPromptInputInitialized()) {
        return
    }

    if (aiPillShimmerAnimator == null) {
        aiPromptShimmer.post {
            val stripWidth = aiPromptShimmer.width.toFloat().takeIf { it > 0f } ?: dp(84).toFloat()
            val travel = aiPromptRow.width.toFloat().takeIf { it > 0f } ?: return@post
            aiPromptShimmer.layoutParams = aiPromptShimmer.layoutParams.apply {
                height = aiPromptRow.height
            }
            aiPromptShimmer.isVisible = true
            aiPillShimmerAnimator?.cancel()
            aiPillShimmerAnimator = ValueAnimator.ofFloat(-stripWidth, travel + stripWidth).apply {
                duration = 1100L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                addUpdateListener { animator ->
                    aiPromptShimmer.translationX = animator.animatedValue as Float
                }
                start()
            }
        }
    }

    if (aiTextPulseAnimator == null) {
        val baseColor = uiColor(R.color.ai_text)
        val pulseColor = uiColor(R.color.ai_text_shine)
        aiTextPulseAnimator = ValueAnimator.ofObject(ArgbEvaluator(), baseColor, pulseColor, baseColor).apply {
            duration = 900L
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                aiPromptInput.setTextColor((animator.animatedValue as Int))
            }
            start()
        }
    }
}

internal fun NboardImeService.stopAiProcessingAnimations() {
    aiPillShimmerAnimator?.cancel()
    aiPillShimmerAnimator = null
    if (isAiPromptShimmerInitialized()) {
        aiPromptShimmer.isVisible = false
        aiPromptShimmer.translationX = 0f
    }

    aiTextPulseAnimator?.cancel()
    aiTextPulseAnimator = null
    if (isAiPromptInputInitialized()) {
        aiPromptInput.setTextColor(uiColor(R.color.ai_text))
    }
}

internal fun NboardImeService.animateAiResultText() {
    if (!isAiPromptInputInitialized()) {
        return
    }

    aiTextPulseAnimator?.cancel()
    aiTextPulseAnimator = null

    val baseColor = uiColor(R.color.ai_text)
    val flashColor = uiColor(R.color.ai_text_shine)
    ValueAnimator.ofObject(ArgbEvaluator(), flashColor, baseColor).apply {
        duration = 420L
        addUpdateListener { animator ->
            aiPromptInput.setTextColor((animator.animatedValue as Int))
        }
        start()
    }
}


internal fun NboardImeService.submitAiPrompt() {
    if (isGenerating) {
        return
    }
    if (!isAiAllowedInCurrentContext()) {
        return
    }

    val prompt = aiPromptInput.text?.toString()?.trim().orEmpty()
    if (prompt.isBlank()) {
        toast("Enter a prompt first")
        return
    }

    val selectedText = currentInputConnection
        ?.getSelectedText(0)
        ?.toString()
        ?.trim()
        .orEmpty()
    val resolvedPrompt = if (selectedText.isBlank()) {
        prompt
    } else {
        buildLanguagePreservingSelectionPrompt(
            instruction = prompt,
            selectedText = selectedText
        )
    }

    if (!textGenerationClient.isConfigured) {
        toast("Configure the selected AI provider in Nboard settings")
        return
    }

    aiPromptInput.error = null
    setGenerating(true)
    serviceScope.launch {
        val result = textGenerationClient.generateText(
            prompt = resolvedPrompt,
            systemInstruction = AI_PROMPT_SYSTEM_INSTRUCTION,
            outputCharLimit = AI_REPLY_CHAR_LIMIT
        )
        setGenerating(false)
        result
            .onSuccess { responseText ->
                val connection = currentInputConnection
                if (connection != null) {
                    connection.commitText(responseText, 1)
                } else {
                    aiPromptInput.error = "No text field focused"
                }
                aiPromptInput.text?.clear()
                animateAiResultText()
            }
            .onFailure { error ->
                val message = error.message ?: "AI request failed"
                aiPromptInput.error = message
                toast(message)
            }
    }
}

internal fun NboardImeService.runQuickAiAction(action: QuickAiAction) {
    if (isGenerating) {
        return
    }
    if (!isAiAllowedInCurrentContext()) {
        return
    }

    if (!textGenerationClient.isConfigured) {
        toast("Configure the selected AI provider in Nboard settings")
        return
    }

    val sourceText = currentInputConnection
        ?.getSelectedText(0)
        ?.toString()
        ?.trim()
        .orEmpty()

    if (sourceText.isBlank()) {
        toast("Select text first")
        return
    }

    aiPromptInput.error = null
    setGenerating(true)
    serviceScope.launch {
        val prompt = when (action) {
            QuickAiAction.SUMMARIZE -> buildLanguagePreservingSelectionPrompt(
                instruction = "Summarize this text.",
                selectedText = sourceText
            )
            QuickAiAction.FIX_GRAMMAR -> buildLanguagePreservingSelectionPrompt(
                instruction = "Fix grammar and spelling while keeping the same meaning.",
                selectedText = sourceText
            )
            QuickAiAction.EXPAND -> buildLanguagePreservingSelectionPrompt(
                instruction = "Expand this text with more detail while keeping the same meaning.",
                selectedText = sourceText
            )
        }

        val result = textGenerationClient.generateText(
            prompt = prompt,
            systemInstruction = AI_QUICK_ACTION_SYSTEM_INSTRUCTION,
            outputCharLimit = AI_PILL_CHAR_LIMIT
        )
        setGenerating(false)
        result
            .onSuccess { responseText ->
                aiPromptInput.setText(responseText)
                aiPromptInput.setSelection(aiPromptInput.text?.length ?: 0)
                animateAiResultText()

                val inputConnection = currentInputConnection
                inputConnection?.commitText(responseText, 1)
            }
            .onFailure { error ->
                val message = error.message ?: "AI request failed"
                aiPromptInput.error = message
                toast(message)
            }
    }
}

internal fun NboardImeService.buildLanguagePreservingSelectionPrompt(
    instruction: String,
    selectedText: String
): String {
    return buildString {
        append("Apply this instruction to the selected text and return only the transformed result.\n")
        append("Keep the output in the same language as the selected text.\n")
        append("Do not translate unless the instruction explicitly asks for translation.\n")
        append("Instruction: ")
        append(instruction.trim())
        append("\n\nSelected text:\n")
        append(selectedText)
    }
}
