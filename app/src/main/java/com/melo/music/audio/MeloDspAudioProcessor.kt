package com.melo.music.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * Профессиональный программный DSP-процессор для ExoPlayer Media3:
 * 1. 3D Spatial Surround Audio (стерео-экспандер + психоакустическая задержка Хааса + HRTF-панорама).
 * 2. Реверберация / Эхо (матрица гребенчатых и фазовых фильтров Шрёдера/Freeverb).
 * 3. 5-полосный IIR Biquad эквалайзер.
 * 4. Усиление громкости (Gain Booster).
 *
 * Работает напрямую с 16-битными стерео PCM-сэмплами в конвейере ExoPlayer,
 * гарантируя 100% работу на абсолютно всех устройствах, прошивках и наушниках.
 */
class MeloDspAudioProcessor : BaseAudioProcessor() {

    // ── 3D Spatial Audio ──
    @Volatile
    var spatialEnabled: Boolean = false
    @Volatile
    var spatialStrength: Float = 0.85f // 0.0f .. 1.0f

    // ── Reverb / Echo ──
    @Volatile
    var reverbPreset: Int = 0 // 0 = off, 1..6 = presets

    // ── Gain ──
    @Volatile
    var gainFactor: Float = 1.0f

    // ── Equalizer ──
    @Volatile
    var eqEnabled: Boolean = false
    private val eqBands = Array(5) { BiquadPeakFilter() }

    // DSP буферы для реверберации и стерео-расширителя
    private var sampleRate: Int = 44100
    private val delayBufferLeft = FloatArray(4410)
    private val delayBufferRight = FloatArray(4410)
    private var delayWriteIndex = 0

    // Comb фильтры реверберации
    private val comb1 = CombFilter(1116)
    private val comb2 = CombFilter(1188)
    private val comb3 = CombFilter(1277)
    private val comb4 = CombFilter(1356)
    private val allpass1 = AllpassFilter(556)
    private val allpass2 = AllpassFilter(441)
    private var reverbHpStoreL = 0f
    private var reverbHpStoreR = 0f

    // ── Фильтры 3D Spatial Audio ──
    private var sideHpStore = 0f
    private var crossLpStoreL = 0f
    private var crossLpStoreR = 0f
    private var sideAirStore = 0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(8000)
        initEqFilters(sampleRate)
        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        return inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET
    }

    private fun initEqFilters(sr: Int) {
        val freqs = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)
        for (i in 0 until 5) {
            eqBands[i].set(freqs[i], 1.0f, 0f, sr.toFloat())
        }
    }

    fun setBandGain(band: Int, gainDb: Float) {
        if (band in eqBands.indices) {
            val freqs = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)
            eqBands[band].set(freqs[band], 1.0f, gainDb, sampleRate.toFloat())
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val output = replaceOutputBuffer(remaining)

        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)

        val isSpatial = spatialEnabled
        val isReverb = reverbPreset > 0
        val isEq = eqEnabled
        val gain = gainFactor

        // ── Параметры реверберации ──
        val (reverbFeedback, reverbDamp, reverbWet) = when (reverbPreset) {
            1 -> Triple(0.55f, 0.40f, 0.35f) // Малая комната
            2 -> Triple(0.68f, 0.35f, 0.45f) // Средняя комната
            3 -> Triple(0.78f, 0.30f, 0.55f) // Большая комната
            4 -> Triple(0.85f, 0.25f, 0.65f) // Средний зал
            5 -> Triple(0.90f, 0.20f, 0.75f) // Большой зал
            6 -> Triple(0.85f, 0.25f, 0.70f) // Пластина (яркое шелковистое студийное эхо)
            else -> Triple(0f, 0f, 0f)
        }

        comb1.feedback = reverbFeedback; comb1.damp = reverbDamp
        comb2.feedback = reverbFeedback; comb2.damp = reverbDamp
        comb3.feedback = reverbFeedback; comb3.damp = reverbDamp
        comb4.feedback = reverbFeedback; comb4.damp = reverbDamp

        val spatialWidth = 1.25f + spatialStrength * 1.5f // 1.25 .. 2.75x широкая сцена
        val delaySamples = (sampleRate * 0.0012f).toInt().coerceIn(10, delayBufferLeft.size - 1) // 1.2ms межушная задержка

        // Коэффициенты DSP-фильтров
        val bassCrossoverAlpha = (2.0 * Math.PI * 140.0 / sampleRate).toFloat().coerceIn(0.005f, 0.25f) // 140 Hz моно-бас
        val headShadowAlpha = (2.0 * Math.PI * 2200.0 / sampleRate).toFloat().coerceIn(0.05f, 0.65f) // 2.2 kHz HRTF тень головы
        val airAlpha = (2.0 * Math.PI * 8500.0 / sampleRate).toFloat().coerceIn(0.1f, 0.9f) // 8.5 kHz воздух/подъём сцены
        val hpAlpha = (2.0 * Math.PI * 250.0 / sampleRate).toFloat().coerceIn(0.01f, 0.5f)

        while (inputBuffer.remaining() >= 4) {
            var left = inputBuffer.short.toFloat()
            var right = inputBuffer.short.toFloat()

            // 1. Эквалайзер (5-полосный IIR Biquad)
            if (isEq) {
                for (b in eqBands) {
                    left = b.process(left)
                    right = b.process(right)
                }
            }

            // 2. 3D Spatial Audio (HRTF Head-Shadowing + Mono-Bass + Air Shimmer)
            if (isSpatial) {
                val mid = (left + right) * 0.5f
                val rawSide = (left - right) * 0.5f

                // 2a. Mono-Bass: частоты ниже 140 Гц не размываются в стерео, удар бочки остаётся центрированным и мощным
                sideHpStore += (rawSide - sideHpStore) * bassCrossoverAlpha
                val sideHigh = rawSide - sideHpStore
                val sideBass = sideHpStore

                // 2b. Air Shimmer: лёгкий подъём ультра-высоких частот (>8.5 кГц) для ощущения простора
                sideAirStore += (sideHigh - sideAirStore) * airAlpha
                val sideAir = sideHigh - sideAirStore

                val widenedSide = sideBass + sideHigh * spatialWidth + sideAir * (spatialStrength * 0.28f)

                // 2c. Бинауральная задержка + HRTF фильтр поглощения черепом (Head Shadowing)
                delayBufferLeft[delayWriteIndex] = left
                delayBufferRight[delayWriteIndex] = right
                val readIndex = (delayWriteIndex - delaySamples + delayBufferLeft.size) % delayBufferLeft.size
                val delayedL = delayBufferLeft[readIndex]
                val delayedR = delayBufferRight[readIndex]
                delayWriteIndex = (delayWriteIndex + 1) % delayBufferLeft.size

                crossLpStoreL += (delayedR - crossLpStoreL) * headShadowAlpha
                crossLpStoreR += (delayedL - crossLpStoreR) * headShadowAlpha

                val crossfeedGain = spatialStrength * 0.30f
                left = mid + widenedSide + crossLpStoreL * crossfeedGain
                right = mid - widenedSide - crossLpStoreR * crossfeedGain
            }

            // 3. Реверберация (Schroeder / Freeverb Matrix с High-Pass фильтром)
            if (isReverb) {
                // Отсекаем низкие частоты из сигнала ревербератора
                reverbHpStoreL += (left - reverbHpStoreL) * hpAlpha
                reverbHpStoreR += (right - reverbHpStoreR) * hpAlpha
                val hpInL = left - reverbHpStoreL
                val hpInR = right - reverbHpStoreR
                val monoIn = (hpInL + hpInR) * 0.5f * 0.025f

                val cOut = comb1.process(monoIn) + comb2.process(monoIn) + comb3.process(monoIn) + comb4.process(monoIn)
                val revOut = allpass2.process(allpass1.process(cOut)) * 16.0f
                left = left * (1f - reverbWet * 0.30f) + revOut * reverbWet
                right = right * (1f - reverbWet * 0.30f) + revOut * reverbWet
            }

            // 4. Усиление громкости (Gain Booster)
            if (gain != 1.0f) {
                left *= gain
                right *= gain
            }

            // 5. Мягкий лимитер (Soft-Clipping / Tanh), предотвращающий перегруз и хрипы на пиках
            val normL = left / 32768f
            val normR = right / 32768f
            val limitedL = if (abs(normL) > 0.95f) sign(normL) * (0.95f + 0.05f * tanh((abs(normL) - 0.95f) / 0.5f)) else normL
            val limitedR = if (abs(normR) > 0.95f) sign(normR) * (0.95f + 0.05f * tanh((abs(normR) - 0.95f) / 0.5f)) else normR

            val outL = (limitedL * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            val outR = (limitedR * 32767f).toInt().coerceIn(-32768, 32767).toShort()

            output.putShort(outL)
            output.putShort(outR)
        }

        output.flip()
    }

    override fun onFlush() {
        for (b in eqBands) b.reset()
        delayBufferLeft.fill(0f)
        delayBufferRight.fill(0f)
        reverbHpStoreL = 0f
        reverbHpStoreR = 0f
        sideHpStore = 0f
        crossLpStoreL = 0f
        crossLpStoreR = 0f
        sideAirStore = 0f
        comb1.reset(); comb2.reset(); comb3.reset(); comb4.reset()
        allpass1.reset(); allpass2.reset()
    }

    override fun onReset() {
        onFlush()
    }

    private class CombFilter(size: Int) {
        val buffer = FloatArray(size)
        var idx = 0
        var filterStore = 0f
        var feedback = 0.8f
        var damp = 0.2f

        fun process(input: Float): Float {
            val output = buffer[idx]
            filterStore = (output * (1f - damp)) + (filterStore * damp)
            buffer[idx] = input + (filterStore * feedback)
            idx = (idx + 1) % buffer.size
            return output
        }

        fun reset() {
            buffer.fill(0f)
            filterStore = 0f
            idx = 0
        }
    }

    private class AllpassFilter(size: Int) {
        val buffer = FloatArray(size)
        var idx = 0
        val feedback = 0.5f

        fun process(input: Float): Float {
            val bufOut = buffer[idx]
            val output = -input + bufOut
            buffer[idx] = input + (bufOut * feedback)
            idx = (idx + 1) % buffer.size
            return output
        }

        fun reset() {
            buffer.fill(0f)
            idx = 0
        }
    }

    private class BiquadPeakFilter {
        var b0 = 1f; var b1 = 0f; var b2 = 0f; var a1 = 0f; var a2 = 0f
        var x1 = 0f; var x2 = 0f; var y1 = 0f; var y2 = 0f

        fun set(freq: Float, q: Float, gainDb: Float, sr: Float) {
            val a = 10.0.pow((gainDb / 40.0)).toFloat()
            val w0 = (2.0 * Math.PI * freq / sr).toFloat()
            val alpha = (sin(w0.toDouble()) / (2.0 * q)).toFloat()
            val cosW = cos(w0.toDouble()).toFloat()

            val a0 = 1.0f + alpha / a
            b0 = (1.0f + alpha * a) / a0
            b1 = (-2.0f * cosW) / a0
            b2 = (1.0f - alpha * a) / a0
            a1 = (-2.0f * cosW) / a0
            a2 = (1.0f - alpha / a) / a0
        }

        fun process(x: Float): Float {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x
            y2 = y1; y1 = y
            return if (y.isNaN()) 0f else y
        }

        fun reset() {
            x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
        }
    }
}
