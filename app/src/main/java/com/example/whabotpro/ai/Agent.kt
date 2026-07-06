package com.example.whabotpro.ai

import com.example.whabotpro.data.model.*
import com.example.whabotpro.data.store.DataRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Two-Round AI Agent: Classify → Fetch → Answer
 *
 * Round 1: Ask the active LLM to classify the customer's intent and pick a tool + collection.
 * Round 2: Execute the tool (read from local store) → send result to the LLM → get answer.
 *
 * Uses Groq if configured, otherwise falls back to Gemini (auto-configured by default).
 */
class Agent(
    private val groq: GroqClient = GroqClient(),
    private val gemini: GeminiClient = GeminiClient()
) {

    private val gson = Gson()

    private val llmReady: Boolean get() = groq.isReady || gemini.isReady

    private suspend fun classify(prompt: String): String {
        return if (groq.isReady) groq.chatJson(prompt) else gemini.chatJson(prompt)
    }

    private suspend fun reply(prompt: String): String {
        return if (groq.isReady) groq.chatReply(prompt) else gemini.chatReply(prompt)
    }

    suspend fun run(
        message: String,
        customerName: String,
        customerNumber: String,
        chatHistory: List<ChatHistoryEntry> = emptyList()
    ): AgentResult {
        if (message.isBlank()) return AgentResult("", "none")
        if (!llmReady) return kbFallback(message)

        val meta = DataRepository.buildMeta()
        val historyBlock = buildHistoryBlock(chatHistory)
        val source = if (groq.isReady) "groq" else "gemini"

        return try {
            // Round 1: classify
            val classifyPrompt = buildClassifyPrompt(message, meta, historyBlock)
            val classifyJson = classify(classifyPrompt)
            val classification = parseClassification(classifyJson)

            // Round 2: execute tool + generate answer
            val toolResult = executeTool(classification, customerNumber, message)
            val answerPrompt = buildAnswerPrompt(
                message, customerName, toolResult, classification.intent,
                DataRepository.activeRules(), detectLanguage(message), historyBlock
            )
            val reply = reply(answerPrompt)
            AgentResult(reply, source)
        } catch (e: Exception) {
            DataRepository.log("error", "Agent error: ${e.message}")
            kbFallback(message)
        }
    }

    // ── Test chat (for the Test Chat screen) ──
    suspend fun testChat(
        message: String,
        history: List<ChatHistoryEntry>
    ): String {
        if (!llmReady) return "Set Groq or Gemini API key in Settings first."

        val meta = DataRepository.buildMeta()
        val historyBlock = buildHistoryBlock(history)

        val classifyPrompt = buildClassifyPrompt(message, meta, historyBlock)
        val classifyJson = classify(classifyPrompt)
        val classification = parseClassification(classifyJson)

        val toolResult = executeTool(classification, "", message)
        val answerPrompt = buildAnswerPrompt(
            message, "Test User", toolResult, classification.intent,
            DataRepository.activeRules(), detectLanguage(message), historyBlock
        )
        return reply(answerPrompt)
    }

    // ── Classification prompt (Round 1) ──
    private fun buildClassifyPrompt(message: String, meta: Map<String, Int>, historyBlock: String): String {
        val toolsBlock = buildToolsBlock(meta)
        val colNames = meta.keys.toList()
        val businessInfoCol = colNames.find { it.contains("business_info") } ?: "business_info"
        val menuCol = colNames.find { it.contains("menu") } ?: "menu"
        val servicesCol = colNames.find { it.contains("service") } ?: "services"
        val faqsCol = colNames.find { it.contains("faq") } ?: "faqs"

        return """You are a JSON classifier for a WhatsApp business assistant. Decide how to answer the customer message.

$toolsBlock

IMPORTANT: Use ONLY the exact tool names listed above. Do NOT invent tool names.

EXAMPLES:
Q: "What are your opening hours?"
A: {"intent":"database","tool":"get_business_info","args":{}}

Q: "What is your business name?"
A: {"intent":"database","tool":"get_business_info","args":{}}

Q: "Where are you located?"
A: {"intent":"database","tool":"get_business_info","args":{}}

Q: "What is in your menu?"
A: {"intent":"database","tool":"get_kb_items_by_section","args":{"section":"menu"}}

Q: "menu me kya ha?"
A: {"intent":"database","tool":"get_kb_items_by_section","args":{"section":"menu"}}

Q: "How much is chicken karahi?"
A: {"intent":"database","tool":"search_kb_items","args":{"section":"menu","field":"title","value":"chicken karahi"}}

Q: "Do you deliver?"
A: {"intent":"database","tool":"search_kb_items","args":{"section":"services","field":"title","value":"delivery"}}

Q: "Do you take reservations?"
A: {"intent":"database","tool":"get_kb_items_by_section","args":{"section":"faqs"}}

Q: "Where is my order?"
A: {"intent":"database","tool":"get_orders_by_phone","args":{"phoneNumber":"<customer_phone>"}}

Q: "What are your BBQ categories?"
A: {"intent":"database","tool":"get_categories_by_section","args":{"section":"menu"}}

Q: "Show me only available menu items"
A: {"intent":"database","tool":"get_available_items","args":{"section":"menu"}}

Q: "Hi, how are you?"
A: {"intent":"general"}

Q: "Tell me a joke"
A: {"intent":"general"}

${if (historyBlock.isNotEmpty()) "\nCONVERSATION HISTORY (last 10 messages, for context only):\n$historyBlock\n" else ""}Now classify this message. Output ONLY valid JSON using an exact tool name from the list:
Q: "$message"
A:"""
    }

    private fun buildToolsBlock(meta: Map<String, Int>): String {
        val sb = StringBuilder("AVAILABLE TOOLS:\n")
        sb.append("BUSINESS INFO:\n")
        sb.append("  - get_business_info: Get complete business information. args: {}\n\n")
        sb.append("KNOWLEDGE BASE (MENU, SERVICES, FAQs, etc.):\n")
        sb.append("  - get_kb_items_by_section: Get all items from a section. args: {\"section\":\"menu|services|faqs|promotions|policies|events|reservations|delivery_zones\"}\n")
        sb.append("  - get_kb_item_by_id: Get a specific item by ID. args: {\"id\":\"<item_id>\"}\n")
        sb.append("  - search_kb_items: Search items by field value. args: {\"section\":\"<section>\",\"field\":\"title|category|content\",\"value\":\"<search_term>\"}\n")
        sb.append("  - get_available_items: Get only available items from a section. args: {\"section\":\"<section>\"}\n\n")
        sb.append("CATEGORIES:\n")
        sb.append("  - get_categories_by_section: Get categories for a section. args: {\"section\":\"menu|services|faqs\"}\n")
        sb.append("  - get_all_categories: Get all categories. args: {}\n\n")
        sb.append("ORDERS:\n")
        sb.append("  - get_order_by_number: Get order by order number. args: {\"orderNumber\":\"<order_number>\"}\n")
        sb.append("  - get_orders_by_phone: Get all orders for a customer. args: {\"phoneNumber\":\"<phone>\"}\n")
        sb.append("  - get_pending_orders: Get all pending orders. args: {}\n")
        sb.append("  - get_orders_by_status: Get orders by status. args: {\"status\":\"pending|preparing|ready|completed|cancelled\"}\n\n")
        sb.append("CONTACTS:\n")
        sb.append("  - get_contact_by_phone: Get contact by phone number. args: {\"phoneNumber\":\"<phone>\"}\n")
        sb.append("  - get_all_contacts: Get all contacts. args: {}\n\n")
        sb.append("AI RULES:\n")
        sb.append("  - get_active_rules: Get all active AI rules. args: {}\n")
        sb.append("  - get_all_rules: Get all rules (active and inactive). args: {}\n\n")
        sb.append("SYSTEM LOGS:\n")
        sb.append("  - get_recent_logs: Get recent log entries. args: {\"level\":\"info|error|warning\",\"limit\":10}\n\n")
        sb.append("AVAILABLE SECTIONS:\n")
        for ((name, count) in meta) {
            sb.append("  - $name ($count items)\n")
        }
        return sb.toString()
    }

    // ── Answer prompt (Round 2) ──
    private fun buildAnswerPrompt(
        message: String,
        customerName: String,
        toolResult: List<Map<String, Any?>>,
        intent: String,
        rules: List<Rule>,
        language: String,
        historyBlock: String
    ): String {
        val biz = DataRepository.businessInfo.value
        val bizName = biz.brandName.ifEmpty { "our business" }

        val langInstruction = if (language == "roman_urdu")
            "The customer is speaking in Roman Urdu (English letters, Urdu words). You MUST reply in Roman Urdu only. Do NOT use English-only sentences. Do NOT use Urdu/Arabic script."
        else
            "The customer is speaking in English. You MUST reply in English only. Do NOT use Roman Urdu words."

        val contextBlock = buildContextBlock(toolResult)
        val rulesBlock = buildRulesBlock(rules)
        val orderBlock = buildOrderBlock(toolResult)

        return """You are a friendly WhatsApp customer service assistant for $bizName. Answer the customer's question naturally and concisely.

$langInstruction

$contextBlock
$orderBlock
$rulesBlock
${if (historyBlock.isNotEmpty()) "\nCONVERSATION HISTORY:\n$historyBlock\n" else ""}
Customer name: $customerName
Customer message: "$message"

Reply:"""
    }

    private fun buildContextBlock(toolResult: List<Map<String, Any?>>): String {
        if (toolResult.isEmpty()) return ""
        val sb = StringBuilder("DATA FROM DATABASE:\n")
        for (item in toolResult.take(8)) {
            val title = item["title"]?.toString() ?: item["question"]?.toString() ?: item["name"]?.toString() ?: ""
            val content = item["content"]?.toString() ?: item["answer"]?.toString() ?: item["description"]?.toString() ?: ""
            val price = item["price"]?.let { " | Price: $it" } ?: ""
            val category = item["category"]?.let { " | Category: $it" } ?: ""
            sb.append("  - $title${if (content.isNotEmpty()) ": $content" else ""}$price$category\n")

            // List extra fields for business_info
            val coreKeys = setOf("id", "title", "content", "updatedAt", "createdAt", "section", "price", "category")
            for ((k, v) in item) {
                if (k !in coreKeys && v != null && v.toString().isNotBlank()) {
                    sb.append("  $k: $v\n")
                }
            }
        }
        return sb.toString()
    }

    private fun buildRulesBlock(rules: List<Rule>): String {
        if (rules.isEmpty()) return ""
        val sb = StringBuilder("\nRULES YOU MUST FOLLOW:\n")
        for (rule in rules) {
            sb.append("  - ${rule.title}${if (rule.content.isNotEmpty()) ": ${rule.content}" else ""}\n")
        }
        return sb.toString()
    }

    private fun buildOrderBlock(toolResult: List<Map<String, Any?>>): String {
        if (toolResult.isEmpty() || toolResult[0]["orderNumber"] == null) return ""
        val sb = StringBuilder("\nORDER DETAILS:\n")
        for (order in toolResult.take(3)) {
            sb.append("  - Order Number: ${order["orderNumber"] ?: "N/A"}\n")
            sb.append("    Customer: ${order["customerName"] ?: "N/A"}\n")
            sb.append("    Items: ${order["items"] ?: "N/A"}\n")
            sb.append("    Total: ${order["totalAmount"] ?: "N/A"}\n")
            sb.append("    Status: ${order["status"] ?: "pending"}\n")
            if (order["address"] != null) sb.append("    Address: ${order["address"]}\n")
        }
        sb.append("If this is a NEW order, include the Order Number in your reply.\n")
        return sb.toString()
    }

    // ── Tool execution ──
    private fun executeTool(classification: Classification, customerNumber: String, message: String): List<Map<String, Any?>> {
        if (classification.intent != "database") return emptyList()
        val tool = classification.tool
        val args = classification.args

        return when (tool) {
            // Business Info
            "get_business_info" -> getBusinessInfo()

            // Knowledge Base
            "get_kb_items_by_section" -> {
                val section = args?.get("section") ?: return emptyList()
                getKbItemsBySection(section)
            }
            "get_kb_item_by_id" -> {
                val id = args?.get("id") ?: return emptyList()
                getKbItemById(id)
            }
            "search_kb_items" -> {
                val section = args?.get("section") ?: return emptyList()
                val field = args.get("field") ?: "title"
                val value = args.get("value") ?: return emptyList()
                searchKbItems(section, field, value)
            }
            "get_available_items" -> {
                val section = args?.get("section") ?: return emptyList()
                getAvailableItems(section)
            }

            // Categories
            "get_categories_by_section" -> {
                val section = args?.get("section") ?: return emptyList()
                getCategoriesBySection(section)
            }
            "get_all_categories" -> getAllCategories()

            // Orders
            "get_order_by_number" -> {
                val orderNumber = args?.get("orderNumber") ?: return emptyList()
                getOrderByNumber(orderNumber)
            }
            "get_orders_by_phone" -> {
                val phone = args?.get("phoneNumber") ?: customerNumber
                getOrdersByPhone(phone)
            }
            "get_pending_orders" -> getOrdersByStatus("pending")
            "get_orders_by_status" -> {
                val status = args?.get("status") ?: "pending"
                getOrdersByStatus(status)
            }

            // Contacts
            "get_contact_by_phone" -> {
                val phone = args?.get("phoneNumber") ?: customerNumber
                getContactByPhone(phone)
            }
            "get_all_contacts" -> getAllContacts()

            // AI Rules
            "get_active_rules" -> getActiveRules()
            "get_all_rules" -> getAllRules()

            // System Logs
            "get_recent_logs" -> {
                val level = args?.get("level") ?: "info"
                val limit = args.get("limit")?.toString()?.toIntOrNull() ?: 10
                getRecentLogs(level, limit)
            }

            else -> emptyList()
        }
    }

    // ── Business Info ──
    private fun getBusinessInfo(): List<Map<String, Any?>> {
        val biz = DataRepository.businessInfo.value
        return listOf(gson.fromJson(gson.toJsonTree(biz), Map::class.java) as Map<String, Any?>)
    }

    // ── Knowledge Base ──
    private fun getKbItemsBySection(section: String): List<Map<String, Any?>> {
        return DataRepository.kbItemsBySection(section).map {
            gson.fromJson(gson.toJsonTree(it), Map::class.java) as Map<String, Any?>
        }
    }

    private fun getKbItemById(id: String): List<Map<String, Any?>> {
        val item = DataRepository.kbItems.value.find { it.id == id }
        return if (item != null) {
            listOf(gson.fromJson(gson.toJsonTree(item), Map::class.java) as Map<String, Any?>)
        } else emptyList()
    }

    private fun searchKbItems(section: String, field: String, value: String): List<Map<String, Any?>> {
        val items = getKbItemsBySection(section)
        return items.filter {
            it[field]?.toString()?.contains(value, ignoreCase = true) == true
        }.ifEmpty { items }
    }

    private fun getAvailableItems(section: String): List<Map<String, Any?>> {
        return getKbItemsBySection(section).filter {
            it["available"] == true
        }
    }

    // ── Categories ──
    private fun getCategoriesBySection(section: String): List<Map<String, Any?>> {
        return DataRepository.categories.value.filter { it.section == section }.map {
            gson.fromJson(gson.toJsonTree(it), Map::class.java) as Map<String, Any?>
        }
    }

    private fun getAllCategories(): List<Map<String, Any?>> {
        return DataRepository.categories.value.map {
            gson.fromJson(gson.toJsonTree(it), Map::class.java) as Map<String, Any?>
        }
    }

    // ── Orders ──
    private fun getOrderByNumber(orderNumber: String): List<Map<String, Any?>> {
        val order = DataRepository.findOrderByNumber(orderNumber)
        return if (order != null) {
            listOf(gson.fromJson(gson.toJsonTree(order), Map::class.java) as Map<String, Any?>)
        } else emptyList()
    }

    private fun getOrdersByPhone(phone: String): List<Map<String, Any?>> {
        return DataRepository.findOrdersByPhone(phone).map {
            gson.fromJson(gson.toJsonTree(it), Map::class.java) as Map<String, Any?>
        }
    }

    private fun getOrdersByStatus(status: String): List<Map<String, Any?>> {
        return DataRepository.orders.value.filter { it.status == status }.map {
            gson.fromJson(gson.toJsonTree(it), Map::class.java) as Map<String, Any?>
        }
    }

    // ── Contacts ──
    private fun getContactByPhone(phone: String): List<Map<String, Any?>> {
        val contact = DataRepository.contacts.value.find { it.phoneNumber == phone }
        return if (contact != null) {
            listOf(gson.fromJson(gson.toJsonTree(contact), Map::class.java) as Map<String, Any?>)
        } else emptyList()
    }

    private fun getAllContacts(): List<Map<String, Any?>> {
        return DataRepository.contacts.value.map {
            gson.fromJson(gson.toJsonTree(it), Map::class.java) as Map<String, Any?>
        }
    }

    // ── AI Rules ──
    private fun getActiveRules(): List<Map<String, Any?>> {
        return DataRepository.rules.value.filter { it.active }.map {
            gson.fromJson(gson.toJsonTree(it), Map::class.java) as Map<String, Any?>
        }
    }

    private fun getAllRules(): List<Map<String, Any?>> {
        return DataRepository.rules.value.map {
            gson.fromJson(gson.toJsonTree(it), Map::class.java) as Map<String, Any?>
        }
    }

    // ── System Logs ──
    private fun getRecentLogs(level: String, limit: Int): List<Map<String, Any?>> {
        return DataRepository.logs.value
            .filter { it.level == level }
            .takeLast(limit)
            .map {
                gson.fromJson(gson.toJsonTree(it), Map::class.java) as Map<String, Any?>
            }
    }

    private fun generateOrderNumber(): String {
        val ts = System.currentTimeMillis()
        val rand = (1000..9999).random()
        return "ORD-${ts.toString().takeLast(8)}-$rand"
    }

    // ── Helpers ──
    private fun parseClassification(json: String): Classification {
        return try {
            val clean = json.trim().let { if (it.startsWith("```")) it.removePrefix("```json").removePrefix("```").removeSuffix("```").trim() else it }
            val obj = JsonParser.parseString(clean).asJsonObject
            Classification(
                intent = obj.get("intent")?.asString ?: "general",
                tool = obj.get("tool")?.asString ?: "",
                args = obj.getAsJsonObject("args")?.entrySet()?.associate { it.key to it.value.asString } ?: emptyMap()
            )
        } catch (e: Exception) {
            Classification(intent = "general")
        }
    }

    private fun detectLanguage(text: String): String {
        val romanUrduWords = listOf("kya", "hai", "ha", "kaise", "kaisa", "kitna", "kitne", "kahan", "kahaan", "mujhe", "mera", "meri", "aap", "apka", "apki", "bhai", "yaar", "acha", "achha", "theek", "nahi", "nahin", "haan", "bhej", "bhejo", "karo", "karna", "chahiye", "wala", "wale", "mein", "par", "aur", "ya", "lekin", "magar", "jab", "tab", "agar")
        val lower = text.lowercase()
        val matches = romanUrduWords.count { lower.contains(it) }
        return if (matches >= 1) "roman_urdu" else "english"
    }

    private fun buildHistoryBlock(history: List<ChatHistoryEntry>): String {
        if (history.isEmpty()) return ""
        return history.takeLast(10).joinToString("\n") { "${it.role}: ${it.content}" }
    }

    // ── KB fallback (when AI unavailable) ──
    private fun kbFallback(message: String): AgentResult {
        val lower = message.lowercase()
        val items = DataRepository.kbItems.value
        val match = items.firstOrNull {
            it.title.lowercase().split(" ").any { word -> word.length > 3 && lower.contains(word) }
        }
        val reply = match?.let { "${it.title}: ${it.content}" } ?: "Sorry, I couldn't process that. Please try again."
        return AgentResult(reply, "kb_fallback")
    }

    private data class Classification(
        val intent: String,
        val tool: String = "",
        val args: Map<String, String> = emptyMap()
    )
}
