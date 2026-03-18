package com.example.alarmfm.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.alarmfm.data.Alarm
import com.example.alarmfm.data.AlarmDatabase
import com.example.alarmfm.util.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AlarmViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AlarmDatabase.getInstance(app).alarmDao()

    val alarms: Flow<List<Alarm>> = dao.getAllAlarms()

    fun insertAlarm(alarm: Alarm) = viewModelScope.launch {
        val newId = dao.insert(alarm).toInt()
        AlarmScheduler.schedule(getApplication(), alarm.copy(id = newId))
    }

    fun updateAlarm(alarm: Alarm) = viewModelScope.launch {
        dao.update(alarm)
        if (alarm.isEnabled) AlarmScheduler.schedule(getApplication(), alarm)
        else                 AlarmScheduler.cancel(getApplication(), alarm)
    }

    fun toggleAlarm(alarm: Alarm, enabled: Boolean) = viewModelScope.launch {
        val updated = alarm.copy(isEnabled = enabled)
        dao.update(updated)
        if (enabled) AlarmScheduler.schedule(getApplication(), updated)
        else         AlarmScheduler.cancel(getApplication(), updated)
    }

    fun deleteAlarm(alarm: Alarm) = viewModelScope.launch {
        AlarmScheduler.cancel(getApplication(), alarm)
        dao.delete(alarm)
    }

    suspend fun getAlarmById(id: Int): Alarm? = dao.getById(id)
}

class AlarmViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AlarmViewModel(app) as T
}
