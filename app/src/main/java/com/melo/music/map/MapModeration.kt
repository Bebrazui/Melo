package com.melo.music.map

import android.content.Context
import android.content.SharedPreferences

/**
 * Локальное скрытие пинов, на которые пользователь пожаловался или которые он скрыл.
 * Скрытые пины сразу пропадают из выдачи у этого пользователя (серверная модерация —
 * через ревью коллекции reports).
 */
object MapModeration {

    private const val PREFS = "melo_map_moderation"
    private const val KEY = "hidden"

    private var prefs: SharedPreferences? = null
    private val hidden = HashSet<String>()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs?.getStringSet(KEY, emptySet())?.let { hidden.addAll(it) }
    }

    fun isHidden(id: String): Boolean = hidden.contains(id)

    @Synchronized
    fun hide(id: String) {
        if (hidden.add(id)) prefs?.edit()?.putStringSet(KEY, HashSet(hidden))?.apply()
    }
}
