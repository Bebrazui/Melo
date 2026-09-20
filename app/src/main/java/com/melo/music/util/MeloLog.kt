package com.melo.music.util

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Логгер для отображения диагностических сообщений прямо в UI.
 */
object MeloLog {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val entries = mutableStateListOf<String>()

    fun d(tag: String, msg: String) {
        val time = timeFormat.format(Date())
        val line = "[$time] [$tag] $msg"
        android.util.Log.d(tag, msg)
        FileLog.d(tag, msg)
        addEntry(line)
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        val time = timeFormat.format(Date())
        val err = if (tr != null) " -> ${tr.javaClass.simpleName}: ${tr.message}" else ""
        val line = "[$time] [ERROR/$tag] $msg$err"
        android.util.Log.e(tag, msg, tr)
        FileLog.e(tag, msg, tr)
        addEntry(line)
    }

    fun clear() {
        entries.clear()
    }

    private fun addEntry(entry: String) {
        if (entries.size > 200) {
            entries.removeAt(0)
        }
        entries.add(entry)
    }
}
