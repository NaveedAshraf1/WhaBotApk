package com.example.whabotpro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.whabotpro.data.store.DataRepository
import com.example.whabotpro.engine.EngineManager
import com.example.whabotpro.engine.NodeJSEngine

class WhaBotApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        DataRepository.init(this)
        EngineManager.init(this)
        NodeJSEngine.start(this)
        // Auto-start the WhatsApp engine (polls embedded Node.js Baileys server)
        EngineManager.startEngine()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_SERVICE,
            "WhaBotPro Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps WhatsApp connection alive in background"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_SERVICE = "whabot_service"
        lateinit var instance: WhaBotApp
            private set
    }
}
