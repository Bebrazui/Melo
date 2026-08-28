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
 * Профессиональный Karaoke / Vocal Remover DSP процессор.
 *
 * Почему в классических реализациях звук казался «телефонным»:
 * При обычном вычитании левый канал получал +Side, а правый -Side (инверсия фазы).
 * При воспроизведении через динамики телефона или в моно левый и правый каналы складывались в воздухе:
 * (+Side) + (-Side) = 0! То есть ВСЕ инструменты уничтожались, и оставался только глухой бубнеж.
 *
 * Как работает этот процессор:
 * 1. Side = (Left - Right) * 0.85 — извлекает все инструменты, синты, гитары, реверб и бэки.
 *    Поскольку ведущий вокал сведен в 0 по панораме (L = R), он ПОЛНОСТЬЮ вычитается в ноль.
 * 2. 2-полюсный Butterworth LPF (160 Гц) подмешивает чистый плотный суб-бас и бочку (Low).
 * 3. 2-полюсный Butterworth HPF (7.5 кГц) сохраняет звонкие верха и тарелки (High).
 * 4. Итоговый микс (Side + Low + High) подается в ОБА канала В ОДНОЙ ФАЗЕ (+Side).
 *    В результате инструменты НЕ гасят друг друга на динамиках телефона, звук остается
 *    громким, кристально чистым, с мощным басом, а солист полностью заглушен!
 */
class VocalCutAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var isEnabled: Boolean = false

    private val lpf = BiquadLPF()
    private val hpf = BiquadHPF()

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        val sampleRate = inputAudioFormat.sampleRate.toFloat().coerceAtLeast(8000f)
        lpf.set(160f, 0.707f, sampleRate)
        hpf.set(7500f, 0.707f, sampleRate)

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

            // 1. Полное фазовое подавление центрального вокала:
            val side = (left - right) * 0.85f

            // 2. Восстановление баса и бочки (< 160 Гц)
            val mid = (left + right) * 0.5f
            val low = lpf.process(mid)

            // 3. Сохранение кристальных верхов (> 7.5 кГц)
            val high = hpf.process(mid) * 0.5f

            // 4. Синфазный микс без противофазного гашения
            val instrumental = (side + low + high).toInt().coerceIn(-32768, 32767).toShort()

            output.putShort(instrumental)
            output.putShort(instrumental)
        }

        output.flip()
    }

    override fun onReset() {
        lpf.reset()
        hpf.reset()
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
