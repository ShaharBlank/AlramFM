package com.example.alarmfm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.alarmfm.data.AlarmDatabase
import com.example.alarmfm.service.AlarmService
import com.example.alarmfm.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // Re-schedule all enabled alarms after device reboot
            Intent.ACTION_BOOT_COMPLETED -> rescheduleAll(context)
            // Normal alarm trigger from AlarmManager
            else -> triggerAlarm(context, intent)
        }
    }

    // ── fire the alarm ────────────────────────────────────────────────────
    private fun triggerAlarm(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id",     intent.getIntExtra("alarm_id", -1))
            putExtra("station_id",   intent.getStringExtra("station_id") ?: "galgalatz")
            putExtra("label",        intent.getStringExtra("label") ?: "")
            putExtra("vibrate",      intent.getBooleanExtra("vibrate", true))
            putExtra("fallback_uri", intent.getStringExtra("fallback_uri") ?: "DEFAULT")
            putExtra("snooze_mins",  intent.getIntExtra("snooze_mins", 10))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    // ── re-schedule after reboot ──────────────────────────────────────────
    private fun rescheduleAll(context: Context) {
        val pending = goAsync()   // keep BroadcastReceiver alive for async work
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmDatabase.getInstance(context)
                    .alarmDao()
                    .getEnabledAlarms()
                    .forEach { alarm -> AlarmScheduler.schedule(context, alarm) }
            } finally {
                pending.finish()
            }
        }
    }
}
