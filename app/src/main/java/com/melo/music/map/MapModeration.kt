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
    private const val KEY_MY = "my_drops"

    private var prefs: SharedPreferences? = null
    private val hidden = HashSet<String>()
    private val myDrops = HashSet<String>()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs?.getStringSet(KEY, emptySet())?.let { hidden.addAll(it) }
        prefs?.getStringSet(KEY_MY, emptySet())?.let { myDrops.addAll(it) }
    }

    fun isHidden(id: String): Boolean = hidden.contains(id)

    @Synchronized
    fun hide(id: String) {
        if (hidden.add(id)) prefs?.edit()?.putStringSet(KEY, HashSet(hidden))?.apply()
    }

    fun isMyDrop(id: String, ownerId: String? = null): Boolean {
        if (myDrops.contains(id)) return true
        val cur = AppwriteService.userId
        if (!cur.isNullOrBlank() && !ownerId.isNullOrBlank() && cur == ownerId) return true
        return false
    }

    @Synchronized
    fun markMyDrop(id: String) {
        if (myDrops.add(id)) prefs?.edit()?.putStringSet(KEY_MY, HashSet(myDrops))?.apply()
    }

    @Synchronized
    fun unmarkMyDrop(id: String) {
        if (myDrops.remove(id)) prefs?.edit()?.putStringSet(KEY_MY, HashSet(myDrops))?.apply()
    }
}
