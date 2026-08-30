package com.melo.music.settings

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Простые настройки приложения (реактивные для Compose). */
object AppSettings {

    private const val PREFS = "melo_settings"
    private const val KEY_KARAOKE = "karaoke_lyrics"
    private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
    private const val KEY_AUTO_DOWNLOAD_FAVORITES = "auto_download_favorites"
    private const val KEY_SEEN_WELCOME = "seen_welcome"
    private const val KEY_LAUNCHER_ICON = "launcher_icon"

    private var prefs: SharedPreferences? = null

    /** Караоке-подсветка текста: слова в активной строке загораются по времени. */
    var karaoke by mutableStateOf(false)
        private set

    /** Тактильный отклик и звуковой щелчок при нажатии кнопок. */
    var hapticFeedback by mutableStateOf(true)
        private set

    /** Автоматически скачивать избранные треки для офлайн-прослушивания. */
    var autoDownloadFavorites by mutableStateOf(false)
        private set

    /** Видел ли пользователь экран приветствия/входа. */
    var seenWelcome by mutableStateOf(false)
        private set

    /** Текущий id иконки лаунчера (ключ из [IconPreset]). */
    var launcherIcon by mutableStateOf(IconPreset.DEFAULT.id)
        private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        karaoke = prefs?.getBoolean(KEY_KARAOKE, false) ?: false
        hapticFeedback = prefs?.getBoolean(KEY_HAPTIC_FEEDBACK, true) ?: true
        autoDownloadFavorites = prefs?.getBoolean(KEY_AUTO_DOWNLOAD_FAVORITES, false) ?: false
        seenWelcome = prefs?.getBoolean(KEY_SEEN_WELCOME, false) ?: false
        launcherIcon = prefs?.getString(KEY_LAUNCHER_ICON, IconPreset.DEFAULT.id) ?: IconPreset.DEFAULT.id
    }

    fun setSeenWelcome() {
        seenWelcome = true
        prefs?.edit()?.putBoolean(KEY_SEEN_WELCOME, true)?.apply()
    }

    fun updateKaraoke(value: Boolean) {
        karaoke = value
        prefs?.edit()?.putBoolean(KEY_KARAOKE, value)?.apply()
    }

    fun updateHapticFeedback(value: Boolean) {
        hapticFeedback = value
        prefs?.edit()?.putBoolean(KEY_HAPTIC_FEEDBACK, value)?.apply()
    }

    fun updateAutoDownloadFavorites(value: Boolean) {
        autoDownloadFavorites = value
        prefs?.edit()?.putBoolean(KEY_AUTO_DOWNLOAD_FAVORITES, value)?.apply()
    }

    /**
     * Переключить иконку лаунчера.
     * Вызывать из [Context], напр. из Activity.
     */
    fun switchIcon(context: Context, presetId: String) {
        val pm = context.packageManager
        val pkg = context.packageName

        // Отключаем основную activity (убираем из лаунчера).
        pm.setComponentEnabledSetting(
            ComponentName(pkg, "$pkg.MainActivity"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )

        // Отключаем ВСЕ алиасы.
        for (alias in IconPreset.entries) {
            if (alias == IconPreset.DEFAULT) continue
            pm.setComponentEnabledSetting(
                ComponentName(pkg, "$pkg.${alias.componentName}"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }

        // Включаем выбранный алиас (или основную activity для DEFAULT).
        if (presetId == IconPreset.DEFAULT.id) {
            pm.setComponentEnabledSetting(
                ComponentName(pkg, "$pkg.MainActivity"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        } else {
            val selected = IconPreset.entries.first { it.id == presetId }
            pm.setComponentEnabledSetting(
                ComponentName(pkg, "$pkg.${selected.componentName}"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }

        launcherIcon = presetId
        prefs?.edit()?.putString(KEY_LAUNCHER_ICON, presetId)?.apply()
    }
}

/** Набор встроенных иконок лаунчера. */
enum class IconPreset(
    val id: String,
    val label: String,
    val componentName: String,
) {
    DEFAULT("default", "Стандартная", "MainActivity"),
    THORNS("thorns", "Колючка", "LauncherAliasThorns"),
    INVERTED("inverted", "Инвертированный", "LauncherAliasInverted"),
    IOS6("ios6", "iOS 6", "LauncherAliasIos6"),
}
