package com.example.whabotpro.ai

import com.example.whabotpro.data.store.DataRepository
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Google Gemini AI client — used as a fallback / auto-configured option.
 */
class GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMedia = "application/json".toMediaType()

    val isReady: Boolean get() = DataRepository.settings.value.geminiApiKey.isNotEmpty()

    val modelName: String get() = DataRepository.settings.value.geminiModel
    private val model: String get() = DataRepository.settings.value.geminiModel
    private val apiKey: String get() = DataRepository.settings.value.geminiApiKey

    suspend fun chatJson(prompt: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) throw IllegalStateException("Gemini API key not set")
        val body = buildBody(prompt, responseSchema = true)
        val response = doRequest(body.toString())
        extractContent(response)
    }

    suspend fun chatReply(prompt: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) throw IllegalStateException("Gemini API key not set")
        val body = buildBody(prompt)
        val response = doRequest(body.toString())
        extractContent(response)
    }

    suspend fun chatReplyWithHistory(messages: List<Map<String, String>>): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) throw IllegalStateException("Gemini API key not set")
        val body = buildBodyWithHistory(messages)
        val response = doRequest(body.toString())
        extractContent(response)
    }

    /// chatLongReply — for raw data processing, needs high token limit
    suspend fun chatLongReply(prompt: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) throw IllegalStateException("Gemini API key not set")
        val maxTokens = 8000
        DataRepository.log("info", "GeminiClient: Requesting with maxOutputTokens=$maxTokens, prompt length=${prompt.length}")
        val body = buildBody(prompt, maxTokens = maxTokens)
        val response = doRequest(body.toString())
        val content = extractContent(response)
        DataRepository.log("info", "GeminiClient: Response received, length=${content.length}, hasClosingBracket=${content.contains("]")}, hasOpeningBracket=${content.contains("[")}")
        content
    }

    private fun buildBody(prompt: String, responseSchema: Boolean = false, maxTokens: Int = 0): JsonObject {
        val tokens = if (maxTokens > 0) maxTokens else if (responseSchema) 150 else 250
        return JsonObject().apply {
            add("contents", JsonArray().apply {
                add(JsonObject().apply {
                    add("parts", JsonArray().apply {
                        add(JsonObject().apply { addProperty("text", prompt) })
                    })
                })
            })
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", if (responseSchema) 0.05 else 0.4)
                addProperty("maxOutputTokens", tokens)
                if (responseSchema) {
                    add("responseMimeType", gson.toJsonTree("application/json"))
                }
            })
        }
    }

    private fun buildBodyWithHistory(messages: List<Map<String, String>>): JsonObject {
        return JsonObject().apply {
            add("contents", JsonArray().apply {
                messages.forEach { msg ->
                    add(JsonObject().apply {
                        addProperty("role", if (msg["role"] == "assistant") "model" else "user")
                        add("parts", JsonArray().apply {
                            add(JsonObject().apply { addProperty("text", msg["content"] ?: "") })
                        })
                    })
                }
            })
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", 0.4)
                addProperty("maxOutputTokens", 250)
            })
        }
    }

    private fun doRequest(body: String): String {
        val url = "$BASE_URL/$model:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(body.toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { res ->
            val text = res.body?.string() ?: ""
            DataRepository.log("info", "GeminiClient: HTTP ${res.code}, raw response length=${text.length}")
            if (!res.isSuccessful) {
                val errorMsg = "Gemini HTTP ${res.code}: ${text.take(200)}"
                DataRepository.log("error", "GeminiClient: $errorMsg")
                if (res.code == 429) {
                    throw RateLimitException("Gemini rate limit exceeded (HTTP 429)")
                }
                throw RuntimeException(errorMsg)
            }
            return text
        }
    }

    class RateLimitException(message: String) : Exception(message)

    private fun extractContent(json: String): String {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val candidates = root.getAsJsonArray("candidates") ?: return ""
            if (candidates.size() == 0) return ""
            val candidate = candidates[0].asJsonObject
            
            // Check finishReason to detect truncation
            val finishReason = candidate.get("finishReason")?.asString
            if (finishReason != null) {
                DataRepository.log("info", "GeminiClient: finishReason=$finishReason")
            }
            
            val content = candidate.getAsJsonObject("content") ?: return ""
            val parts = content.getAsJsonArray("parts")
            if (parts == null || parts.size() == 0) return ""
            parts[0].asJsonObject?.get("text")?.asString?.trim() ?: ""
        } catch (e: Exception) {
            android.util.Log.e("GeminiClient", "JSON parse error: ${e.message}")
            DataRepository.log("error", "GeminiClient: JSON parse error: ${e.message}")
            ""
        }
    }

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }
}
