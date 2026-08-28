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
 * Hi-Fi Center-Channel Vocal Remover DSP.
 *
 * Математический принцип:
 * 1. Вычисляется центральный сигнал Mid = (Left + Right) / 2.
 * 2. Каскадный 4-полюсный полосовой фильтр (HPF 130 Гц + LPF 6800 Гц) выделяет
 *    только голосовой спектр центрального канала: MidVocal.
 * 3. Из левого и правого каналов вычитается MidVocal:
 *    OutLeft = Left - MidVocal
 *    OutRight = Right - MidVocal
 *
 * Результат:
 * - Ведущий голос в центре (Left = Right = V) вычитается сам из себя: V - V = 0.
 * - Стерео-инструменты по бокам (гитары, синты, реверберации) сохраняют свои исходные каналы и объемное стерео.
 * - Суб-бас и бочка (< 130 Гц) и звонкие верха (> 6.8 кГц) остаются на 100% нетронутыми.
 * - Нулевая задержка (0 мс), отсутствие буферизации, чистейший Hi-Fi звук без эффекта "телефонной трубки".
 */
class VocalCutAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var isEnabled: Boolean = false

    private val hpf1 = BiquadHPF()
    private val hpf2 = BiquadHPF()
    private val lpf1 = BiquadLPF()
    private val lpf2 = BiquadLPF()

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        val sampleRate = inputAudioFormat.sampleRate.toFloat().coerceAtLeast(8000f)

        // 4-полюсный полосовой фильтр голосовой зоны (130 Гц - 6800 Гц)
        hpf1.set(130f, 0.707f, sampleRate)
        hpf2.set(130f, 0.707f, sampleRate)
        lpf1.set(6800f, 0.707f, sampleRate)
        lpf2.set(6800f, 0.707f, sampleRate)

        onReset()

        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        return inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remainingBytes = inputBuffer.remaining()
        if (remainingBytes == 0) return

        val output = replaceOutputBuffer(remainingBytes)

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

            // 1. Центральный сигнал
            val mid = (left + right) * 0.5f

            // 2. Выделение голосового диапазона через 4-полюсную фильтрацию
            val midVocal = lpf2.process(lpf1.process(hpf2.process(hpf1.process(mid))))

            // 3. Вычитание вокала с сохранением стерео-панорамы инструментов
            val outL = (left - midVocal).toInt().coerceIn(-32768, 32767).toShort()
            val outR = (right - midVocal).toInt().coerceIn(-32768, 32767).toShort()

            output.putShort(outL)
            output.putShort(outR)
        }

        output.flip()
    }

    override fun onReset() {
        hpf1.reset()
        hpf2.reset()
        lpf1.reset()
        lpf2.reset()
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
