package com.melo.music.auth

import androidx.activity.ComponentActivity

/**
 * Flavor ruStore: заглушка. Google Sign-In недоступен.
 */
object GoogleAuthHelper {

    fun init(activity: ComponentActivity) {
        // Нет Google-зависимостей — ничего не делаем.
    }

    suspend fun signIn(activity: ComponentActivity): Result<Unit> {
        return Result.failure(Exception("Вход через Google недоступен в этой версии"))
    }
}
