package com.example.whabotpro.ai

import com.example.whabotpro.data.model.AppSettings
import com.example.whabotpro.data.store.DataRepository
import com.google.gson.Gson
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
 * Groq Cloud AI client — OpenAI-compatible API.
 * Used for primary AI replies (fast, ~560 tokens/sec).
 */
class GroqClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMedia = "application/json".toMediaType()

    val isReady: Boolean get() = DataRepository.settings.value.groqApiKey.isNotEmpty()

    val model: String get() = DataRepository.settings.value.groqModel

    /// chatJson — structured JSON output for classification (Round 1)
    suspend fun chatJson(prompt: String): String = withContext(Dispatchers.IO) {
        val settings = DataRepository.settings.value
        if (settings.groqApiKey.isEmpty()) throw IllegalStateException("Groq API key not set")

        val payload = JsonObject().apply {
            addProperty("model", settings.groqModel)
            add("messages", gson.toJsonTree(listOf(mapOf("role" to "user", "content" to prompt))))
            addProperty("temperature", 0.05)
            addProperty("max_tokens", 150)
            add("response_format", gson.toJsonTree(mapOf("type" to "json_object")))
        }

        val response = doRequest(payload.toString())
        extractContent(response)
    }

    /// chatReply — natural language reply for the customer (Round 2)
    suspend fun chatReply(prompt: String): String = withContext(Dispatchers.IO) {
        val settings = DataRepository.settings.value
        if (settings.groqApiKey.isEmpty()) throw IllegalStateException("Groq API key not set")

        val payload = JsonObject().apply {
            addProperty("model", settings.groqModel)
            add("messages", gson.toJsonTree(listOf(mapOf("role" to "user", "content" to prompt))))
            addProperty("temperature", 0.4)
            addProperty("max_tokens", 250)
        }

        val response = doRequest(payload.toString())
        extractContent(response)
    }

    /// chatReplyWithHistory — reply using conversation history
    suspend fun chatReplyWithHistory(messages: List<Map<String, String>>): String = withContext(Dispatchers.IO) {
        val settings = DataRepository.settings.value
        if (settings.groqApiKey.isEmpty()) throw IllegalStateException("Groq API key not set")

        val payload = JsonObject().apply {
            addProperty("model", settings.groqModel)
            add("messages", gson.toJsonTree(messages))
            addProperty("temperature", 0.4)
            addProperty("max_tokens", 250)
        }

        val response = doRequest(payload.toString())
        extractContent(response)
    }

    /// chatLongReply — for raw data processing, needs high token limit
    suspend fun chatLongReply(prompt: String): String = withContext(Dispatchers.IO) {
        val settings = DataRepository.settings.value
        if (settings.groqApiKey.isEmpty()) throw IllegalStateException("Groq API key not set")

        val payload = JsonObject().apply {
            addProperty("model", settings.groqModel)
            add("messages", gson.toJsonTree(listOf(mapOf("role" to "user", "content" to prompt))))
            addProperty("temperature", 0.1)
            addProperty("max_tokens", 2000)
        }

        val response = doRequest(payload.toString())
        extractContent(response)
    }

    private fun doRequest(body: String): String {
        val settings = DataRepository.settings.value
        val request = Request.Builder()
            .url(GROQ_URL)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${settings.groqApiKey}")
            .post(body.toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { res ->
            val text = res.body?.string() ?: ""
            if (!res.isSuccessful) {
                throw RuntimeException("Groq HTTP ${res.code}: ${text.take(200)}")
            }
            return text
        }
    }

    private fun extractContent(json: String): String {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val choices = root.getAsJsonArray("choices")
            if (choices == null || choices.size() == 0) return ""
            choices[0].asJsonObject
                .getAsJsonObject("message")
                ?.get("content")
                ?.asString
                ?.trim()
                ?: ""
        } catch (e: Exception) {
            android.util.Log.e("GroqClient", "JSON parse error: ${e.message}")
            ""
        }
    }

    companion object {
        private const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
    }
}
