package com.melo.music.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLog {
    private var logFile: File? = null
    private val df = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        logFile = File(dir, "melo.log")
        if ((logFile?.length() ?: 0L) > 500_000L) {
            logFile?.delete()
        }
    }

    fun d(tag: String, msg: String) {
        android.util.Log.d(tag, msg)
        append(tag, "D", msg)
    }

    fun i(tag: String, msg: String) {
        android.util.Log.i(tag, msg)
        append(tag, "I", msg)
    }

    fun w(tag: String, msg: String) {
        android.util.Log.w(tag, msg)
        append(tag, "W", msg)
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        android.util.Log.e(tag, msg, tr)
        val fullMsg = if (tr != null) "$msg: ${tr.javaClass.simpleName}: ${tr.message}" else msg
        append(tag, "E", fullMsg)
    }

    private fun append(tag: String, level: String, msg: String) {
        runCatching {
            val f = logFile ?: return
            val line = "${df.format(Date())} [$level/$tag] $msg\n"
            f.appendText(line)
        }
    }
}
