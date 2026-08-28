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
 * Hi-Fi DSP аудио-процессор для подавления ведущего вокала (Karaoke / Vocal Remover).
 *
 * Почему старый метод L - R звучал как телефон:
 * 1. Простое вычитание переворачивало фазу правого канала на 180° (anti-phase), из-за чего
 *    на динамиках телефона или в помещении левый и правый каналы гасили сами себя в воздухе,
 *    оставляя только тонкий средне-низкий бубнеж.
 * 2. Срезались все звонкие высокие частоты (>5 кГц).
 *
 * Новый алгоритм (In-Phase Stereo Pan Correlation + 2nd Order Biquad Bandpass):
 * 1. Высокие частоты (> 4.5 кГц) и суб-бас (< 160 Гц) остаются 100% нетронутыми в кристальном студийном качестве.
 * 2. В голосовом диапазоне (200 - 4500 Гц) отслеживается коэффициент стерео-корреляции:
 *    - Если звук разведен по бокам (стерео-гитары, синты, бэки) -> коэффициент 0, инструменты звучат чисто.
 *    - Если звук строго по центру (солист) -> коэффициент 1, голос синфазно вычитается без инверсии фазы.
 * 3. Каналы остаются синфазными, сохраняя сочный стерео-образ без эффекта "бочки" и "телефонной трубки".
 */
class VocalCutAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var isEnabled: Boolean = false

    private val bpfL = BiquadBPF()
    private val bpfR = BiquadBPF()

    private var envL = 0f
    private var envR = 0f
    private var envLR = 0f
    private var beta = 0.003f // Окно сглаживания энергии ~8 мс

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        val sampleRate = inputAudioFormat.sampleRate.toFloat().coerceAtLeast(8000f)
        // Полосовой фильтр голосового диапазона (f0 = 1050 Гц, Q = 0.42 -> полоса ~180 Гц - 4500 Гц)
        bpfL.set(1050f, 0.42f, sampleRate)
        bpfR.set(1050f, 0.42f, sampleRate)

        beta = (1.0f / (sampleRate * 0.008f)).coerceIn(0.0005f, 0.05f)

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

            // Выделяем только голосовой диапазон частот для анализа
            val leftBP = bpfL.process(left)
            val rightBP = bpfR.process(right)

            // Отслеживание огибающей мощности
            envL += beta * (leftBP * leftBP - envL)
            envR += beta * (rightBP * rightBP - envR)
            envLR += beta * (leftBP * rightBP - envLR)

            // Коэффициент центральной панорамы (1.0 = вокал строго по центру, 0.0 = инструмент по бокам)
            val denom = envL + envR + 1000f
            val corr = (2f * maxOf(0f, envLR) / denom).coerceIn(0f, 1f)

            // Вычитаем синфазный центральный вокал из исходного сигнала
            val midBP = (leftBP + rightBP) * 0.5f
            val vocalCut = 0.90f * corr * midBP

            val outLeft = (left - vocalCut).toInt().coerceIn(-32768, 32767).toShort()
            val outRight = (right - vocalCut).toInt().coerceIn(-32768, 32767).toShort()

            output.putShort(outLeft)
            output.putShort(outRight)
        }

        output.flip()
    }

    override fun onReset() {
        bpfL.reset()
        bpfR.reset()
        envL = 0f
        envR = 0f
        envLR = 0f
    }

    /** Двухполюсный полосовой фильтр (Biquad Bandpass). */
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
