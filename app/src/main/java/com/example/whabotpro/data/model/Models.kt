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
    var mistralApiKey: String = "",
    var mistralModel: String = "mistral-large-latest",
    // Provider 4: Hugging Face Inference - Free tier, many models available
    var huggingfaceApiKey: String = "",
    var huggingfaceModel: String = "meta-llama/Llama-3.2-3B-Instruct",
    // Provider 5: DeepSeek (deepseek-chat) - Chinese AI, free tier, good for general tasks
    var deepseekApiKey: String = "",
    var deepseekModel: String = "deepseek-chat",
    // Provider 6: OpenRouter (various) - Aggregates multiple models, some free options
    var openrouterApiKey: String = "",
    var openrouterModel: String = "meta-llama/llama-3.2-3b-instruct:free",
    // Provider 7: Cohere (command-r) - Requires credit card, good structured output
    var cohereApiKey: String = "",
    var cohereModel: String = "command-r",
    // Provider 8: Together AI (meta-llama) - Requires credit card, fast inference
    var togetherApiKey: String = "",
    var togetherModel: String = "meta-llama/Llama-3.2-3B-Instruct-Turbo",
    // Provider 9: Replicate (llama-3) - Requires credit card, serverless inference
    var replicateApiKey: String = "",
    var replicateModel: String = "meta/meta-llama-3-70b-instruct",
    // Provider 10: Anthropic Claude (claude-3-haiku) - Requires credit card, excellent quality
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
