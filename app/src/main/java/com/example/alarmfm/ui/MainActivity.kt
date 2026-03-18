// ui/MainActivity.kt
package com.example.alarmfm.ui

import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.alarmfm.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AlarmViewModel by viewModels { AlarmViewModelFactory(application) }
    private lateinit var adapter: AlarmAdapter

    // Android 13+ notification permission
    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {}

    // Launcher that re-checks alarm permission after user returns from Settings
    private val alarmPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        // User came back from Settings — check if they granted it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = getSystemService(AlarmManager::class.java)
            if (!mgr.canScheduleExactAlarms()) {
                // Still not granted — show the dialog again
                showAlarmPermissionDialog()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        observeAlarms()

        binding.fabAdd.setOnClickListener {
            startActivity(AddEditAlarmActivity.newIntent(this))
        }
    }

    override fun onResume() {
        super.onResume()
        // Ask for permissions every time the app comes to foreground
        // until they are all granted
        requestNotificationPermission()
        requestExactAlarmPermission()
        requestIgnoreBatteryOptimization()
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = getSystemService(AlarmManager::class.java)
            if (!mgr.canScheduleExactAlarms()) {
                showAlarmPermissionDialog()
            }
        }
    }

    private fun showAlarmPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Allow setting alarms")
            .setMessage(
                "AlarmFM needs the \"Alarms & reminders\" permission to wake you up. " +
                        "Tap Allow to enable it in Settings."
            )
            .setCancelable(false)   // force the user to make a choice
            .setPositiveButton("Allow") { _, _ ->
                alarmPermLauncher.launch(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName")
                    )
                )
            }
            .setNegativeButton("Not now", null)
            .show()
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(android.os.PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this)
                    .setTitle("Disable battery optimization")
                    .setMessage(
                        "To make sure alarms fire reliably in the background, " +
                                "please tap Allow and select \"Unrestricted\"."
                    )
                    .setPositiveButton("Allow") { _, _ ->
                        startActivity(Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:$packageName")
                        ))
                    }
                    .setNegativeButton("Not now", null)
                    .show()
            }
        }
    }

    // ── UI setup ──────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = AlarmAdapter(
            onToggle = { alarm, enabled -> viewModel.toggleAlarm(alarm, enabled) },
            onEdit   = { alarm -> startActivity(AddEditAlarmActivity.editIntent(this, alarm.id)) },
            onDelete = { alarm -> viewModel.deleteAlarm(alarm) }
        )
        binding.recyclerAlarms.layoutManager = LinearLayoutManager(this)
        binding.recyclerAlarms.adapter = adapter
    }

    private fun observeAlarms() {
        lifecycleScope.launch {
            viewModel.alarms.collect { list ->
                adapter.submitList(list)
                binding.tvEmpty.visibility =
                    if (list.isEmpty()) android.view.View.VISIBLE
                    else                android.view.View.GONE
            }
        }
    }
}