// service/AlarmService.kt
package com.example.alarmfm.service

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.alarmfm.R
import com.example.alarmfm.model.RadioStations
import com.example.alarmfm.model.SoundOptions
import com.example.alarmfm.ui.AlarmRingActivity

class AlarmService : Service() {

    private var player: ExoPlayer? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var isFallbackActive = false
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        const val CHANNEL_ID     = "alarmfm_channel"
        const val NOTIF_ID       = 1001
        const val ACTION_DISMISS = "com.example.alarmfm.DISMISS"
        const val ACTION_SNOOZE  = "com.example.alarmfm.SNOOZE"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> { stopAlarm(); return START_NOT_STICKY }
            ACTION_SNOOZE  -> { snooze(intent); return START_NOT_STICKY }
        }

        val alarmId     = intent?.getIntExtra("alarm_id", -1) ?: -1
        val stationId   = intent?.getStringExtra("station_id")   ?: "galgalatz"
        val label       = intent?.getStringExtra("label")        ?: ""
        val fallbackUri = intent?.getStringExtra("fallback_uri") ?: SoundOptions.URI_DEFAULT
        val vibrate     = intent?.getBooleanExtra("vibrate", true) ?: true
        val snoozeMins  = intent?.getIntExtra("snooze_mins", 10) ?: 10

        createNotificationChannel()

        // ↓ The notification itself launches the full-screen ring activity
        startForeground(NOTIF_ID, buildNotification(label, alarmId, snoozeMins, stationId))

        startStreaming(stationId, fallbackUri)
        if (vibrate) startVibration()

        return START_NOT_STICKY
    }

    // ── Notification with full-screen intent ──────────────────────────────────
    private fun buildNotification(
        label: String,
        alarmId: Int,
        snoozeMins: Int,
        stationId: String
    ): Notification {

        // Full-screen intent = what launches AlarmRingActivity over the lock screen
        val fullScreenIntent = Intent(this, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("alarm_id",   alarmId)
            putExtra("station_id", stationId)
            putExtra("label",      label)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this, alarmId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissPi = PendingIntent.getService(
            this, 0,
            Intent(this, AlarmService::class.java).apply { action = ACTION_DISMISS },
            PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePi = PendingIntent.getService(
            this, 1,
            Intent(this, AlarmService::class.java).apply {
                action = ACTION_SNOOZE
                putExtra("alarm_id",    alarmId)
                putExtra("snooze_mins", snoozeMins)
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("AlarmFM")
            .setContentText(label.ifEmpty { "Alarm ringing" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // show on lock screen
            .setOngoing(true)
            .setAutoCancel(false)
            // ↓ This is the key — triggers AlarmRingActivity as a full-screen overlay
            .setFullScreenIntent(fullScreenPi, true)
            .addAction(0, "Snooze",  snoozePi)
            .addAction(0, "Dismiss", dismissPi)
            .build()
    }

    // ── Streaming ─────────────────────────────────────────────────────────────
    private fun startStreaming(stationId: String, fallbackUri: String) {
        val url = RadioStations.findById(stationId).streamUrl
        player = ExoPlayer.Builder(this).build().also { exo ->
            exo.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    if (!isFallbackActive) activateFallback(fallbackUri)
                }
            })
            exo.setMediaItem(MediaItem.fromUri(url))
            exo.prepare()
            exo.play()
        }
        handler.postDelayed({
            if (player?.isPlaying == false && !isFallbackActive)
                activateFallback(fallbackUri)
        }, 10_000L)
    }

    fun activateFallback(fallbackUri: String) {
        isFallbackActive = true
        player?.stop(); player?.release(); player = null
        if (fallbackUri == SoundOptions.URI_NONE) return
        val uri = SoundOptions.resolveUri(fallbackUri)
        ringtone = RingtoneManager.getRingtone(this, uri)?.also { rt ->
            rt.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) rt.isLooping = true
            rt.play()
        }
    }

    // ── Vibration ─────────────────────────────────────────────────────────────
    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 500, 500, 500, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    // ── Dismiss / Snooze ──────────────────────────────────────────────────────
    fun stopAlarm() {
        handler.removeCallbacksAndMessages(null)
        player?.stop(); player?.release(); player = null
        ringtone?.stop(); ringtone = null
        vibrator?.cancel(); vibrator = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun snooze(intent: Intent) {
        handler.removeCallbacksAndMessages(null)
        player?.stop(); ringtone?.stop(); vibrator?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Notification channel ──────────────────────────────────────────────────
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "Alarm", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AlarmFM ringing alerts"
                // Allow this channel to show as a full-screen overlay
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(ch)
        }
    }

    override fun onBind(intent: Intent?) = null
    override fun onDestroy() { super.onDestroy(); stopAlarm() }
}