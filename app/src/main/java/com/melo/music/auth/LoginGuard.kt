package com.melo.music.auth

import android.content.Context
import android.content.SharedPreferences

/**
 * Локальный анти-брутфорс: 5 неудачных входов подряд → блок входа на 3 часа.
 * (Серверный IP-лимит обеспечивает сам Appwrite — это дополнительный барьер на устройстве.)
 */
object LoginGuard {

    private const val PREFS = "melo_login_guard"
    private const val KEY_FAILS = "fails"
    private const val KEY_LOCK = "lock_until"
    private const val MAX_FAILS = 5
    private const val LOCK_MS = 3 * 60 * 60 * 1000L

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /** Сколько миллисекунд ещё длится блокировка (0 — не заблокировано). */
    fun lockedRemainingMs(): Long {
        val until = prefs?.getLong(KEY_LOCK, 0L) ?: 0L
        val rem = until - System.currentTimeMillis()
        return if (rem > 0) rem else 0L
    }

    fun recordFailure() {
        val p = prefs ?: return
        val fails = p.getInt(KEY_FAILS, 0) + 1
        if (fails >= MAX_FAILS) {
            p.edit().putLong(KEY_LOCK, System.currentTimeMillis() + LOCK_MS).putInt(KEY_FAILS, 0).apply()
        } else {
            p.edit().putInt(KEY_FAILS, fails).apply()
        }
    }

    fun reset() {
        prefs?.edit()?.putInt(KEY_FAILS, 0)?.putLong(KEY_LOCK, 0L)?.apply()
    }
}
