package com.example.whabotpro.data.store

import android.content.Context
import com.example.whabotpro.data.db.AppDatabase
import com.example.whabotpro.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Room-backed repository — replaces JsonStore.
 * Maintains the same API surface as JsonStore for easy migration.
 * All DB operations are synchronous (called from background threads or handlers).
 */
object DataRepository {

    private lateinit var db: AppDatabase

    /** True once init() has completed successfully. */
    val isInitialized: Boolean get() = ::db.isInitialized

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
        loadAll()
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
        // Dedup: skip if category with same name+section already exists (case-insensitive)
        val existing = db.categoryDao().findByNameAndSection(cat.section, cat.name.trim())
        if (existing == null) {
            cat.name = cat.name.trim()
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
        if (isInitialized) {
            try {
                db.logDao().insert(entry)
            } catch (e: Exception) {
                android.util.Log.e("DataRepository", "log() DB insert failed: ${e.message}")
            }
        }
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
