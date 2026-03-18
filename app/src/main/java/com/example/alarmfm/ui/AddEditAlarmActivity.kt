package com.example.alarmfm.ui

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Toast
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.alarmfm.data.Alarm
import com.example.alarmfm.databinding.ActivityAddEditAlarmBinding
import com.example.alarmfm.model.RadioStations
import com.example.alarmfm.model.SoundOption
import com.example.alarmfm.model.SoundOptions
import kotlinx.coroutines.launch

class AddEditAlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditAlarmBinding
    private val viewModel: AlarmViewModel by viewModels {
        AlarmViewModelFactory(application)
    }

    private var editAlarmId = -1
    private var selectedStationIdx = 0
    private lateinit var sounds: List<SoundOption>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editAlarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        title = if (editAlarmId == -1) "Add Alarm" else "Edit Alarm"

        setupStationSpinner()
        setupFallbackSpinner()

        if (editAlarmId != -1) loadExistingAlarm()

        binding.btnSave.setOnClickListener { saveAlarm() }
    }

    // ── spinners ──────────────────────────────────────────────────────────
    private fun setupStationSpinner() {
        val names = RadioStations.list.map { it.nameHebrew }
        binding.spinnerStation.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, names
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerStation.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    selectedStationIdx = pos
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
    }

    private fun setupFallbackSpinner() {
        sounds = SoundOptions.load(this)
        val names = sounds.map { it.title }
        binding.spinnerFallback.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, names
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        // Preview button
        binding.btnPreviewFallback.setOnClickListener {
            val sel = sounds[binding.spinnerFallback.selectedItemPosition]
            if (sel.uri == SoundOptions.URI_NONE) return@setOnClickListener
            val uri = SoundOptions.resolveUri(sel.uri)
            RingtoneManager.getRingtone(this, uri)?.also { rt ->
                rt.play()
                Handler(Looper.getMainLooper()).postDelayed({ rt.stop() }, 3_000)
            }
        }
    }

    // ── load existing alarm when editing ──────────────────────────────────
    private fun loadExistingAlarm() {
        lifecycleScope.launch {
            val alarm = viewModel.getAlarmById(editAlarmId) ?: return@launch
            binding.timePicker.hour   = alarm.hour
            binding.timePicker.minute = alarm.minute
            binding.etLabel.setText(alarm.label)
            binding.switchVibrate.isChecked = alarm.vibrate

            // Restore station
            val stIdx = RadioStations.list.indexOfFirst { it.id == alarm.stationId }
            if (stIdx >= 0) binding.spinnerStation.setSelection(stIdx)

            // Restore fallback
            val fbIdx = sounds.indexOfFirst { it.uri == alarm.fallbackSoundUri }
            if (fbIdx >= 0) binding.spinnerFallback.setSelection(fbIdx)

            // Restore repeat days
            val days = alarm.repeatDays.split(",").toSet()
            binding.cbMon.isChecked = "MON" in days
            binding.cbTue.isChecked = "TUE" in days
            binding.cbWed.isChecked = "WED" in days
            binding.cbThu.isChecked = "THU" in days
            binding.cbFri.isChecked = "FRI" in days
            binding.cbSat.isChecked = "SAT" in days
            binding.cbSun.isChecked = "SUN" in days
        }
    }

    // ── save ──────────────────────────────────────────────────────────────
    private fun saveAlarm() {
        val repeatDays = buildList {
            if (binding.cbMon.isChecked) add("MON")
            if (binding.cbTue.isChecked) add("TUE")
            if (binding.cbWed.isChecked) add("WED")
            if (binding.cbThu.isChecked) add("THU")
            if (binding.cbFri.isChecked) add("FRI")
            if (binding.cbSat.isChecked) add("SAT")
            if (binding.cbSun.isChecked) add("SUN")
        }.joinToString(",")

        val alarm = Alarm(
            id               = if (editAlarmId == -1) 0 else editAlarmId,
            hour             = binding.timePicker.hour,
            minute           = binding.timePicker.minute,
            label            = binding.etLabel.text.toString().trim(),
            isEnabled        = true,
            repeatDays       = repeatDays,
            stationId        = RadioStations.list[selectedStationIdx].id,
            fallbackSoundUri = sounds[binding.spinnerFallback.selectedItemPosition].uri,
            snoozeDurationMin = 10,
            vibrate          = binding.switchVibrate.isChecked
        )

        if (editAlarmId == -1) viewModel.insertAlarm(alarm)
        else                   viewModel.updateAlarm(alarm)

        Toast.makeText(this, "Alarm saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    companion object {
        private const val EXTRA_ALARM_ID = "alarm_id"
        fun newIntent(ctx: Context) = Intent(ctx, AddEditAlarmActivity::class.java)
        fun editIntent(ctx: Context, id: Int) =
            Intent(ctx, AddEditAlarmActivity::class.java)
                .putExtra(EXTRA_ALARM_ID, id)
    }
}
