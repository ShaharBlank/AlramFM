package com.example.alarmfm.ui

import android.content.Intent
import android.os.*
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.alarmfm.databinding.ActivityAlarmRingBinding
import com.example.alarmfm.model.RadioStations
import com.example.alarmfm.service.AlarmService
import java.text.SimpleDateFormat
import java.util.*

class AlarmRingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmRingBinding
    private val handler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            binding.tvClock.text =
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            handler.postDelayed(this, 1_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // These flags are required for showing over the lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            // Also tell the keyguard to dismiss so the activity is visible
            val km = getSystemService(android.app.KeyguardManager::class.java)
            km?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityAlarmRingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val stationId = intent.getStringExtra("station_id") ?: "galgalatz"
        val label     = intent.getStringExtra("label") ?: ""
        val station   = RadioStations.findById(stationId)

        binding.tvLabel.text   = label.ifEmpty { "Alarm" }
        binding.chipStation.text = station.nameHebrew + " • " + station.nameEnglish

        handler.post(clockTick)

        binding.btnDismiss.setOnClickListener {
            stopService(Intent(this, AlarmService::class.java))
            finish()
        }
        binding.btnSnooze.setOnClickListener {
            startService(Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_SNOOZE
                putExtra("alarm_id",    intent.getIntExtra("alarm_id", -1))
                putExtra("snooze_mins", 10)
            })
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(clockTick)
    }
}
