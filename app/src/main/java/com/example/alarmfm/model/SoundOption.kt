package com.example.alarmfm.model

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.net.toUri
data class SoundOption(val uri: String, val title: String)

object SoundOptions {
    const val URI_NONE = "NONE"
    const val URI_DEFAULT = "DEFAULT"

    fun load(context: Context): List<SoundOption> {
        val list = mutableListOf(
            SoundOption(URI_NONE, "None (silent if offline)"),
            SoundOption(URI_DEFAULT, "Default alarm sound")
        )
        try {
            val mgr = RingtoneManager(context).apply {
                setType(RingtoneManager.TYPE_ALARM)
            }
            val cursor = mgr.cursor
            while (cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = mgr.getRingtoneUri(cursor.position).toString()
                list.add(SoundOption(uri, title))
            }
        } catch (_: Exception) { /* device has no alarm ringtones */
        }
        return list
    }

    fun resolveUri(uriString: String): Uri =
        when (uriString) {
            URI_DEFAULT -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            else -> uriString.toUri()
        }
}
