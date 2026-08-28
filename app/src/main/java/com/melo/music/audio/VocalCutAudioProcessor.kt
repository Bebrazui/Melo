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
 * DSP аудио-процессор для подавления ведущего вокала на лету.
 *
 * Алгоритм:
 * 1. В стерео-миксах ведущий голос находится строго по центру (L = R).
 * 2. Низкочастотный IIR-фильтр отделяет бас и бочку (< 180 Гц).
 * 3. Из центрального канала (Mid) вычитается низкая частота -> получаем голосовой диапазон.
 * 4. Этот синфазный вокал вычитается из левого и правого каналов.
 *
 * Результат: вокал глушится, а бас, стерео-инструменты и реверберация сохраняются.
 */
class VocalCutAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var isEnabled: Boolean = false

    // Состояние 1-полюсного IIR-фильтра нижних частот для баса
    private var lowState: Float = 0f
    private var alpha: Float = 0.025f // Коэффициент фильтрации (~180 Гц при 44.1кГц)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // Поддерживаем 16-битный стерео PCM поток
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        // Пересчитываем альфа-коэффициент под частоту дискретизации
        val sampleRate = inputAudioFormat.sampleRate
        if (sampleRate > 0) {
            val cutoffHz = 180.0f
            val dt = 1.0f / sampleRate
            val rc = 1.0f / (2.0f * Math.PI.toFloat() * cutoffHz)
            alpha = (dt / (rc + dt)).coerceIn(0.001f, 0.5f)
        }

        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        // Всегда активен в конвейере, чтобы переключать на лету без переинициализации плеера
        return inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val output = replaceOutputBuffer(remaining)

        if (!isEnabled) {
            // Режим выключен — прозрачный сквозной пропуск аудио
            output.put(inputBuffer)
            output.flip()
            return
        }

        // Включаем little-endian порядок байт (стандарт для PCM 16-bit на Android)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)

        while (inputBuffer.remaining() >= 4) {
            val left = inputBuffer.short.toFloat()
            val right = inputBuffer.short.toFloat()

            // Центральный сигнал (Mid)
            val mid = (left + right) * 0.5f

            // Фильтрованный бас (Low)
            lowState += alpha * (mid - lowState)

            // Вокальная составляющая в центре (без баса)
            val vocal = mid - lowState

            // Вычитаем вокал из каналов
            val outLeft = (left - vocal).toInt().coerceIn(-32768, 32767).toShort()
            val outRight = (right - vocal).toInt().coerceIn(-32768, 32767).toShort()

            output.putShort(outLeft)
            output.putShort(outRight)
        }

        output.flip()
    }

    override fun onReset() {
        lowState = 0f
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
