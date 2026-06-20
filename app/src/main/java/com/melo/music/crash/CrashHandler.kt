package com.melo.music.crash

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Глобальный обработчик крашей: ловит необработанные исключения,
 * сохраняет лог в файл и ставит флаг для показа диалога при следующем запуске.
 */
object CrashHandler {

    private const val PREFS = "melo_crash"
    private const val KEY_HAS_CRASH = "has_crash"
    private const val KEY_CRASH_PATH = "crash_path"
    private const val KEY_CRASH_TIME = "crash_time"

    private var prevHandler: Thread.UncaughtExceptionHandler? = null

    fun install(context: Context) {
        prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val log = collectLog(thread, throwable)
                val file = saveLog(context, log)
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean(KEY_HAS_CRASH, true)
                    .putString(KEY_CRASH_PATH, file.absolutePath)
                    .putLong(KEY_CRASH_TIME, System.currentTimeMillis())
                    .apply()
            } catch (_: Exception) {
            }
            // Передаём дальше (показывает системный краш-диалог).
            prevHandler?.uncaughtException(thread, throwable)
        }
    }

    /** Проверяет, был ли краш при прошлом запуске. */
    fun hadCrash(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAS_CRASH, false)

    fun getCrashPath(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CRASH_PATH, null)

    fun getCrashTime(context: Context): String {
        val ms = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_CRASH_TIME, 0)
        if (ms == 0L) return ""
        return SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(ms))
    }

    fun clearCrash(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    private fun collectLog(thread: Thread, throwable: Throwable): String {
        val sw = StringWriter()
        sw.appendLine("=== Melo Crash Report ===")
        sw.appendLine("Thread: ${thread.name}")
        sw.appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        sw.appendLine()
        sw.appendLine(throwable.stackTraceToString())
        if (throwable.cause != null) {
            sw.appendLine("\nCaused by:")
            sw.appendLine(throwable.cause!!.stackTraceToString())
        }
        return sw.toString()
    }

    private fun saveLog(context: Context, log: String): File {
        val dir = File(context.filesDir, "crash_logs").apply { mkdirs() }
        val name = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date()) + ".txt"
        val file = File(dir, name)
        file.writeText(log)
        // Удаляем старые логи (оставляем последние 5).
        dir.listFiles()?.filter { it.extension == "txt" }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(5)
            ?.forEach { it.delete() }
        return file
    }
}
