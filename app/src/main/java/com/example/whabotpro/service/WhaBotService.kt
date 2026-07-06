package com.example.whabotpro.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.whabotpro.MainActivity
import com.example.whabotpro.R
import com.example.whabotpro.WhaBotApp
import com.example.whabotpro.data.model.WaState
import com.example.whabotpro.data.store.DataRepository
import com.example.whabotpro.engine.EngineManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the WhatsApp engine running in the background.
 * Uses LifecycleService for coroutine support with lifecycle awareness.
 */
class WhaBotService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification("Starting..."))

        // Start WhatsApp engine (embedded Node.js Baileys server)
        EngineManager.startEngine()

        // Update notification when state changes
        lifecycleScope.launch {
            EngineManager.waState.collect { state ->
                val text = when (state) {
                    WaState.CONNECTED -> "Connected: ${EngineManager.connectedUser.value.ifEmpty { "WhatsApp" }}"
                    WaState.QR_READY -> "Waiting for QR scan..."
                    WaState.CODE_READY -> "Waiting for pairing code..."
                    WaState.CONNECTING -> "Connecting to WhatsApp..."
                    WaState.ERROR -> "Connection error"
                    WaState.DISCONNECTED -> "Disconnected"
                }
                notify(buildNotification(text))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        EngineManager.stopEngine()
        super.onDestroy()
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT
        val pi = PendingIntent.getActivity(this, 0, intent, pendingFlags)

        return NotificationCompat.Builder(this, WhaBotApp.CHANNEL_SERVICE)
            .setContentTitle("WhaBotPro")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun notify(notification: Notification) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIF_ID, notification)
    }

    companion object {
        const val NOTIF_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, WhaBotService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WhaBotService::class.java))
        }
    }
}
