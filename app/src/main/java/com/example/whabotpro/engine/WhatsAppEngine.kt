package com.example.whabotpro.engine

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.whabotpro.data.model.WaState
import com.example.whabotpro.data.store.DataRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * WhatsApp engine backed by a Baileys desktop/server.
 *
 * This engine no longer uses a hidden WebView. Instead it calls a local Node.js
 * Baileys server over HTTP to request pairing codes, check status, and send messages.
 *
 * The server URL is read from AppSettings.baileysServerUrl.
 */
class WhatsAppEngine(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private val _state = MutableStateFlow(WaState.DISCONNECTED)
    val state: StateFlow<WaState> = _state

    private val _qrCode = MutableStateFlow<String?>(null)
    val qrCode: StateFlow<String?> = _qrCode

    private val _pairingCode = MutableStateFlow<String?>(null)
    val pairingCode: StateFlow<String?> = _pairingCode

    private val _pairingPhoneNumber = MutableStateFlow("")
    val pairingPhoneNumber: StateFlow<String> = _pairingPhoneNumber

    private val _connectedUser = MutableStateFlow("")
    val connectedUser: StateFlow<String> = _connectedUser

    private val _isRequestingPairing = MutableStateFlow(false)
    val isRequestingPairing: StateFlow<Boolean> = _isRequestingPairing

    // Callback for incoming messages — replyJid is the JID to use when replying (handles LID properly)
    var onIncomingMessage: ((from: String, name: String, body: String, replyJid: String) -> Unit)? = null

    private var pollRunnable: Runnable? = null
    private var pendingPairingRequest: Call? = null
    private var isRunning = false

    private val serverUrl: String
        get() = DataRepository.settings.value.baileysServerUrl.trim().trimEnd('/')

    fun start() {
        if (isRunning) return
        isRunning = true
        _state.value = WaState.CONNECTING
        startPolling()
        DataRepository.log("info", "Baileys engine started, server: $serverUrl")
    }

    private fun startPolling() {
        pollRunnable?.let { handler.removeCallbacks(it) }
        val runnable = object : Runnable {
            override fun run() {
                if (!isRunning) return
                checkStatus()
                checkMessages()
                handler.postDelayed(this, POLL_INTERVAL)
            }
        }
        pollRunnable = runnable
        handler.post(runnable)
    }

    private fun checkMessages() {
        val url = "$serverUrl/api/messages"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                DataRepository.log("error", "Message poll failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: return
                handler.post {
                    try {
                        val data = parseMap(body)
                        @Suppress("UNCHECKED_CAST")
                        val messages = data["messages"] as? List<Map<String, Any?>>
                        if (messages == null) {
                            DataRepository.log("warn", "No messages in response")
                            return@post
                        }
                        for (msg in messages) {
                            val from = msg["from"] as? String ?: ""
                            val name = msg["name"] as? String ?: from
                            val text = msg["body"] as? String ?: ""
                            if (text.isNotEmpty()) {
                                onIncomingMessage?.invoke(from, name, text, from)
                            }
                        }
                    } catch (e: Exception) {
                        DataRepository.log("error", "Messages parse error: ${e.message}")
                    }
                }
            }
        })
    }

    private fun checkStatus() {
        val url = "$serverUrl/api/status"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler.post {
                    if (_state.value != WaState.CONNECTED && _state.value != WaState.CODE_READY) {
                        _state.value = WaState.ERROR
                    }
                    DataRepository.log("error", "Baileys server unreachable: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                handler.post {
                    try {
                        val data = parseMap(body)
                        val stateStr = data["state"] as? String ?: "disconnected"
                        val code = data["pairingCode"] as? String
                        val newState = when (stateStr) {
                            "connected" -> WaState.CONNECTED
                            "connecting" -> WaState.CONNECTING
                            "disconnected" -> if (_state.value == WaState.CODE_READY) WaState.CODE_READY else WaState.DISCONNECTED
                            else -> _state.value
                        }

                        // If the server already has a pairing code for our phone, show it
                        if (!code.isNullOrBlank()) {
                            _pairingCode.value = code
                            _state.value = WaState.CODE_READY
                        } else if (newState != WaState.CODE_READY) {
                            _state.value = newState
                        }

                        // We don't have a user name from the server; keep empty
                        if (newState == WaState.CONNECTED) {
                            _connectedUser.value = ""
                        }
                    } catch (e: Exception) {
                        DataRepository.log("error", "Baileys status parse error: ${e.message}")
                    }
                }
            }
        })
    }

    fun startPairingCode(phoneNumber: String) {
        if (_isRequestingPairing.value) return
        _isRequestingPairing.value = true
        _pairingPhoneNumber.value = phoneNumber
        _pairingCode.value = null
        _qrCode.value = null
        _state.value = WaState.CONNECTING

        val url = "$serverUrl/api/pairing-code"
        val body = gson.toJson(mapOf("phone" to phoneNumber)).toRequestBody(jsonType)
        val request = Request.Builder().url(url).post(body).build()

        pendingPairingRequest?.cancel()
        pendingPairingRequest = client.newCall(request)
        pendingPairingRequest?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler.post {
                    _isRequestingPairing.value = false
                    _state.value = WaState.ERROR
                    DataRepository.log("error", "Pairing code request failed: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string()
                handler.post {
                    _isRequestingPairing.value = false
                    try {
                        val data = parseMap(bodyString)
                        if (data["success"] == true) {
                            val code = data["code"] as? String
                            if (!code.isNullOrBlank()) {
                                _pairingCode.value = code
                                _state.value = WaState.CODE_READY
                                DataRepository.log("info", "Pairing code received: $code")
                            } else {
                                _state.value = WaState.ERROR
                                DataRepository.log("error", "Pairing code empty")
                            }
                        } else {
                            _state.value = WaState.ERROR
                            DataRepository.log("error", "Pairing code error: ${data["error"]}")
                        }
                    } catch (e: Exception) {
                        _state.value = WaState.ERROR
                        DataRepository.log("error", "Pairing code parse error: ${e.message}")
                    }
                }
            }
        })
    }

    fun refreshPairingCode(phoneNumber: String) {
        startPairingCode(phoneNumber)
    }

    fun refreshQr() {
        // Baileys server does not expose QR; use pairing code instead
        DataRepository.log("info", "QR refresh not supported with Baileys backend")
    }

    fun logout() {
        val url = "$serverUrl/api/logout"
        val request = Request.Builder().url(url).post("{}".toRequestBody(jsonType)).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                resetState()
            }
            override fun onResponse(call: Call, response: Response) {
                handler.post { resetState() }
            }
        })
    }

    private fun resetState() {
        _state.value = WaState.CONNECTING
        _qrCode.value = null
        _pairingCode.value = null
        _pairingPhoneNumber.value = ""
        _connectedUser.value = ""
    }

    fun stop() {
        isRunning = false
        pollRunnable?.let { handler.removeCallbacks(it) }
        pendingPairingRequest?.cancel()
        _state.value = WaState.DISCONNECTED
    }

    fun sendTextMessage(number: String, message: String, callback: ((Boolean) -> Unit)? = null) {
        val url = "$serverUrl/api/send-text"
        val body = gson.toJson(mapOf("number" to number, "message" to message)).toRequestBody(jsonType)
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                DataRepository.log("error", "Send failed: ${e.message}")
                callback?.let { handler.post { it(false) } }
            }

            override fun onResponse(call: Call, response: Response) {
                val ok = response.isSuccessful
                if (!ok) DataRepository.log("error", "Send failed: ${response.code}")
                callback?.let { handler.post { it(ok) } }
            }
        })
    }

    private fun parseMap(json: String?): Map<String, Any?> {
        if (json.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, Any?>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    companion object {
        private const val TAG = "WhatsAppEngine"
        private const val POLL_INTERVAL = 2000L
    }
}
