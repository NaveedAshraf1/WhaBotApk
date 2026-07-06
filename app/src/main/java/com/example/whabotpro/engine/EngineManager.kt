package com.example.whabotpro.engine

import android.content.Context
import com.example.whabotpro.ai.Agent
import com.example.whabotpro.ai.GeminiClient
import com.example.whabotpro.ai.GroqClient
import com.example.whabotpro.data.model.*
import com.example.whabotpro.data.store.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton manager — ties together WhatsApp engine and AI agent.
 * The embedded Node.js Baileys server (port 3001) is the only server.
 * Handles incoming messages by routing them through the AI agent and auto-replying.
 */
object EngineManager {

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var waEngine: WhatsAppEngine
    private val groqClient = GroqClient()
    private val geminiClient = GeminiClient()
    private val agent = Agent(groqClient, geminiClient)

    private val aiReady: Boolean get() = groqClient.isReady || geminiClient.isReady
    private val aiModel: String get() = when {
        groqClient.isReady -> groqClient.model
        geminiClient.isReady -> geminiClient.modelName
        else -> ""
    }

    // Chat history per contact (for AI context) — ConcurrentHashMap for thread-safe access
    private val chatHistories = ConcurrentHashMap<String, MutableList<ChatHistoryEntry>>()

    fun init(context: Context) {
        appContext = context.applicationContext
        waEngine = WhatsAppEngine(appContext)
        waEngine.onIncomingMessage = { from, name, body, replyJid -> handleIncoming(from, name, body, replyJid) }

        scope.launch {
            combine(waEngine.state, waEngine.connectedUser, DataRepository.settings, DataRepository.inbox) {
                state, user, settings, inbox ->
                StatusSnapshot(state, user, aiReady, aiModel, true, 3001, inbox.size)
            }.collect { _status.value = it }
        }
    }

    val waState: StateFlow<WaState> get() = waEngine.state
    val qrCode: StateFlow<String?> get() = waEngine.qrCode
    val pairingCode: StateFlow<String?> get() = waEngine.pairingCode
    val pairingPhoneNumber: StateFlow<String> get() = waEngine.pairingPhoneNumber
    val connectedUser: StateFlow<String> get() = waEngine.connectedUser
    val isRequestingPairing: StateFlow<Boolean> get() = waEngine.isRequestingPairing

    // The embedded Node.js server is always running when the app is alive
    private val _serverRunning = MutableStateFlow(true)
    val serverRunning: StateFlow<Boolean> = _serverRunning

    private val _status = MutableStateFlow(StatusSnapshot())
    val status: StateFlow<StatusSnapshot> = _status

    fun startEngine() {
        waEngine.start()
    }

    fun startPairingCode(phoneNumber: String) {
        waEngine.startPairingCode(phoneNumber)
    }

    fun stopEngine() {
        waEngine.stop()
    }

    fun sendText(number: String, message: String, callback: ((Boolean) -> Unit)? = null) {
        waEngine.sendTextMessage(number, message, callback)
        DataRepository.logInbox(InboxMessage(
            from = number, contactName = number, phoneNumber = number,
            body = message, direction = "out", source = "manual"
        ))
    }

    fun logout() {
        waEngine.logout()
    }

    fun refreshQr() {
        waEngine.refreshQr()
    }

    fun refreshPairingCode(phoneNumber: String) {
        waEngine.refreshPairingCode(phoneNumber)
    }

    // ── Incoming message handler ──
    private fun handleIncoming(from: String, name: String, body: String, replyJid: String) {
        if (body.isBlank()) return
        val number = from.substringBefore("@").substringBefore(":")

        // Log incoming
        DataRepository.logInbox(InboxMessage(
            from = from, contactName = name, phoneNumber = number,
            body = body, direction = "in", source = "ai"
        ))

        // Save contact
        DataRepository.addContact(Contact(name = name, phoneNumber = number))

        // Track chat history — synchronized to avoid concurrent modification
        val history = chatHistories.getOrPut(number) { mutableListOf() }
        synchronized(history) {
            history.add(ChatHistoryEntry("user", body))
        }

        // Auto-reply if enabled
        val settings = DataRepository.settings.value
        if (!settings.autoReplyEnabled) return

        scope.launch {
            try {
                val result = agent.run(body, name, number, history)
                if (result.reply.isNotBlank()) {
                    synchronized(history) {
                        history.add(ChatHistoryEntry("assistant", result.reply))
                        // Trim history
                        if (history.size > 20) {
                            chatHistories[number] = history.takeLast(20).toMutableList()
                        }
                    }
                    // Send reply using replyJid (handles LID properly)
                    waEngine.sendTextMessage(replyJid, result.reply)
                    // Log outgoing
                    DataRepository.logInbox(InboxMessage(
                        from = from, contactName = name, phoneNumber = number,
                        body = result.reply, direction = "out", source = result.source
                    ))
                }
            } catch (e: Exception) {
                DataRepository.log("error", "Auto-reply error: ${e.message}")
            }
        }
    }

    // ── Test chat (for UI) ──
    suspend fun testChat(message: String, history: List<ChatHistoryEntry>): String {
        return agent.testChat(message, history)
    }
}
