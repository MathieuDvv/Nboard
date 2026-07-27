package com.nboard.ime

import com.nboard.ime.ai.GeminiClient
import com.nboard.ime.ai.AnthropicClient
import com.nboard.ime.ai.OpenAiCompatibleClient
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiClientTest {
    @Test
    fun compatibleProviderPresets_haveCompleteHttpsConfigurations() {
        OpenAiProviderPreset.entries
            .filter { it != OpenAiProviderPreset.CUSTOM }
            .forEach { preset ->
                assertTrue(preset.defaultBaseUrl.startsWith("https://"))
                assertTrue(preset.defaultModel.isNotBlank())
                assertEquals(
                    preset,
                    OpenAiProviderPreset.matchingBaseUrl(
                        "${preset.defaultBaseUrl}/"
                    )
                )
            }
    }

    @Test
    fun anthropic_sendsMessagesRequestAndParsesTextBlocks() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """{"content":[{"type":"text","text":"Improved text"}]}"""
            )
        )
        server.start()
        try {
            val client = AnthropicClient(
                apiKey = "anthropic-key",
                model = AnthropicModel.CLAUDE_SONNET_5.modelId,
                endpointUrl = server.url("/v1/messages").toString()
            )
            val result = client.generateText("Improve this", "Return only text", 100)

            assertEquals("Improved text", result.getOrThrow())
            val request = server.takeRequest()
            assertEquals("/v1/messages", request.path)
            assertEquals("anthropic-key", request.getHeader("x-api-key"))
            assertEquals("2023-06-01", request.getHeader("anthropic-version"))
            val body = JSONObject(request.body.readUtf8())
            assertEquals("claude-sonnet-5", body.getString("model"))
            assertEquals("Return only text", body.getString("system"))
            assertEquals(
                "Improve this",
                body.getJSONArray("messages").getJSONObject(0).getString("content")
            )
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun gemini_usesSelectedModelWithoutDeprecatedTemperature() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """{"candidates":[{"content":{"parts":[{"text":"Fixed text"}]}}]}"""
            )
        )
        server.start()
        try {
            val client = GeminiClient(
                apiKey = "gemini-key",
                model = GeminiModel.GEMINI_3_6_FLASH.modelId,
                endpointBaseUrl = server.url("/v1beta").toString()
            )
            val result = client.generateText("Fix me", "Return only text", 100)

            assertEquals("Fixed text", result.getOrThrow())
            val request = server.takeRequest()
            assertTrue(request.path.orEmpty().contains("/models/gemini-3.6-flash:generateContent"))
            assertTrue(request.path.orEmpty().contains("key=gemini-key"))
            val body = JSONObject(request.body.readUtf8())
            assertFalse(body.getJSONObject("generationConfig").has("temperature"))
            assertEquals("Fix me", body.getJSONArray("contents")
                .getJSONObject(0)
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun openAiCompatible_sendsChatCompletionAndParsesContent() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                """{"choices":[{"message":{"role":"assistant","content":"Summary"}}]}"""
            )
        )
        server.start()
        try {
            val client = OpenAiCompatibleClient(
                baseUrl = server.url("/v1").toString(),
                model = "custom-model",
                apiKey = "provider-key"
            )
            val result = client.generateText("Summarize", "Be concise", 100)

            assertEquals("Summary", result.getOrThrow())
            val request = server.takeRequest()
            assertEquals("/v1/chat/completions", request.path)
            assertEquals("Bearer provider-key", request.getHeader("Authorization"))
            val body = JSONObject(request.body.readUtf8())
            assertEquals("custom-model", body.getString("model"))
            assertEquals("system", body.getJSONArray("messages").getJSONObject(0).getString("role"))
            assertEquals("user", body.getJSONArray("messages").getJSONObject(1).getString("role"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun openAiCompatible_surfacesProviderError() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("""{"error":{"message":"Quota exceeded"}}""")
        )
        server.start()
        try {
            val client = OpenAiCompatibleClient(
                baseUrl = server.url("/v1").toString(),
                model = "custom-model",
                apiKey = "provider-key"
            )
            val result = client.generateText("Hello")

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Quota exceeded"))
        } finally {
            server.shutdown()
        }
    }
}
