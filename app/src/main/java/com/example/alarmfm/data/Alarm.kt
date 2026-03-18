package com.example.alarmfm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDays: String = "",       // "MON,WED,FRI" or "" = once
    val stationId: String = "galgalatz",
    // "NONE" = no fallback (mute if stream fails)
    // "DEFAULT" = system default alarm tone
    // Any other value = RingtoneManager URI string
    val fallbackSoundUri: String = "DEFAULT",
    val snoozeDurationMin: Int = 10,
    val vibrate: Boolean = true
)
