package com.example.whabotpro.data.store

import android.content.Context
import com.example.whabotpro.data.db.AppDatabase
import com.example.whabotpro.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Room-backed repository — replaces JsonStore.
 * Maintains the same API surface as JsonStore for easy migration.
 * All DB operations are synchronous (called from background threads or handlers).
 */
object DataRepository {

    private lateinit var db: AppDatabase
    private val gson = Gson()

    // In-memory state flows for Compose reactivity (mirrored from Room)
    private val _businessInfo = MutableStateFlow(BusinessInfo())
    val businessInfo: StateFlow<BusinessInfo> = _businessInfo

    private val _kbItems = MutableStateFlow<List<KbItem>>(emptyList())
    val kbItems: StateFlow<List<KbItem>> = _kbItems

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders

    private val _inbox = MutableStateFlow<List<InboxMessage>>(emptyList())
    val inbox: StateFlow<List<InboxMessage>> = _inbox

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    private val _rules = MutableStateFlow<List<Rule>>(emptyList())
    val rules: StateFlow<List<Rule>> = _rules

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings

    val SECTIONS = listOf(
        "menu" to "Menu",
        "services" to "Services",
        "faqs" to "FAQs",
        "promotions" to "Deals",
        "delivery_zones" to "Delivery Zones",
        "events" to "Events",
        "reservations" to "Reservations",
        "policies" to "Policies"
    )

    fun init(context: Context) {
        db = AppDatabase.get(context)
        migrateFromJsonIfNeeded(context)
        loadAll()
    }

    private fun migrateFromJsonIfNeeded(context: Context) {
        val migrationFlag = File(context.filesDir, "room_migrated.flag")
        if (migrationFlag.exists()) return

        try {
            // Migrate business_info — parse manually to handle missing 'id' field
            val bizFile = File(context.filesDir, "business_info.json")
            if (bizFile.exists()) {
                val bizObj = com.google.gson.JsonParser.parseString(bizFile.readText()).asJsonObject
                val biz = BusinessInfo(id = "1")
                bizObj.entrySet().forEach { (key, value) ->
                    if (!value.isJsonNull) {
                        try {
                            val field = BusinessInfo::class.java.getDeclaredField(key)
                            field.isAccessible = true
                            when (field.type) {
                                String::class.java -> field.set(biz, value.asString)
                                else -> {}
                            }
                        } catch (e: Exception) { }
                    }
                }
                db.businessInfoDao().upsert(biz)
            }

            // Migrate kb_items — handle old extra field (was Map, now String)
            val kbFile = File(context.filesDir, "kb_items.json")
            if (kbFile.exists()) {
                val rawItems = com.google.gson.JsonParser.parseString(kbFile.readText()).asJsonArray
                val items = rawItems.map { elem ->
                    val obj = elem.asJsonObject
                    KbItem(
                        id = obj.get("id")?.asString ?: java.util.UUID.randomUUID().toString(),
                        section = obj.get("section")?.asString ?: "",
                        title = obj.get("title")?.asString ?: "",
                        content = obj.get("content")?.asString ?: "",
                        description = obj.get("description")?.asString ?: "",
                        price = obj.get("price")?.asString ?: "",
                        category = obj.get("category")?.asString ?: "",
                        available = obj.get("available")?.asBoolean ?: true,
                        stock = obj.get("stock")?.asInt ?: 0,
                        active = obj.get("active")?.asBoolean ?: true,
                        extra = obj.get("extra")?.toString() ?: "",
                        createdAt = obj.get("createdAt")?.asLong ?: System.currentTimeMillis(),
                        updatedAt = obj.get("updatedAt")?.asLong ?: System.currentTimeMillis()
                    )
                }
                if (items.isNotEmpty()) db.kbItemDao().upsertAll(items)
            }

            // Migrate categories
            val catFile = File(context.filesDir, "categories.json")
            if (catFile.exists()) {
                val type = object : TypeToken<List<Category>>() {}.type
                val cats: List<Category> = gson.fromJson(catFile.readText(), type) ?: emptyList()
                if (cats.isNotEmpty()) db.categoryDao().upsertAll(cats)
            }

            // Migrate orders
            val orderFile = File(context.filesDir, "orders.json")
            if (orderFile.exists()) {
                val type = object : TypeToken<List<Order>>() {}.type
                val orders: List<Order> = gson.fromJson(orderFile.readText(), type) ?: emptyList()
                orders.forEach { db.orderDao().upsert(it) }
            }

            // Migrate inbox
            val inboxFile = File(context.filesDir, "inbox.json")
            if (inboxFile.exists()) {
                val type = object : TypeToken<List<InboxMessage>>() {}.type
                val msgs: List<InboxMessage> = gson.fromJson(inboxFile.readText(), type) ?: emptyList()
                msgs.forEach { db.inboxDao().insert(it) }
            }

            // Migrate contacts
            val contactFile = File(context.filesDir, "contacts.json")
            if (contactFile.exists()) {
                val type = object : TypeToken<List<Contact>>() {}.type
                val contacts: List<Contact> = gson.fromJson(contactFile.readText(), type) ?: emptyList()
                contacts.forEach { db.contactDao().upsert(it) }
            }

            // Migrate rules
            val rulesFile = File(context.filesDir, "rules.json")
            if (rulesFile.exists()) {
                val type = object : TypeToken<List<Rule>>() {}.type
                val rules: List<Rule> = gson.fromJson(rulesFile.readText(), type) ?: emptyList()
                if (rules.isNotEmpty()) db.ruleDao().upsertAll(rules)
            }

            // Migrate logs
            val logsFile = File(context.filesDir, "logs.json")
            if (logsFile.exists()) {
                val type = object : TypeToken<List<LogEntry>>() {}.type
                val logs: List<LogEntry> = gson.fromJson(logsFile.readText(), type) ?: emptyList()
                logs.forEach { db.logDao().insert(it) }
            }

            // Migrate settings
            val settingsFile = File(context.filesDir, "settings.json")
            if (settingsFile.exists()) {
                val s = gson.fromJson(settingsFile.readText(), AppSettings::class.java)
                if (s != null) { s.id = "1"; db.settingsDao().upsert(s) }
            }

            migrationFlag.writeText("migrated at ${System.currentTimeMillis()}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadAll() {
        _businessInfo.value = db.businessInfoDao().get() ?: BusinessInfo()
        _kbItems.value = db.kbItemDao().getAll()
        _categories.value = db.categoryDao().getAll()
        _orders.value = db.orderDao().getAll()
        _inbox.value = db.inboxDao().getAll()
        _contacts.value = db.contactDao().getAll()
        _rules.value = db.ruleDao().getAll()
        _logs.value = db.logDao().getAll()
        _settings.value = db.settingsDao().get() ?: AppSettings()
    }

    // ── Business Info ──
    fun saveBusinessInfo(info: BusinessInfo) {
        info.id = "1"
        db.businessInfoDao().upsert(info)
        _businessInfo.value = info
    }

    // ── KB Items ──
    fun kbItemsBySection(section: String): List<KbItem> =
        _kbItems.value.filter { it.section == section }

    fun addKbItem(item: KbItem) {
        // Dedup: if item with same title+section exists, update it instead of creating duplicate
        val existing = db.kbItemDao().findByTitleAndSection(item.section, item.title)
        if (existing != null) {
            item.id = existing.id
            item.createdAt = existing.createdAt
            item.updatedAt = System.currentTimeMillis()
            db.kbItemDao().upsert(item)
            _kbItems.value = _kbItems.value.map { if (it.id == item.id) item else it }
        } else {
            db.kbItemDao().upsert(item)
            _kbItems.value = _kbItems.value + item
        }
    }

    fun updateKbItem(item: KbItem) {
        item.updatedAt = System.currentTimeMillis()
        db.kbItemDao().upsert(item)
        _kbItems.value = _kbItems.value.map { if (it.id == item.id) item else it }
    }

    fun deleteKbItem(id: String) {
        db.kbItemDao().delete(id)
        _kbItems.value = _kbItems.value.filterNot { it.id == id }
    }

    // ── Categories ──
    fun categoriesBySection(section: String): List<Category> =
        _categories.value.filter { it.section == section }

    fun addCategory(cat: Category) {
        // Dedup: skip if category with same name+section already exists
        val existing = db.categoryDao().findByNameAndSection(cat.section, cat.name)
        if (existing == null) {
            db.categoryDao().upsert(cat)
            _categories.value = _categories.value + cat
        }
    }

    fun updateCategory(cat: Category) {
        db.categoryDao().upsert(cat)
        _categories.value = _categories.value.map { if (it.id == cat.id) cat else it }
    }

    fun deleteCategory(id: String) {
        db.categoryDao().delete(id)
        _categories.value = _categories.value.filterNot { it.id == id }
    }

    // ── Orders ──
    fun addOrder(order: Order) {
        db.orderDao().upsert(order)
        _orders.value = listOf(order) + _orders.value
    }

    fun updateOrder(order: Order) {
        db.orderDao().upsert(order)
        _orders.value = _orders.value.map { if (it.id == order.id) order else it }
    }

    fun deleteOrder(id: String) {
        db.orderDao().delete(id)
        _orders.value = _orders.value.filterNot { it.id == id }
    }

    fun findOrderByNumber(orderNumber: String): Order? =
        _orders.value.find { it.orderNumber.equals(orderNumber, ignoreCase = true) }

    fun findOrdersByPhone(phone: String): List<Order> =
        _orders.value.filter { it.customerPhone == phone }

    // ── Inbox ──
    fun logInbox(msg: InboxMessage) {
        db.inboxDao().insert(msg)
        _inbox.value = (listOf(msg) + _inbox.value).take(200)
    }

    fun clearInbox() {
        db.inboxDao().clear()
        _inbox.value = emptyList()
    }

    // ── Contacts ──
    fun addContact(contact: Contact) {
        if (_contacts.value.none { it.phoneNumber == contact.phoneNumber }) {
            db.contactDao().upsert(contact)
            _contacts.value = _contacts.value + contact
        }
    }

    fun updateContact(contact: Contact) {
        db.contactDao().upsert(contact)
        _contacts.value = _contacts.value.map { if (it.id == contact.id) contact else it }
    }

    fun deleteContact(id: String) {
        db.contactDao().delete(id)
        _contacts.value = _contacts.value.filterNot { it.id == id }
    }

    // ── Rules ──
    fun addRule(rule: Rule) {
        // Dedup: if rule with same title exists, update it
        val existing = db.ruleDao().findByTitle(rule.title)
        if (existing != null) {
            rule.id = existing.id
            db.ruleDao().upsert(rule)
            _rules.value = _rules.value.map { if (it.id == rule.id) rule else it }
        } else {
            db.ruleDao().upsert(rule)
            _rules.value = _rules.value + rule
        }
    }

    fun updateRule(rule: Rule) {
        db.ruleDao().upsert(rule)
        _rules.value = _rules.value.map { if (it.id == rule.id) rule else it }
    }

    fun deleteRule(id: String) {
        db.ruleDao().delete(id)
        _rules.value = _rules.value.filterNot { it.id == id }
    }

    fun activeRules(): List<Rule> = _rules.value.filter { it.active }

    // ── Logs ──
    fun log(level: String, message: String) {
        val entry = LogEntry(level = level, message = message)
        db.logDao().insert(entry)
        _logs.value = (listOf(entry) + _logs.value).take(500)
    }

    fun clearLogs() {
        db.logDao().clear()
        _logs.value = emptyList()
    }

    // ── Settings ──
    fun saveSettings(settings: AppSettings) {
        settings.id = "1"
        db.settingsDao().upsert(settings)
        _settings.value = settings
    }

    // ── Meta for AI agent (collection names + counts) ──
    fun buildMeta(): Map<String, Int> {
        val meta = mutableMapOf<String, Int>()
        meta["business_info"] = 1
        for ((key, _) in SECTIONS) {
            meta[key] = kbItemsBySection(key).size
        }
        meta["orders"] = _orders.value.size
        return meta
    }
}
