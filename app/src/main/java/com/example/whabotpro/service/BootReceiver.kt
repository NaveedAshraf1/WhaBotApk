package com.example.whabotpro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                WhaBotService.start(context)
            } catch (e: Exception) {
                // On Android 12+ (API 31+), starting a foreground service from a
                // boot-completed broadcast throws ForegroundServiceStartNotAllowedException.
                // Log and skip — the user can start the service from the app.
                Log.e("BootReceiver", "Cannot start service on boot: ${e.message}")
            }
        }
    }
}
