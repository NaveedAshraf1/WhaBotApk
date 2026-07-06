package com.example.whabotpro.ai

import com.example.whabotpro.data.model.*
import com.example.whabotpro.data.store.DataRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Processes raw text data using AI (Groq or Gemini) with tool-calling.
 * The AI receives raw data + DB schema, then returns tool calls to save data.
 *
 * If the raw data is large (>6000 chars), uses Gemini (larger context window).
 * Otherwise uses Groq (faster).
 */
class RawDataProcessor(
    private val groq: GroqClient = GroqClient(),
    private val gemini: GeminiClient = GeminiClient()
) {

    data class ProcessResult(
        val success: Boolean,
        val savedCount: Int,
        val details: String,
        val aiResponse: String,
        val engineUsed: String
    )

    private val gson = Gson()

    suspend fun process(rawData: String): ProcessResult = withContext(Dispatchers.IO) {
        if (rawData.isBlank()) return@withContext ProcessResult(false, 0, "No data provided", "", "none")

        // Groq llama-3.1-8b-instant has TPM limit of 6000. For data > 3000 chars,
        // the prompt + response will exceed that, so use Gemini (larger context).
        val useGemini = rawData.length > 3000 || !groq.isReady
        val engine = if (useGemini) "gemini" else "groq"

        try {
            DataRepository.log("info", "RawDataProcessor: processing ${rawData.length} chars with $engine")

            val schema = buildSchemaBlock()
            val prompt = buildPrompt(rawData, schema)

            // Get AI response with tool calls
            val aiResponse = if (useGemini) {
                gemini.chatLongReply(prompt)
            } else {
                groq.chatLongReply(prompt)
            }

            DataRepository.log("info", "RawDataProcessor: AI response received (${aiResponse.length} chars)")

            // Parse and execute tool calls
            val toolCalls = parseToolCalls(aiResponse)
            if (toolCalls.isEmpty()) {
                return@withContext ProcessResult(false, 0, "AI did not return valid tool calls", aiResponse, engine)
            }

            var savedCount = 0
            val details = StringBuilder()

            for (call in toolCalls) {
                val result = executeTool(call)
                savedCount += result.first
                details.append(result.second).append("\n")
            }

            DataRepository.log("info", "RawDataProcessor: saved $savedCount items")
            ProcessResult(true, savedCount, details.toString().trim(), aiResponse, engine)

        } catch (e: Exception) {
            DataRepository.log("error", "RawDataProcessor error: ${e.message}")
            ProcessResult(false, 0, "Error: ${e.message}", "", engine)
        }
    }

    private fun buildSchemaBlock(): String {
        val sb = StringBuilder()
        sb.append("DATABASE SCHEMA — Available tables and their fields:\n\n")

        sb.append("1. business_info (single row, updates existing):\n")
        sb.append("   Fields: brandName, tagline, category, phone, email, website, address,\n")
        sb.append("   cuisine, openingHours, businessDescription, mission, vision, coreValues,\n")
        sb.append("   positioning, targetAudience, competitors, wordsToUse, wordsToAvoid,\n")
        sb.append("   brandVoice, signatureStory, originStory, uniqueSellingProposition, landmarks\n\n")

        sb.append("2. kb_items (menu, deals, services, faqs, policies, events, reservations, delivery_zones):\n")
        sb.append("   Fields: id (auto), section (required), title (required), content, description, price,\n")
        sb.append("   category, available (boolean), stock (int), active (boolean), extra (json)\n")
        sb.append("   Valid sections: menu, promotions, services, faqs, policies, events, reservations, delivery_zones\n\n")

        sb.append("3. categories:\n")
        sb.append("   Fields: id (auto), section (required), name (required), icon\n\n")

        sb.append("4. rules (AI behavior rules):\n")
        sb.append("   Fields: id (auto), title (required), content (required), active (boolean)\n\n")

        sb.append("5. orders:\n")
        sb.append("   Fields: id (auto), orderNumber (auto), customerName, customerPhone, items, totalAmount,\n")
        sb.append("   orderType (delivery/takeaway/dinein), status (pending/preparing/ready/completed/cancelled),\n")
        sb.append("   address, specialRequests, createdAt (auto)\n\n")

        sb.append("6. contacts:\n")
        sb.append("   Fields: id (auto), name, phoneNumber, notes, createdAt (auto)\n\n")

        return sb.toString()
    }

    private fun buildPrompt(rawData: String, schema: String): String {
        return """You are a data extraction assistant. Parse raw business data and return tool calls to save it.

$schema

TOOLS (return as JSON array):

BUSINESS INFO:
1. create_business_info: {"tool":"create_business_info","args":{"brandName":"...","phone":"...","address":"...","cuisine":"...","openingHours":"..."}}
2. update_business_info: {"tool":"update_business_info","args":{"brandName":"...","phone":"...","address":"...","cuisine":"..."}}

KNOWLEDGE BASE ITEMS:
3. create_kb_item: {"tool":"create_kb_item","args":{"section":"menu","title":"Item Name","content":"Description","price":"PKR 500","category":"BBQ","available":true,"stock":10}}
4. update_kb_item: {"tool":"update_kb_item","args":{"id":"<existing_id>","title":"New Title","price":"PKR 600","available":false}}
5. delete_kb_item: {"tool":"delete_kb_item","args":{"id":"<item_id>"}}
6. toggle_kb_item_availability: {"tool":"toggle_kb_item_availability","args":{"id":"<item_id>","available":false}}

CATEGORIES:
7. create_category: {"tool":"create_category","args":{"section":"menu","name":"BBQ","icon":"🍖"}}
8. update_category: {"tool":"update_category","args":{"id":"<existing_id>","name":"Grill","icon":"🔥"}}
9. delete_category: {"tool":"delete_category","args":{"id":"<category_id>"}}

AI RULES:
10. create_rule: {"tool":"create_rule","args":{"title":"Rule Name","content":"Rule content","active":true}}
11. update_rule: {"tool":"update_rule","args":{"id":"<existing_id>","title":"New Title","content":"New content","active":false}}
12. delete_rule: {"tool":"delete_rule","args":{"id":"<rule_id>"}}
13. toggle_rule_active: {"tool":"toggle_rule_active","args":{"id":"<rule_id>","active":false}}

ORDERS:
14. create_order: {"tool":"create_order","args":{"customerName":"...","customerPhone":"...","items":"...","totalAmount":"...","orderType":"delivery","address":"..."}}
15. update_order_status: {"tool":"update_order_status","args":{"orderNumber":"<order_number>","status":"preparing"}}
16. cancel_order: {"tool":"cancel_order","args":{"orderNumber":"<order_number>"}}

CONTACTS:
17. create_contact: {"tool":"create_contact","args":{"name":"John Doe","phoneNumber":"923001234567","notes":"VIP customer"}}
18. update_contact: {"tool":"update_contact","args":{"id":"<existing_id>","name":"New Name","notes":"Updated notes"}}
19. delete_contact: {"tool":"delete_contact","args":{"id":"<contact_id>"}}

RULES:
- Extract ALL items from the raw data. Don't skip any.
- Each unique category should only appear ONCE in create_category calls. Don't create duplicate categories.
- Each menu item should only appear ONCE. Don't create duplicate items.
- For items without a price, use empty string "".
- For items without a category, use "General".
- Infer section: menu items→"menu", deals→"promotions", FAQs→"faqs", policies→"policies", services→"services", events→"events".
- Only use create_business_info if business name/phone/address is in the data.
- Keep content/descriptions concise (max 200 chars).
- For updates, you need the existing ID. If you don't have it, use create instead.
- Return ONLY a JSON array. No markdown, no explanation.

RAW DATA:
---
$rawData
---

JSON array:"""
    }

    private fun parseToolCalls(response: String): List<ToolCall> {
        return try {
            val clean = response.trim().let {
                if (it.startsWith("```")) it.removePrefix("```json").removePrefix("```").removeSuffix("```").trim() else it
            }
            // Find the JSON array in the response
            val start = clean.indexOf('[')
            val end = clean.lastIndexOf(']')
            if (start < 0 || end < 0) return emptyList()
            val jsonArr = clean.substring(start, end + 1)
            val arr = JsonParser.parseString(jsonArr).asJsonArray
            arr.map { elem ->
                val obj = elem.asJsonObject
                val tool = obj.get("tool")?.asString ?: ""
                val argsObj = obj.getAsJsonObject("args") ?: JsonObject()
                val args = argsObj.entrySet().associate { it.key to it.value }
                ToolCall(tool, args)
            }
        } catch (e: Exception) {
            DataRepository.log("error", "Parse tool calls error: ${e.message}")
            emptyList()
        }
    }

    private fun executeTool(call: ToolCall): Pair<Int, String> {
        return try {
            when (call.tool) {
                // ── Business Info ──
                "create_business_info", "update_business_info" -> {
                    val biz = DataRepository.businessInfo.value
                    call.args.forEach { (key, value) ->
                        if (!value.isJsonNull) {
                            try {
                                val field = BusinessInfo::class.java.getDeclaredField(key)
                                field.isAccessible = true
                                if (field.type == String::class.java) {
                                    field.set(biz, value.asString)
                                }
                            } catch (e: Exception) { }
                        }
                    }
                    DataRepository.saveBusinessInfo(biz)
                    Pair(1, "Business info updated")
                }

                // ── Knowledge Base Items ──
                "create_kb_item" -> {
                    val item = KbItem(
                        section = call.args["section"]?.asString ?: "menu",
                        title = call.args["title"]?.asString ?: "",
                        content = call.args["content"]?.asString ?: "",
                        description = call.args["description"]?.asString ?: "",
                        price = call.args["price"]?.asString ?: "",
                        category = call.args["category"]?.asString ?: "",
                        available = call.args["available"]?.asBoolean ?: true,
                        stock = call.args["stock"]?.asInt ?: 0,
                        active = call.args["active"]?.asBoolean ?: true
                    )
                    if (item.title.isNotBlank()) {
                        DataRepository.addKbItem(item)
                        Pair(1, "Created: ${item.title} (${item.section})")
                    } else Pair(0, "Skipped empty item")
                }
                "update_kb_item" -> {
                    val id = call.args["id"]?.asString ?: return Pair(0, "Missing id")
                    val existing = DataRepository.kbItems.value.find { it.id == id }
                    if (existing != null) {
                        val updated = existing.copy(
                            title = call.args["title"]?.asString ?: existing.title,
                            content = call.args["content"]?.asString ?: existing.content,
                            description = call.args["description"]?.asString ?: existing.description,
                            price = call.args["price"]?.asString ?: existing.price,
                            category = call.args["category"]?.asString ?: existing.category,
                            available = call.args["available"]?.asBoolean ?: existing.available,
                            stock = call.args["stock"]?.asInt ?: existing.stock,
                            active = call.args["active"]?.asBoolean ?: existing.active
                        )
                        DataRepository.updateKbItem(updated)
                        Pair(1, "Updated: ${updated.title}")
                    } else Pair(0, "Item not found: $id")
                }
                "delete_kb_item" -> {
                    val id = call.args["id"]?.asString ?: return Pair(0, "Missing id")
                    DataRepository.deleteKbItem(id)
                    Pair(1, "Deleted item: $id")
                }
                "toggle_kb_item_availability" -> {
                    val id = call.args["id"]?.asString ?: return Pair(0, "Missing id")
                    val available = call.args["available"]?.asBoolean ?: return Pair(0, "Missing available")
                    val existing = DataRepository.kbItems.value.find { it.id == id }
                    if (existing != null) {
                        val updated = existing.copy(available = available)
                        DataRepository.updateKbItem(updated)
                        Pair(1, "Toggled availability for: ${existing.title}")
                    } else Pair(0, "Item not found: $id")
                }

                // ── Categories ──
                "create_category" -> {
                    val cat = Category(
                        section = call.args["section"]?.asString ?: "menu",
                        name = call.args["name"]?.asString ?: "",
                        icon = call.args["icon"]?.asString ?: ""
                    )
                    if (cat.name.isNotBlank()) {
                        DataRepository.addCategory(cat)
                        Pair(1, "Created category: ${cat.name} (${cat.section})")
                    } else Pair(0, "Skipped empty category")
                }
                "update_category" -> {
                    val id = call.args["id"]?.asString ?: return Pair(0, "Missing id")
                    val existing = DataRepository.categories.value.find { it.id == id }
                    if (existing != null) {
                        val updated = existing.copy(
                            name = call.args["name"]?.asString ?: existing.name,
                            icon = call.args["icon"]?.asString ?: existing.icon
                        )
                        DataRepository.updateCategory(updated)
                        Pair(1, "Updated category: ${updated.name}")
                    } else Pair(0, "Category not found: $id")
                }
                "delete_category" -> {
                    val id = call.args["id"]?.asString ?: return Pair(0, "Missing id")
                    DataRepository.deleteCategory(id)
                    Pair(1, "Deleted category: $id")
                }

                // ── AI Rules ──
                "create_rule" -> {
                    val rule = Rule(
                        title = call.args["title"]?.asString ?: "",
                        content = call.args["content"]?.asString ?: "",
                        active = call.args["active"]?.asBoolean ?: true
                    )
                    if (rule.title.isNotBlank()) {
                        DataRepository.addRule(rule)
                        Pair(1, "Created rule: ${rule.title}")
                    } else Pair(0, "Skipped empty rule")
                }
                "update_rule" -> {
                    val id = call.args["id"]?.asString ?: return Pair(0, "Missing id")
                    val existing = DataRepository.rules.value.find { it.id == id }
                    if (existing != null) {
                        val updated = existing.copy(
                            title = call.args["title"]?.asString ?: existing.title,
                            content = call.args["content"]?.asString ?: existing.content,
                            active = call.args["active"]?.asBoolean ?: existing.active
                        )
                        DataRepository.updateRule(updated)
                        Pair(1, "Updated rule: ${updated.title}")
                    } else Pair(0, "Rule not found: $id")
                }
                "delete_rule" -> {
                    val id = call.args["id"]?.asString ?: return Pair(0, "Missing id")
                    DataRepository.deleteRule(id)
                    Pair(1, "Deleted rule: $id")
                }
                "toggle_rule_active" -> {
                    val id = call.args["id"]?.asString ?: return Pair(0, "Missing id")
                    val active = call.args["active"]?.asBoolean ?: return Pair(0, "Missing active")
                    val existing = DataRepository.rules.value.find { it.id == id }
                    if (existing != null) {
                        val updated = existing.copy(active = active)
                        DataRepository.updateRule(updated)
                        Pair(1, "Toggled active for: ${existing.title}")
                    } else Pair(0, "Rule not found: $id")
                }

                // ── Orders ──
                "create_order" -> {
                    val order = Order(
                        orderNumber = "ORD-${System.currentTimeMillis()}",
                        customerName = call.args["customerName"]?.asString ?: "",
                        customerPhone = call.args["customerPhone"]?.asString ?: "",
                        items = call.args["items"]?.asString ?: "",
                        totalAmount = call.args["totalAmount"]?.asString ?: "",
                        orderType = call.args["orderType"]?.asString ?: "delivery",
                        status = "pending",
                        address = call.args["address"]?.asString ?: "",
                        specialRequests = call.args["specialRequests"]?.asString ?: ""
                    )
                    DataRepository.addOrder(order)
                    Pair(1, "Created order: ${order.orderNumber}")
                }
                "update_order_status" -> {
                    val orderNumber = call.args["orderNumber"]?.asString ?: return Pair(0, "Missing orderNumber")
                    val status = call.args["status"]?.asString ?: return Pair(0, "Missing status")
                    val existing = DataRepository.orders.value.find { it.orderNumber == orderNumber }
                    if (existing != null) {
                        val updated = existing.copy(status = status)
                        DataRepository.updateOrder(updated)
                        Pair(1, "Updated order $orderNumber to $status")
                    } else Pair(0, "Order not found: $orderNumber")
                }
                "cancel_order" -> {
                    val orderNumber = call.args["orderNumber"]?.asString ?: return Pair(0, "Missing orderNumber")
                    val existing = DataRepository.orders.value.find { it.orderNumber == orderNumber }
                    if (existing != null) {
                        val updated = existing.copy(status = "cancelled")
                        DataRepository.updateOrder(updated)
                        Pair(1, "Cancelled order: $orderNumber")
                    } else Pair(0, "Order not found: $orderNumber")
                }

                // ── Contacts ──
                "create_contact" -> {
                    val contact = Contact(
                        name = call.args["name"]?.asString ?: "",
                        phoneNumber = call.args["phoneNumber"]?.asString ?: "",
                        notes = call.args["notes"]?.asString ?: ""
                    )
                    if (contact.name.isNotBlank() && contact.phoneNumber.isNotBlank()) {
                        DataRepository.addContact(contact)
                        Pair(1, "Created contact: ${contact.name}")
                    } else Pair(0, "Skipped empty contact")
                }
                "update_contact" -> {
                    val id = call.args["id"]?.asString ?: return Pair(0, "Missing id")
                    val existing = DataRepository.contacts.value.find { it.id == id }
                    if (existing != null) {
                        val updated = existing.copy(
                            name = call.args["name"]?.asString ?: existing.name,
                            phoneNumber = call.args["phoneNumber"]?.asString ?: existing.phoneNumber,
                            notes = call.args["notes"]?.asString ?: existing.notes
                        )
                        DataRepository.updateContact(updated)
                        Pair(1, "Updated contact: ${updated.name}")
                    } else Pair(0, "Contact not found: $id")
                }
                "delete_contact" -> {
                    val id = call.args["id"]?.asString ?: return Pair(0, "Missing id")
                    DataRepository.deleteContact(id)
                    Pair(1, "Deleted contact: $id")
                }

                else -> Pair(0, "Unknown tool: ${call.tool}")
            }
        } catch (e: Exception) {
            Pair(0, "Error executing ${call.tool}: ${e.message}")
        }
    }

    private data class ToolCall(
        val tool: String,
        val args: Map<String, com.google.gson.JsonElement>
    )
}
