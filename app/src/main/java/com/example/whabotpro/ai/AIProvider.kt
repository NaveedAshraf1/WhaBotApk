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
 * Unified AI Provider with 10 fallback options
 * 
 * Provider List:
 * 1. Groq (llama-3.1-8b-instant) - Fast, 6000 TPM limit, free tier
 * 2. Google Gemini (gemini-2.5-flash) - Large context, rate limits on free tier
 * 3. Mistral AI (mistral-large-latest) - Good JSON support, free tier available
 * 4. Hugging Face Inference - Free tier, many models available
 * 5. DeepSeek (deepseek-chat) - Chinese AI, free tier, good for general tasks
 * 6. OpenRouter (various) - Aggregates multiple models, some free options
 * 7. Cohere (command-r) - Requires credit card, good structured output
 * 8. Together AI (meta-llama) - Requires credit card, fast inference
 * 9. Replicate (llama-3) - Requires credit card, serverless inference
 * 10. Anthropic Claude (claude-3-haiku) - Requires credit card, excellent quality
 */

sealed class AIProvider(val name: String, val requiresCreditCard: Boolean = false) {
    object GROQ : AIProvider("Groq", false)
    object GEMINI : AIProvider("Gemini", false)
    object MISTRAL : AIProvider("Mistral", false)
    object HUGGINGFACE : AIProvider("HuggingFace", false)
    object DEEPSEEK : AIProvider("DeepSeek", false)
    object OPENROUTER : AIProvider("OpenRouter", false)
    object COHERE : AIProvider("Cohere", true)
    object TOGETHER : AIProvider("Together", true)
    object REPLICATE : AIProvider("Replicate", true)
    object ANTHROPIC : AIProvider("Anthropic", true)
}

class UnifiedAIProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMedia = "application/json".toMediaType()

    private val settings = DataRepository.settings.value

    /**
     * Get available providers in priority order
     */
    private fun getProviders(): List<AIProvider> {
        return listOf(
            AIProvider.GROQ,
            AIProvider.GEMINI,
            AIProvider.MISTRAL,
            AIProvider.HUGGINGFACE,
            AIProvider.DEEPSEEK,
            AIProvider.OPENROUTER,
            AIProvider.COHERE,
            AIProvider.TOGETHER,
            AIProvider.REPLICATE,
            AIProvider.ANTHROPIC
        )
    }

    /**
     * Check if provider has API key configured
     */
    private fun isProviderReady(provider: AIProvider): Boolean {
        return when (provider) {
            AIProvider.GROQ -> settings.groqApiKey.isNotEmpty()
            AIProvider.GEMINI -> settings.geminiApiKey.isNotEmpty()
            AIProvider.MISTRAL -> settings.mistralApiKey.isNotEmpty()
            AIProvider.HUGGINGFACE -> settings.huggingfaceApiKey.isNotEmpty()
            AIProvider.DEEPSEEK -> settings.deepseekApiKey.isNotEmpty()
            AIProvider.OPENROUTER -> settings.openrouterApiKey.isNotEmpty()
            AIProvider.COHERE -> settings.cohereApiKey.isNotEmpty()
            AIProvider.TOGETHER -> settings.togetherApiKey.isNotEmpty()
            AIProvider.REPLICATE -> settings.replicateApiKey.isNotEmpty()
            AIProvider.ANTHROPIC -> settings.anthropicApiKey.isNotEmpty()
        }
    }

    /**
     * Get model name for provider
     */
    private fun getModel(provider: AIProvider): String {
        return when (provider) {
            AIProvider.GROQ -> settings.groqModel
            AIProvider.GEMINI -> settings.geminiModel
            AIProvider.MISTRAL -> settings.mistralModel
            AIProvider.HUGGINGFACE -> settings.huggingfaceModel
            AIProvider.DEEPSEEK -> settings.deepseekModel
            AIProvider.OPENROUTER -> settings.openrouterModel
            AIProvider.COHERE -> settings.cohereModel
            AIProvider.TOGETHER -> settings.togetherModel
            AIProvider.REPLICATE -> settings.replicateModel
            AIProvider.ANTHROPIC -> settings.anthropicModel
        }
    }

    /**
     * Get API key for provider
     */
    private fun getApiKey(provider: AIProvider): String {
        return when (provider) {
            AIProvider.GROQ -> settings.groqApiKey
            AIProvider.GEMINI -> settings.geminiApiKey
            AIProvider.MISTRAL -> settings.mistralApiKey
            AIProvider.HUGGINGFACE -> settings.huggingfaceApiKey
            AIProvider.DEEPSEEK -> settings.deepseekApiKey
            AIProvider.OPENROUTER -> settings.openrouterApiKey
            AIProvider.COHERE -> settings.cohereApiKey
            AIProvider.TOGETHER -> settings.togetherApiKey
            AIProvider.REPLICATE -> settings.replicateApiKey
            AIProvider.ANTHROPIC -> settings.anthropicApiKey
        }
    }

    /**
     * Get API endpoint for provider
     */
    private fun getEndpoint(provider: AIProvider): String {
        return when (provider) {
            AIProvider.GROQ -> "https://api.groq.com/openai/v1/chat/completions"
            AIProvider.GEMINI -> "https://generativelanguage.googleapis.com/v1beta/models/${getModel(provider)}:generateContent?key=${getApiKey(provider)}"
            AIProvider.MISTRAL -> "https://api.mistral.ai/v1/chat/completions"
            AIProvider.HUGGINGFACE -> "https://api-inference.huggingface.co/models/${getModel(provider)}/v1/chat/completions"
            AIProvider.DEEPSEEK -> "https://api.deepseek.com/v1/chat/completions"
            AIProvider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            AIProvider.COHERE -> "https://api.cohere.ai/v1/chat"
            AIProvider.TOGETHER -> "https://api.together.xyz/v1/chat/completions"
            AIProvider.REPLICATE -> "https://api.replicate.com/v1/predictions"
            AIProvider.ANTHROPIC -> "https://api.anthropic.com/v1/messages"
        }
    }

    /**
     * Build request body for provider
     */
    private fun buildBody(provider: AIProvider, prompt: String): JsonObject {
        return when (provider) {
            AIProvider.GEMINI -> {
                // Gemini uses different format
                JsonObject().apply {
                    add("contents", JsonArray().apply {
                        add(JsonObject().apply {
                            add("parts", JsonArray().apply {
                                add(JsonObject().apply { addProperty("text", prompt) })
                            })
                        })
                    })
                    add("generationConfig", JsonObject().apply {
                        addProperty("temperature", 0.3)
                        addProperty("maxOutputTokens", 8000)
                    })
                }
            }
            AIProvider.ANTHROPIC -> {
                // Anthropic uses different format
                JsonObject().apply {
                    add("model", gson.toJsonTree(getModel(provider)))
                    add("max_tokens", gson.toJsonTree(8000))
                    add("messages", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("role", "user")
                            addProperty("content", prompt)
                        })
                    })
                }
            }
            else -> {
                // OpenAI-compatible format (Groq, Mistral, HuggingFace, DeepSeek, OpenRouter, Cohere, Together, Replicate)
                JsonObject().apply {
                    add("model", gson.toJsonTree(getModel(provider)))
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
        }
    }

    /**
     * Build request headers for provider
     */
    private fun buildHeaders(provider: AIProvider): Map<String, String> {
        return when (provider) {
            AIProvider.GEMINI -> mapOf("Content-Type" to "application/json")
            AIProvider.ANTHROPIC -> mapOf(
                "Content-Type" to "application/json",
                "x-api-key" to getApiKey(provider),
                "anthropic-version" to "2023-06-01"
            )
            else -> mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer ${getApiKey(provider)}"
            )
        }
    }

    /**
     * Extract content from response based on provider format
     */
    private fun extractContent(provider: AIProvider, json: String): String {
        return try {
            when (provider) {
                AIProvider.GEMINI -> {
                    val root = JsonParser.parseString(json).asJsonObject
                    val candidates = root.getAsJsonArray("candidates") ?: return ""
                    if (candidates.size() == 0) return ""
                    val content = candidates[0].asJsonObject.getAsJsonObject("content") ?: return ""
                    val parts = content.getAsJsonArray("parts")
                    if (parts == null || parts.size() == 0) return ""
                    parts[0].asJsonObject?.get("text")?.asString?.trim() ?: ""
                }
                AIProvider.ANTHROPIC -> {
                    val root = JsonParser.parseString(json).asJsonObject
                    val content = root.getAsJsonArray("content") ?: return ""
                    if (content.size() == 0) return ""
                    content[0].asJsonObject?.get("text")?.asString?.trim() ?: ""
                }
                else -> {
                    // OpenAI-compatible format
                    val root = JsonParser.parseString(json).asJsonObject
                    val choices = root.getAsJsonArray("choices") ?: return ""
                    if (choices.size() == 0) return ""
                    val choice = choices[0].asJsonObject
                    val message = choice.getAsJsonObject("message") ?: return ""
                    message.get("content")?.asString?.trim() ?: ""
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UnifiedAIProvider", "JSON parse error: ${e.message}")
            DataRepository.log("error", "UnifiedAIProvider: JSON parse error: ${e.message}")
            ""
        }
    }

    /**
     * Send request to provider with automatic fallback
     */
    suspend fun chatLongReply(prompt: String): AIResponse = withContext(Dispatchers.IO) {
        val providers = getProviders()
        
        for (provider in providers) {
            if (!isProviderReady(provider)) {
                DataRepository.log("info", "UnifiedAIProvider: ${provider.name} not ready, skipping")
                continue
            }

            try {
                DataRepository.log("info", "UnifiedAIProvider: Trying ${provider.name} with model ${getModel(provider)}")
                val body = buildBody(provider, prompt)
                val response = doRequest(provider, body.toString())
                val content = extractContent(provider, response)
                
                DataRepository.log("info", "UnifiedAIProvider: ${provider.name} success, response length=${content.length}")
                return@withContext AIResponse(content, provider.name, true)
            } catch (e: Exception) {
                DataRepository.log("error", "UnifiedAIProvider: ${provider.name} failed: ${e.message}")
                // Continue to next provider
            }
        }

        // All providers failed
        DataRepository.log("error", "UnifiedAIProvider: All providers failed")
        AIResponse("", "none", false)
    }

    /**
     * Execute HTTP request
     */
    private fun doRequest(provider: AIProvider, body: String): String {
        val url = getEndpoint(provider)
        val headers = buildHeaders(provider)
        
        val requestBuilder = Request.Builder()
            .url(url)
        
        headers.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }
        
        val request = requestBuilder
            .post(body.toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { res ->
            val text = res.body?.string() ?: ""
            DataRepository.log("info", "UnifiedAIProvider: ${provider.name} HTTP ${res.code}, response length=${text.length}")
            
            if (!res.isSuccessful) {
                val errorMsg = "${provider.name} HTTP ${res.code}: ${text.take(200)}"
                DataRepository.log("error", "UnifiedAIProvider: $errorMsg")
                if (res.code == 429) {
                    throw RateLimitException("${provider.name} rate limit exceeded")
                }
                throw RuntimeException(errorMsg)
            }
            return text
        }
    }

    data class AIResponse(
        val content: String,
        val provider: String,
        val success: Boolean
    )

    class RateLimitException(message: String) : Exception(message)
}
