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

class AnthropicClient(
    private val apiKey: String,
    private val model: String,
    private val endpointUrl: String = "https://api.anthropic.com/v1/messages",
    private val httpClient: OkHttpClient = defaultHttpClient()
) : TextGenerationClient {
    override val isConfigured: Boolean
        get() = apiKey.isNotBlank() && model.isNotBlank()

    override suspend fun generateText(
        prompt: String,
        systemInstruction: String?,
        outputCharLimit: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(
                IllegalStateException("Anthropic provider settings are incomplete")
            )
        }

        val requestJson = JSONObject()
            .put("model", model.trim())
            .put("max_tokens", 256)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", prompt)
                )
            )
        if (!systemInstruction.isNullOrBlank()) {
            requestJson.put("system", systemInstruction.trim())
        }

        val request = Request.Builder()
            .url(endpointUrl)
            .header("x-api-key", apiKey.trim())
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = extractApiError(bodyString)
                        ?: "Anthropic request failed (${response.code})"
                    return@withContext Result.failure(IOException(detail))
                }
                if (bodyString.isBlank()) {
                    return@withContext Result.failure(
                        IOException("Anthropic returned an empty response")
                    )
                }

                var text = runCatching {
                    val content = JSONObject(bodyString).optJSONArray("content") ?: JSONArray()
                    buildString {
                        for (index in 0 until content.length()) {
                            val block = content.optJSONObject(index) ?: continue
                            if (block.optString("type") == "text") {
                                append(block.optString("text"))
                            }
                        }
                    }.trim()
                }.getOrDefault("")
                if (outputCharLimit > 0 && text.length > outputCharLimit) {
                    text = text.take(outputCharLimit).trimEnd().plus("…")
                }
                if (text.isBlank()) {
                    return@withContext Result.failure(
                        IOException("Anthropic response had no text output")
                    )
                }
                Result.success(text)
            }
        } catch (error: Exception) {
            Result.failure(IOException(error.message ?: "Anthropic request error", error))
        }
    }

    private fun extractApiError(body: String): String? {
        if (body.isBlank()) return null
        return runCatching {
            JSONObject(body)
                .optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }
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
