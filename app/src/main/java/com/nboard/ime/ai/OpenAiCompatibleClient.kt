package com.nboard.ime.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenAiCompatibleClient(
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String,
    private val httpClient: OkHttpClient = defaultHttpClient()
) : TextGenerationClient {
    override val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank() && apiKey.isNotBlank()

    override suspend fun generateText(
        prompt: String,
        systemInstruction: String?,
        outputCharLimit: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(
                IllegalStateException("OpenAI-compatible provider settings are incomplete")
            )
        }

        val messages = JSONArray()
        if (!systemInstruction.isNullOrBlank()) {
            messages.put(
                JSONObject()
                    .put("role", "system")
                    .put("content", systemInstruction.trim())
            )
        }
        messages.put(
            JSONObject()
                .put("role", "user")
                .put("content", prompt)
        )

        val requestJson = JSONObject()
            .put("model", model.trim())
            .put("messages", messages)
            .put("max_tokens", 256)

        val request = Request.Builder()
            .url("${baseUrl.trim().trimEnd('/')}/chat/completions")
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = extractApiError(bodyString)
                        ?: "OpenAI-compatible request failed (${response.code})"
                    return@withContext Result.failure(IOException(detail))
                }
                if (bodyString.isBlank()) {
                    return@withContext Result.failure(IOException("AI provider returned an empty response"))
                }

                var text = runCatching {
                    JSONObject(bodyString)
                        .optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")
                        .orEmpty()
                        .trim()
                }.getOrDefault("")
                if (outputCharLimit > 0 && text.length > outputCharLimit) {
                    text = text.take(outputCharLimit).trimEnd().plus("…")
                }
                if (text.isBlank()) {
                    return@withContext Result.failure(IOException("AI provider response had no text output"))
                }
                Result.success(text)
            }
        } catch (error: Exception) {
            Result.failure(IOException(error.message ?: "AI provider request error", error))
        }
    }

    private fun extractApiError(body: String): String? {
        if (body.isBlank()) return null
        return runCatching {
            val error = JSONObject(body).opt("error")
            when (error) {
                is JSONObject -> error.optString("message").takeIf { it.isNotBlank() }
                is String -> error.takeIf { it.isNotBlank() }
                else -> null
            }
        }.getOrNull()
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        private fun defaultHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }
}
