package com.melo.music.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer

/**
 * Аудио-эффекты: эквалайзер + усиление (gain) + реверберация (reverb) + пространственный звук 3D (spatial audio).
 * Обёртка над android.media.audiofx.*. Привязывается к audioSessionId плеера,
 * сохраняет настройки в SharedPreferences.
 */
object EqualizerManager {

    private const val PREFS = "melo_equalizer"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PRESET = "preset"
    private const val KEY_BANDS = "bands"
    private const val KEY_GAIN = "gain_mb"
    private const val KEY_REVERB = "reverb_preset"
    private const val KEY_SPATIAL = "spatial_audio"
    private const val KEY_SPATIAL_STRENGTH = "spatial_strength"

    /** Максимальное усиление (мДб) = +20 dB. */
    const val MAX_GAIN_MB = 2000

    /** Названия пресетов реверберации (индекс = значение PresetReverb.PRESET_*). */
    val reverbPresetNames = arrayOf(
        "Выкл", "Малая комната", "Средняя комната",
        "Большая комната", "Средний зал", "Большой зал", "Пластина",
    )

    private var prefs: SharedPreferences? = null
    private var equalizer: Equalizer? = null
    private var loudness: LoudnessEnhancer? = null
    private var virtualizer: Virtualizer? = null
    private var insertReverb: PresetReverb? = null

    // Реверберация — ГЛОБАЛЬНЫЙ вспомогательный (auxiliary) эффект на сессии 0.
    // Плеер шлёт в него звук через setAuxEffectInfo (вставка insert-реверба
    // на сессию плеера не работает стабильно на многих устройствах).
    private var auxReverb: PresetReverb? = null

    /** Уровень посыла в реверб, когда он включён. */
    private const val REVERB_SEND_LEVEL = 0.9f

    /** Сервис подписывается сюда, чтобы переприменить aux-эффект к активному плееру. */
    var onReverbChanged: (() -> Unit)? = null

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
        // Глобальный aux-реверб создаём один раз. Сессия 0 = output mix.
        try {
            val rv = PresetReverb(1, 0)
            rv.preset = (prefs?.getInt(KEY_REVERB, 0) ?: 0).toShort()
            rv.enabled = true // всегда включён; «нет реверба» = preset NONE / send 0.
            auxReverb = rv
        } catch (_: Exception) {
            auxReverb = null
        }
    }

    /**
     * Привязать эквалайзер, реверберацию и пространственный звук к сессии плеера. Вызывать при старте воспроизведения.
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

        // ── Усиление (gain) — insert-эффект на сессии плеера ──
        try {
            val le = LoudnessEnhancer(audioSessionId)
            val gain = prefs?.getInt(KEY_GAIN, 0) ?: 0
            le.setTargetGain(gain)
            le.enabled = gain > 0
            loudness = le
        } catch (_: Exception) {
            loudness = null
        }

        // ── Пространственный звук (3D Spatial Virtualizer) ──
        try {
            val virt = Virtualizer(0, audioSessionId)
            val spatialEnabled = prefs?.getBoolean(KEY_SPATIAL, false) ?: false
            val strength = prefs?.getInt(KEY_SPATIAL_STRENGTH, 850) ?: 850
            if (virt.strengthSupported) {
                virt.setStrength(strength.toShort())
            }
            virt.enabled = spatialEnabled
            virtualizer = virt
        } catch (_: Exception) {
            virtualizer = null
        }

        // ── Прямая реверберация (Insert PresetReverb на сессии) ──
        try {
            val ir = PresetReverb(0, audioSessionId)
            val p = prefs?.getInt(KEY_REVERB, 0) ?: 0
            if (p > 0) {
                ir.preset = p.toShort()
                ir.enabled = true
            } else {
                ir.enabled = false
            }
            insertReverb = ir
        } catch (_: Exception) {
            insertReverb = null
        }
    }

    @Synchronized
    fun release() {
        equalizer?.release()
        equalizer = null
        loudness?.release()
        loudness = null
        virtualizer?.release()
        virtualizer = null
        insertReverb?.release()
        insertReverb = null
        // auxReverb НЕ освобождаем — он глобальный, живёт всё время.
    }

    private val dspProcessors = java.util.concurrent.CopyOnWriteArrayList<MeloDspAudioProcessor>()

    fun registerDsp(dsp: MeloDspAudioProcessor) {
        dspProcessors.add(dsp)
        syncDsp(dsp)
    }

    fun unregisterDsp(dsp: MeloDspAudioProcessor) {
        dspProcessors.remove(dsp)
    }

    private fun syncDsp(dsp: MeloDspAudioProcessor) {
        dsp.spatialEnabled = isSpatialEnabled()
        dsp.spatialStrength = (getSpatialStrength() / 1000f).coerceIn(0f, 1f)
        dsp.reverbPreset = getReverbPreset()
        dsp.gainFactor = 1.0f + (getGain() / 1000f) * 0.75f
        dsp.eqEnabled = isEnabled()
        val levels = loadBandLevels()
        for (i in 0 until 5) {
            val db = if (i < levels.size) levels[i] / 100f else 0f
            dsp.setBandGain(i, db)
        }
    }

    // ── Пространственный звук 3D (Spatial Audio) ──────────────────────────────

    fun isSpatialEnabled(): Boolean = prefs?.getBoolean(KEY_SPATIAL, false) ?: false

    fun getSpatialStrength(): Int = prefs?.getInt(KEY_SPATIAL_STRENGTH, 850) ?: 850

    @Synchronized
    fun setSpatialEnabled(enabled: Boolean) {
        virtualizer?.let { runCatching { it.enabled = enabled } }
        for (d in dspProcessors) d.spatialEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_SPATIAL, enabled)?.apply()
    }

    @Synchronized
    fun setSpatialStrength(strength: Int) {
        val s = strength.coerceIn(0, 1000)
        virtualizer?.let {
            runCatching {
                if (it.strengthSupported) it.setStrength(s.toShort())
            }
        }
        for (d in dspProcessors) d.spatialStrength = (s / 1000f).coerceIn(0f, 1f)
        prefs?.edit()?.putInt(KEY_SPATIAL_STRENGTH, s)?.apply()
    }

    // ── Усиление (gain) ───────────────────────────────────────────────────────

    /** Текущее усиление в мДб (0…[MAX_GAIN_MB]). */
    fun getGain(): Int = prefs?.getInt(KEY_GAIN, 0) ?: 0

    @Synchronized
    fun setGain(mb: Int) {
        val v = mb.coerceIn(0, MAX_GAIN_MB)
        loudness?.let {
            runCatching {
                it.setTargetGain(v)
                it.enabled = v > 0
            }
        }
        for (d in dspProcessors) d.gainFactor = 1.0f + (v / 1000f) * 0.75f
        prefs?.edit()?.putInt(KEY_GAIN, v)?.apply()
    }

    // ── Реверберация ──────────────────────────────────────────────────────────

    /** Текущий пресет реверберации (0 = выкл). */
    fun getReverbPreset(): Int = prefs?.getInt(KEY_REVERB, 0) ?: 0

    /** Id aux-эффекта реверба (0 = нет эффекта) — для player.setAuxEffectInfo. */
    fun reverbEffectId(): Int = auxReverb?.id ?: 0

    /** Уровень посыла в реверб (0 если выключен). */
    fun reverbSendLevel(): Float = if (getReverbPreset() > 0) REVERB_SEND_LEVEL else 0f

    @Synchronized
    fun setReverbPreset(preset: Int) {
        val p = preset.coerceIn(0, reverbPresetNames.lastIndex)
        insertReverb?.let {
            runCatching {
                if (p > 0) {
                    it.preset = p.toShort()
                    it.enabled = true
                } else {
                    it.enabled = false
                }
            }
        }
        auxReverb?.let {
            runCatching { it.preset = p.toShort() }
        }
        for (d in dspProcessors) d.reverbPreset = p
        prefs?.edit()?.putInt(KEY_REVERB, p)?.apply()
        // Сервис переустановит aux-send на активном плеере (0 ↔ полный).
        onReverbChanged?.invoke()
    }

    fun isEnabled(): Boolean = prefs?.getBoolean(KEY_ENABLED, false) ?: false

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        for (d in dspProcessors) d.eqEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
    }

    @Synchronized
    fun setPreset(presetIndex: Int) {
        val eq = equalizer
        if (eq != null && presetIndex in 0 until eq.numberOfPresets) {
            eq.usePreset(presetIndex.toShort())
            saveBandLevels()
        }
        prefs?.edit()?.putInt(KEY_PRESET, presetIndex)?.apply()
        val levels = loadBandLevels()
        for (d in dspProcessors) {
            for (i in 0 until 5) {
                val db = if (i < levels.size) levels[i] / 100f else 0f
                d.setBandGain(i, db)
            }
        }
    }

    @Synchronized
    fun setBandLevel(band: Int, level: Short) {
        equalizer?.let {
            if (band in 0 until bandCount) {
                it.setBandLevel(band.toShort(), level)
            }
        }
        for (d in dspProcessors) {
            d.setBandGain(band, level / 100f)
        }
        prefs?.edit()?.putInt(KEY_PRESET, -1)?.apply() // Сброс пресета
        saveBandLevels()
    }

    fun getBandLevel(band: Int): Short =
        equalizer?.getBandLevel(band.toShort()) ?: 0

    fun getPreset(): Int {
        val eq = equalizer ?: return -1
        return try {
            val saved = prefs?.getInt(KEY_PRESET, -1) ?: -1
            if (saved in 0 until eq.numberOfPresets) saved else -1
        } catch (_: Exception) {
            -1
        }
    }

    private fun saveBandLevels() {
        val eq = equalizer
        val levels = if (eq != null && bandCount > 0) {
            ShortArray(bandCount) { eq.getBandLevel(it.toShort()) }
        } else {
            ShortArray(5) { 0 }
        }
        val json = levels.joinToString(",")
        prefs?.edit()?.putString(KEY_BANDS, json)?.apply()
    }

    private fun loadBandLevels(): ShortArray {
        val json = prefs?.getString(KEY_BANDS, null) ?: return shortArrayOf(0, 0, 0, 0, 0)
        return try {
            json.split(",").map { it.trim().toShort() }.toShortArray()
        } catch (_: Exception) {
            shortArrayOf(0, 0, 0, 0, 0)
        }
    }
}
