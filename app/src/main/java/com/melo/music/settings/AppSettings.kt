package com.melo.music.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Простые настройки приложения (реактивные для Compose). */
object AppSettings {

    private const val PREFS = "melo_settings"
    private const val KEY_KARAOKE = "karaoke_lyrics"
    private const val KEY_SEEN_WELCOME = "seen_welcome"

    private var prefs: SharedPreferences? = null

    /** Караоке-подсветка текста: слова в активной строке загораются по времени. */
    var karaoke by mutableStateOf(false)
        private set

    /** Видел ли пользователь экран приветствия/входа. */
    var seenWelcome by mutableStateOf(false)
        private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        karaoke = prefs?.getBoolean(KEY_KARAOKE, false) ?: false
        seenWelcome = prefs?.getBoolean(KEY_SEEN_WELCOME, false) ?: false
    }

    fun setSeenWelcome() {
        seenWelcome = true
        prefs?.edit()?.putBoolean(KEY_SEEN_WELCOME, true)?.apply()
    }

    fun updateKaraoke(value: Boolean) {
        karaoke = value
        prefs?.edit()?.putBoolean(KEY_KARAOKE, value)?.apply()
    }
}
