package com.example.whabotpro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.whabotpro.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.util.UUID

// ── Business Info (single document) ──
@Entity(tableName = "business_info")
data class BusinessInfo(
    @PrimaryKey var id: String = "1",
    var brandName: String = "",
    var tagline: String = "",
    var category: String = "",
    var phone: String = "",
    var email: String = "",
    var website: String = "",
    var address: String = "",
    var cuisine: String = "",
    var openingHours: String = "",
    var businessDescription: String = "",
    var mission: String = "",
    var vision: String = "",
    var coreValues: String = "",
    var positioning: String = "",
    var targetAudience: String = "",
    var competitors: String = "",
    var wordsToUse: String = "",
    var wordsToAvoid: String = "",
    var brandVoice: String = "",
    var signatureStory: String = "",
    var originStory: String = "",
    var uniqueSellingProposition: String = "",
    var landmarks: String = "",
    var content: String = ""
)

// ── Generic KB item used by Menu, Services, FAQs, Deals, etc. ──
@Entity(tableName = "kb_items")
data class KbItem(
    @PrimaryKey @SerializedName("id") var id: String = UUID.randomUUID().toString(),
    @SerializedName("section") var section: String = "",
    @SerializedName("title") var title: String = "",
    @SerializedName("content") var content: String = "",
    @SerializedName("description") var description: String = "",
    @SerializedName("price") var price: String = "",
    @SerializedName("category") var category: String = "",
    @SerializedName("available") var available: Boolean = true,
    @SerializedName("stock") var stock: Int = 0,
    @SerializedName("active") var active: Boolean = true,
    @SerializedName("extra") var extra: String = "",
    @SerializedName("createdAt") var createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt") var updatedAt: Long = System.currentTimeMillis()
)

// ── Category ──
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var section: String = "",
    var name: String = "",
    var icon: String = ""
)

// ── Order ──
@Entity(tableName = "orders")
data class Order(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var orderNumber: String = "",
    var customerName: String = "",
    var customerPhone: String = "",
    var items: String = "",
    var totalAmount: String = "",
    var orderType: String = "delivery",
    var status: String = "pending",
    var address: String = "",
    var specialRequests: String = "",
    var createdAt: Long = System.currentTimeMillis()
)

// ── Inbox message ──
@Entity(tableName = "inbox")
data class InboxMessage(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var from: String = "",
    var contactName: String = "",
    var phoneNumber: String = "",
    var body: String = "",
    var direction: String = "in",
    var source: String = "ai",
    var timestamp: Long = System.currentTimeMillis()
)

// ── Contact ──
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var phoneNumber: String = "",
    var notes: String = "",
    var createdAt: Long = System.currentTimeMillis()
)

// ── Rule ──
@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var content: String = "",
    var active: Boolean = true
)

// ── Log entry ──
@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey var id: String = UUID.randomUUID().toString(),
    var level: String = "info",
    var message: String = "",
    var timestamp: Long = System.currentTimeMillis()
)

// ── App settings ──
@Entity(tableName = "settings")
data class AppSettings(
    @PrimaryKey var id: String = "1",
    // Provider 1: Groq (llama-3.1-8b-instant) - Fast, 6000 TPM limit, free tier
    var groqApiKey: String = BuildConfig.GROQ_API_KEY,
    var groqModel: String = "llama-3.1-8b-instant",
    // Provider 2: Google Gemini (gemini-2.5-flash) - Large context, rate limits on free tier
    var geminiApiKey: String = BuildConfig.GEMINI_API_KEY,
    var geminiModel: String = "gemini-2.5-flash",
    // Provider 3: Mistral AI (mistral-large-latest) - Good JSON support, free tier available
    // Key obtained from https://console.mistral.ai/ - free tier, no credit card needed
    var mistralApiKey: String = BuildConfig.MISTRAL_API_KEY,
    var mistralModel: String = "mistral-large-latest",
    // Provider 4: Hugging Face Inference - Free tier, many models available
    // Key obtained from https://huggingface.co/settings/tokens - free tier, no credit card needed
    var huggingfaceApiKey: String = BuildConfig.HUGGINGFACE_API_KEY,
    var huggingfaceModel: String = "Qwen/Qwen2.5-7B-Instruct",
    // Provider 5: OpenRouter (various) - Aggregates multiple models, some free options
    // Key obtained from https://openrouter.ai/ - free models available, no credit card needed
    var openrouterApiKey: String = BuildConfig.OPENROUTER_API_KEY,
    var openrouterModel: String = "openai/gpt-oss-20b:free",
    // Provider 6: Cohere (command-r) - Trial key free, rate-limited, good structured output
    // Key obtained from https://dashboard.cohere.com/ - free trial key, no credit card needed
    var cohereApiKey: String = BuildConfig.COHERE_API_KEY,
    var cohereModel: String = "command-r-08-2024",
    // Provider 7: Together AI (meta-llama) - Requires credit card, fast inference
    var togetherApiKey: String = "",
    var togetherModel: String = "meta-llama/Llama-3.2-3B-Instruct-Turbo",
    // Provider 8: NVIDIA NIM (various) - Free serverless APIs for development, no credit card needed
    // Key obtained from https://build.nvidia.com/ - free tier, no credit card needed
    var nvidiaApiKey: String = "",
    var nvidiaModel: String = "nvidia/llama-3.1-nemotron-70b-instruct",
    // Provider 9: Anthropic Claude (claude-3-haiku) - Requires credit card, excellent quality
    var anthropicApiKey: String = "",
    var anthropicModel: String = "claude-3-5-haiku-20241022",
    var autoReplyEnabled: Boolean = true,
    var businessName: String = "My Business",
    var bulkDelayMs: Long = 2000,
    var baileysServerUrl: String = "http://127.0.0.1:3001"
)

// ── WhatsApp connection state ──
enum class WaState {
    DISCONNECTED, CONNECTING, QR_READY, CODE_READY, CONNECTED, ERROR
}

// ── Connection status snapshot ──
data class StatusSnapshot(
    val waState: WaState = WaState.DISCONNECTED,
    val waUser: String = "",
    val aiReady: Boolean = false,
    val aiModel: String = "",
    val serverRunning: Boolean = false,
    val serverPort: Int = 0,
    val inboxCount: Int = 0
)

// ── AI agent result ──
data class AgentResult(
    val reply: String,
    val source: String
)

// ── Chat history entry for AI context ──
data class ChatHistoryEntry(
    val role: String,
    val content: String
)
