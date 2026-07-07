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
 * Mistral AI client — free tier fallback option
 */
class MistralClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMedia = "application/json".toMediaType()

    val isReady: Boolean get() = DataRepository.settings.value.mistralApiKey.isNotEmpty()

    val modelName: String get() = DataRepository.settings.value.mistralModel
    private val model: String get() = DataRepository.settings.value.mistralModel
    private val apiKey: String get() = DataRepository.settings.value.mistralApiKey

    suspend fun chatLongReply(prompt: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) throw IllegalStateException("Mistral API key not set")
        DataRepository.log("info", "MistralClient: Requesting with model=$model, prompt length=${prompt.length}")
        val body = buildBody(prompt)
        val response = doRequest(body.toString())
        val content = extractContent(response)
        DataRepository.log("info", "MistralClient: Response received, length=${content.length}, hasClosingBracket=${content.contains("]")}, hasOpeningBracket=${content.contains("[")}")
        content
    }

    private fun buildBody(prompt: String): JsonObject {
        return JsonObject().apply {
            add("model", gson.toJsonTree(model))
            add("messages", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", prompt)
                })
            })
            add("temperature", gson.toJsonTree(0.3))
            add("max_tokens", gson.toJsonTree(8000))
        }
    }

    private fun doRequest(body: String): String {
        val url = "https://api.mistral.ai/v1/chat/completions"
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .post(body.toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { res ->
            val text = res.body?.string() ?: ""
            DataRepository.log("info", "MistralClient: HTTP ${res.code}, raw response length=${text.length}")
            if (!res.isSuccessful) {
                val errorMsg = "Mistral HTTP ${res.code}: ${text.take(200)}"
                DataRepository.log("error", "MistralClient: $errorMsg")
                if (res.code == 429) {
                    throw RateLimitException("Mistral rate limit exceeded (HTTP 429)")
                }
                throw RuntimeException(errorMsg)
            }
            return text
        }
    }

    private fun extractContent(json: String): String {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            val choices = root.getAsJsonArray("choices") ?: return ""
            if (choices.size() == 0) return ""
            val choice = choices[0].asJsonObject
            val message = choice.getAsJsonObject("message") ?: return ""
            message.get("content")?.asString?.trim() ?: ""
        } catch (e: Exception) {
            android.util.Log.e("MistralClient", "JSON parse error: ${e.message}")
            DataRepository.log("error", "MistralClient: JSON parse error: ${e.message}")
            ""
        }
    }

    class RateLimitException(message: String) : Exception(message)
}
