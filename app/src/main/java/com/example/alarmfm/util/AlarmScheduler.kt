package com.example.alarmfm.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.alarmfm.data.Alarm
import com.example.alarmfm.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, alarm: Alarm) {
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !mgr.canScheduleExactAlarms()) {
            // Don't silently fail — log it so you can see it
            android.util.Log.e(
                "AlarmScheduler",
                "Cannot schedule exact alarms — permission not granted!"
            )
            return
        }

        android.util.Log.d(
            "AlarmScheduler",
            "Scheduling alarm ${alarm.id} at ${alarm.hour}:${alarm.minute}"
        )

        mgr.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTriggerTime(alarm),
            buildPi(context, alarm)
        )
    }

    fun cancel(context: Context, alarm: Alarm) {
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mgr.cancel(buildPi(context, alarm))
    }

    fun scheduleSnooze(context: Context, alarmId: Int, snoozeMins: Int) {
        val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + snoozeMins * 60_000L
        val pi = PendingIntent.getBroadcast(
            context, alarmId + 10_000,
            Intent(context, AlarmReceiver::class.java).putExtra("alarm_id", alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    private fun buildPi(context: Context, alarm: Alarm): PendingIntent =
        PendingIntent.getBroadcast(
            context, alarm.id,
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarm.id)
                putExtra("station_id", alarm.stationId)
                putExtra("label", alarm.label)
                putExtra("vibrate", alarm.vibrate)
                putExtra("fallback_uri", alarm.fallbackSoundUri)
                putExtra("snooze_mins", alarm.snoozeDurationMin)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun nextTriggerTime(alarm: Alarm): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis())
                add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
}
