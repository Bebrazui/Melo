package com.melo.music.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Улучшенный Hi-Fi DSP аудиопроцессор для подавления вокала (Karaoke Pro).
 *
 * Особенности:
 * 1. Адаптивная авто-калибровка панорамы (Auto-Pan Calibration):
 *    Отслеживает баланс энергии каналов в голосовом диапазоне и подстраивает коэффициент вычитания,
 *    чтобы убрать вокал даже при смещении центра на 2-5%.
 * 2. 2-полюсный Peaking/Notch фильтр вокальных формант (-7 dB на 1.8 кГц):
 *    Приглушает стерео-реверберацию вокала, стерео-эхо и дабл-треки в разностном сигнале.
 * 3. Butterworth LPF (< 150 Гц) и HPF (> 7.2 кГц):
 *    Сохраняют мощный суб-бас, кик и кристальные верха.
 * 4. Синфазная подача:
 *    Звук не гасит сам себя на динамиках телефона.
 */
class VocalCutAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var isEnabled: Boolean = false

    private val lpf = BiquadLPF()
    private val hpf = BiquadHPF()
    private val bpfL = BiquadBPF()
    private val bpfR = BiquadBPF()
    private val vocalNotch = BiquadPeakingEQ()

    private var envL = 1000f
    private var envR = 1000f
    private var beta = 0.002f // Окно сглаживания ~12 мс

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        val sampleRate = inputAudioFormat.sampleRate.toFloat().coerceAtLeast(8000f)
        lpf.set(150f, 0.707f, sampleRate)
        hpf.set(7200f, 0.707f, sampleRate)
        bpfL.set(1200f, 0.5f, sampleRate)
        bpfR.set(1200f, 0.5f, sampleRate)
        vocalNotch.set(1800f, 0.7f, -7.0f, sampleRate)

        beta = (1.0f / (sampleRate * 0.012f)).coerceIn(0.0005f, 0.05f)

        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        return inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val output = replaceOutputBuffer(remaining)

        if (!isEnabled) {
            output.put(inputBuffer)
            output.flip()
            return
        }

        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)

        while (inputBuffer.remaining() >= 4) {
            val left = inputBuffer.short.toFloat()
            val right = inputBuffer.short.toFloat()

            // 1. Отслеживание баланса панорамы в голосовом диапазоне
            val vL = bpfL.process(left)
            val vR = bpfR.process(right)
            envL += beta * (vL * vL - envL)
            envR += beta * (vR * vR - envR)

            val panRatio = (Math.sqrt((envL + 200.0) / (envR + 200.0))).toFloat().coerceIn(0.75f, 1.33f)

            // 2. Адаптивное вычитание центрального вокала
            val rawSide = (left - panRatio * right) * 0.88f

            // 3. Подавление остаточных стерео-хвостов реверберации и дабл-треков
            val sideClean = vocalNotch.process(rawSide)

            // 4. Восстановление баса (< 150 Гц) и верхов (> 7.2 кГц)
            val mid = (left + right) * 0.5f
            val low = lpf.process(mid)
            val high = hpf.process(mid) * 0.5f

            // 5. Синфазный микс без потерь на динамиках
            val instrumental = (sideClean + low + high).toInt().coerceIn(-32768, 32767).toShort()

            output.putShort(instrumental)
            output.putShort(instrumental)
        }

        output.flip()
    }

    override fun onReset() {
        lpf.reset()
        hpf.reset()
        bpfL.reset()
        bpfR.reset()
        vocalNotch.reset()
        envL = 1000f
        envR = 1000f
    }

    /** 2-полюсный полосовой фильтр (Band-Pass) для анализа энергии голоса. */
    private class BiquadBPF {
        private var b0 = 0f
        private var b1 = 0f
        private var b2 = 0f
        private var a1 = 0f
        private var a2 = 0f
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f

        fun set(f0: Float, q: Float, sampleRate: Float) {
            val w0 = (2.0 * Math.PI * f0 / sampleRate).toFloat()
            val alpha = (Math.sin(w0.toDouble()) / (2.0 * q)).toFloat()
            val cosw0 = Math.cos(w0.toDouble()).toFloat()
            val a0 = 1f + alpha

            b0 = alpha / a0
            b1 = 0f
            b2 = -alpha / a0
            a1 = (-2f * cosw0) / a0
            a2 = (1f - alpha) / a0
        }

        fun process(x: Float): Float {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            return y
        }

        fun reset() {
            x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
        }
    }

    /** Параметрический Peaking EQ / Notch для приглушения стерео-ревербераций вокала. */
    private class BiquadPeakingEQ {
        private var b0 = 0f
        private var b1 = 0f
        private var b2 = 0f
        private var a1 = 0f
        private var a2 = 0f
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f

        fun set(f0: Float, q: Float, gainDb: Float, sampleRate: Float) {
            val a = Math.pow(10.0, gainDb / 40.0).toFloat()
            val w0 = (2.0 * Math.PI * f0 / sampleRate).toFloat()
            val alpha = (Math.sin(w0.toDouble()) / (2.0 * q)).toFloat()
            val cosw0 = Math.cos(w0.toDouble()).toFloat()
            val a0 = 1f + alpha / a

            b0 = (1f + alpha * a) / a0
            b1 = (-2f * cosw0) / a0
            b2 = (1f - alpha * a) / a0
            a1 = (-2f * cosw0) / a0
            a2 = (1f - alpha / a) / a0
        }

        fun process(x: Float): Float {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            return y
        }

        fun reset() {
            x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
        }
    }

    /** 2-полюсный фильтр нижних частот Баттерворта (Low-Pass). */
    private class BiquadLPF {
        private var b0 = 0f
        private var b1 = 0f
        private var b2 = 0f
        private var a1 = 0f
        private var a2 = 0f
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f

        fun set(cutoffHz: Float, q: Float, sampleRate: Float) {
            val w0 = (2.0 * Math.PI * cutoffHz / sampleRate).toFloat()
            val alpha = (Math.sin(w0.toDouble()) / (2.0 * q)).toFloat()
            val cosw0 = Math.cos(w0.toDouble()).toFloat()
            val a0 = 1f + alpha

            b0 = ((1f - cosw0) / 2f) / a0
            b1 = (1f - cosw0) / a0
            b2 = ((1f - cosw0) / 2f) / a0
            a1 = (-2f * cosw0) / a0
            a2 = (1f - alpha) / a0
        }

        fun process(x: Float): Float {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            return y
        }

        fun reset() {
            x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
        }
    }

    /** 2-полюсный фильтр верхних частот Баттерворта (High-Pass). */
    private class BiquadHPF {
        private var b0 = 0f
        private var b1 = 0f
        private var b2 = 0f
        private var a1 = 0f
        private var a2 = 0f
        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f

        fun set(cutoffHz: Float, q: Float, sampleRate: Float) {
            val w0 = (2.0 * Math.PI * cutoffHz / sampleRate).toFloat()
            val alpha = (Math.sin(w0.toDouble()) / (2.0 * q)).toFloat()
            val cosw0 = Math.cos(w0.toDouble()).toFloat()
            val a0 = 1f + alpha

            b0 = ((1f + cosw0) / 2f) / a0
            b1 = (-(1f + cosw0)) / a0
            b2 = ((1f + cosw0) / 2f) / a0
            a1 = (-2f * cosw0) / a0
            a2 = (1f - alpha) / a0
        }

        fun process(x: Float): Float {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            return y
        }

        fun reset() {
            x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
        }
    }
}

/**
 * Менеджер для глобального управления состоянием удаления вокала в UI и сервисе.
 */
object VocalCutManager {
    var isEnabled by mutableStateOf(false)
        private set

    private val processors = mutableListOf<VocalCutAudioProcessor>()

    fun register(processor: VocalCutAudioProcessor) {
        synchronized(processors) {
            if (!processors.contains(processor)) {
                processor.isEnabled = isEnabled
                processors.add(processor)
            }
        }
    }

    fun unregister(processor: VocalCutAudioProcessor) {
        synchronized(processors) {
            processors.remove(processor)
        }
    }

    fun toggle(): Boolean {
        setVocalCutActive(!isEnabled)
        return isEnabled
    }

    fun setVocalCutActive(active: Boolean) {
        isEnabled = active
        synchronized(processors) {
            for (p in processors) {
                p.isEnabled = active
            }
        }
    }
}
