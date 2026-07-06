package com.example.whabotpro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whabotpro.data.model.*
import com.example.whabotpro.data.store.DataRepository
import com.example.whabotpro.engine.EngineManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {

    // ── State flows from stores ──
    val businessInfo = DataRepository.businessInfo
    val kbItems = DataRepository.kbItems
    val categories = DataRepository.categories
    val orders = DataRepository.orders
    val inbox = DataRepository.inbox
    val contacts = DataRepository.contacts
    val rules = DataRepository.rules
    val logs = DataRepository.logs
    val settings = DataRepository.settings

    // ── Engine state ──
    val waState = EngineManager.waState
    val qrCode = EngineManager.qrCode
    val pairingCode = EngineManager.pairingCode
    val pairingPhoneNumber = EngineManager.pairingPhoneNumber
    val connectedUser = EngineManager.connectedUser
    val isRequestingPairing = EngineManager.isRequestingPairing
    val serverRunning = EngineManager.serverRunning

    // ── Test chat state ──
    private val _testChatMessages = MutableStateFlow<List<ChatHistoryEntry>>(emptyList())
    val testChatMessages = _testChatMessages.asStateFlow()
    private val _testChatLoading = MutableStateFlow(false)
    val testChatLoading = _testChatLoading.asStateFlow()

    // ── Actions ──
    fun startService(context: android.content.Context) {
        com.example.whabotpro.service.WhaBotService.start(context)
    }

    fun stopService(context: android.content.Context) {
        com.example.whabotpro.service.WhaBotService.stop(context)
    }

    fun startServer() {
        EngineManager.startEngine()
    }

    fun stopServer() {
        EngineManager.stopEngine()
    }

    fun sendText(number: String, message: String, callback: ((Boolean) -> Unit)? = null) {
        EngineManager.sendText(number, message, callback)
    }

    fun logout() {
        EngineManager.logout()
    }

    fun refreshQr() {
        EngineManager.refreshQr()
    }

    fun startPairingCode(phoneNumber: String) {
        EngineManager.startPairingCode(phoneNumber)
    }

    fun refreshPairingCode(phoneNumber: String) {
        EngineManager.refreshPairingCode(phoneNumber)
    }

    // ── Business ──
    fun saveBusiness(info: BusinessInfo) {
        DataRepository.saveBusinessInfo(info)
    }

    // ── KB items ──
    fun addKbItem(item: KbItem) { DataRepository.addKbItem(item) }
    fun updateKbItem(item: KbItem) { DataRepository.updateKbItem(item) }
    fun deleteKbItem(id: String) { DataRepository.deleteKbItem(id) }
    fun kbItemsBySection(section: String): List<KbItem> = DataRepository.kbItemsBySection(section)

    // ── Categories ──
    fun addCategory(cat: Category) { DataRepository.addCategory(cat) }
    fun deleteCategory(id: String) { DataRepository.deleteCategory(id) }
    fun categoriesBySection(section: String): List<Category> = DataRepository.categoriesBySection(section)

    // ── Orders ──
    fun addOrder(order: Order) { DataRepository.addOrder(order) }
    fun updateOrder(order: Order) { DataRepository.updateOrder(order) }
    fun deleteOrder(id: String) { DataRepository.deleteOrder(id) }

    // ── Inbox ──
    fun clearInbox() { DataRepository.clearInbox() }

    // ── Contacts ──
    fun addContact(contact: Contact) { DataRepository.addContact(contact) }
    fun deleteContact(id: String) { DataRepository.deleteContact(id) }

    // ── Rules ──
    fun addRule(rule: Rule) { DataRepository.addRule(rule) }
    fun updateRule(rule: Rule) { DataRepository.updateRule(rule) }
    fun deleteRule(id: String) { DataRepository.deleteRule(id) }

    // ── Settings ──
    fun saveSettings(settings: AppSettings) {
        DataRepository.saveSettings(settings)
    }

    // ── Logs ──
    fun clearLogs() { DataRepository.clearLogs() }

    // ── Test chat ──
    fun sendTestMessage(message: String) {
        if (message.isBlank()) return
        _testChatMessages.value = _testChatMessages.value + ChatHistoryEntry("user", message)
        _testChatLoading.value = true
        viewModelScope.launch {
            try {
                val reply = EngineManager.testChat(message, _testChatMessages.value)
                _testChatMessages.value = _testChatMessages.value + ChatHistoryEntry("assistant", reply)
            } catch (e: Exception) {
                _testChatMessages.value = _testChatMessages.value + ChatHistoryEntry("assistant", "Error: ${e.message}")
            } finally {
                _testChatLoading.value = false
            }
        }
    }

    fun clearTestChat() {
        _testChatMessages.value = emptyList()
    }
}
