package com.melo.music.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.Equalizer

/**
 * Эквалайзер: обёртка над android.media.audiofx.Equalizer.
 * Привязывается к audioSessionId плеера, сохраняет настройки в SharedPreferences.
 */
object EqualizerManager {

    private const val PREFS = "melo_equalizer"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PRESET = "preset"
    private const val KEY_BANDS = "bands"

    private var prefs: SharedPreferences? = null
    private var equalizer: Equalizer? = null

    /** Количество полос эквалайзера (определяется после привязки). */
    var bandCount: Int = 0
        private set

    /** Диапазон LEVEL (в мДб) для каждой полосы. */
    var bandLevelRange: ShortArray = shortArrayOf()
        private set

    /** Частоты полос (в мГц). */
    var bandFrequencies: IntArray = intArrayOf()
        private set

    /** Названия встроенных пресетов. */
    var presetNames: Array<String> = arrayOf()
        private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /**
     * Привязать эквалайзер к сессии плеера. Вызывать при старте воспроизведения.
     */
    @Synchronized
    fun attach(audioSessionId: Int) {
        release()
        try {
            val eq = Equalizer(0, audioSessionId)
            bandCount = eq.numberOfBands.toInt()
            bandLevelRange = shortArrayOf(eq.bandLevelRange[0], eq.bandLevelRange[1])
            bandFrequencies = IntArray(bandCount) { eq.getCenterFreq(it.toShort()) / 1000 }
            presetNames = Array(eq.numberOfPresets.toInt()) { eq.getPresetName(it.toShort()) }

            // Восстанавливаем сохранённое состояние.
            val enabled = prefs?.getBoolean(KEY_ENABLED, false) ?: false
            eq.enabled = enabled

            val savedPreset = prefs?.getInt(KEY_PRESET, -1) ?: -1
            if (savedPreset in 0 until eq.numberOfPresets) {
                eq.usePreset(savedPreset.toShort())
            } else {
                val savedBands = loadBandLevels()
                if (savedBands.size == bandCount) {
                    for (i in 0 until bandCount) {
                        eq.setBandLevel(i.toShort(), savedBands[i])
                    }
                }
            }
            equalizer = eq
        } catch (_: Exception) {
            equalizer = null
        }
    }

    @Synchronized
    fun release() {
        equalizer?.release()
        equalizer = null
    }

    fun isEnabled(): Boolean = equalizer?.enabled ?: false

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        prefs?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
    }

    @Synchronized
    fun setPreset(presetIndex: Int) {
        val eq = equalizer ?: return
        if (presetIndex in 0 until eq.numberOfPresets) {
            eq.usePreset(presetIndex.toShort())
            prefs?.edit()?.putInt(KEY_PRESET, presetIndex)?.apply()
            saveBandLevels()
        }
    }

    @Synchronized
    fun setBandLevel(band: Int, level: Short) {
        val eq = equalizer ?: return
        if (band in 0 until bandCount) {
            eq.setBandLevel(band.toShort(), level)
            prefs?.edit()?.putInt(KEY_PRESET, -1)?.apply() // Сброс пресета
            saveBandLevels()
        }
    }

    fun getBandLevel(band: Int): Short =
        equalizer?.getBandLevel(band.toShort()) ?: 0

    fun getPreset(): Int {
        val eq = equalizer ?: return -1
        return try {
            // Проверяем, совпадает ли текущее состояние с каким-то пресетом.
            val saved = prefs?.getInt(KEY_PRESET, -1) ?: -1
            if (saved in 0 until eq.numberOfPresets) saved else -1
        } catch (_: Exception) {
            -1
        }
    }

    private fun saveBandLevels() {
        val eq = equalizer ?: return
        val levels = ShortArray(bandCount) { eq.getBandLevel(it.toShort()) }
        val json = levels.joinToString(",")
        prefs?.edit()?.putString(KEY_BANDS, json)?.apply()
    }

    private fun loadBandLevels(): ShortArray {
        val json = prefs?.getString(KEY_BANDS, null) ?: return shortArrayOf()
        return try {
            json.split(",").map { it.trim().toShort() }.toShortArray()
        } catch (_: Exception) {
            shortArrayOf()
        }
    }
}
