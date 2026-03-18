package com.example.alarmfm.ui

import android.media.RingtoneManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.alarmfm.data.Alarm
import com.example.alarmfm.databinding.ItemAlarmBinding
import com.example.alarmfm.model.RadioStations
import com.example.alarmfm.model.SoundOptions
import java.util.Locale

class AlarmAdapter(
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onEdit:   (Alarm) -> Unit,
    private val onDelete: (Alarm) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemAlarmBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(alarm: Alarm) {
            b.tvTime.text  = String.format(Locale.getDefault(),
                "%02d:%02d", alarm.hour, alarm.minute)
            b.tvLabel.text = alarm.label.ifEmpty { "Alarm" }

            // Radio chip
            val station = RadioStations.findById(alarm.stationId)
            b.chipStation.text = station.nameHebrew

            // Fallback chip
            if (alarm.fallbackSoundUri == SoundOptions.URI_NONE) {
                b.chipFallback.visibility = View.GONE
            } else {
                b.chipFallback.visibility = View.VISIBLE
                b.chipFallback.text = when (alarm.fallbackSoundUri) {
                    SoundOptions.URI_DEFAULT -> "Default ringtone"
                    else -> try {
                        RingtoneManager.getRingtone(
                            itemView.context, Uri.parse(alarm.fallbackSoundUri)
                        ).getTitle(itemView.context)
                    } catch (_: Exception) { "Custom sound" }
                }
            }

            b.switchEnabled.setOnCheckedChangeListener(null)
            b.switchEnabled.isChecked = alarm.isEnabled
            b.switchEnabled.setOnCheckedChangeListener { _, checked ->
                onToggle(alarm, checked)
            }
            b.root.setOnClickListener { onEdit(alarm) }
            b.btnDelete.setOnClickListener { onDelete(alarm) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Alarm>() {
            override fun areItemsTheSame(a: Alarm, b: Alarm) = a.id == b.id
            override fun areContentsTheSame(a: Alarm, b: Alarm) = a == b
        }
    }
}
