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
 * Спектральный STFT аудиопроцессор для подавления вокала (Spectral Center-Channel Suppression).
 *
 * Принцип работы:
 * В отличие от обычного вычитания во временной области (L - R), спектральный алгоритм
 * раскладывает звук на 512 частотных полос через БПФ (Быстрое Преобразование Фурье).
 *
 * В каждой полосе частот вычисляется:
 * 1. Когерентность фаз между левым и правым ухом: cos(phi_L - phi_R).
 * 2. Баланс амплитуд: min(|L|, |R|) / max(|L|, |R|).
 *
 * Если частота находится строго по центру (голос солиста) — ее амплитуда в этой точке спектра
 * подавляется в ноль, при этом все остальные инструменты (бас, гитары, синты, ударные),
 * играющие на других частотах в этот же миллисекундный момент, остаются нетронутыми
 * и продолжают звучать в чистом, объемном стерео!
 */
class VocalCutAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var isEnabled: Boolean = false

    companion object {
        private const val FFT_SIZE = 1024
        private const val HOP_SIZE = 512 // 50% overlap
    }

    private val fft = FastRealFft(FFT_SIZE)
    private val sineWindow = FloatArray(FFT_SIZE) { i ->
        Math.sin(Math.PI * (i + 0.5) / FFT_SIZE).toFloat()
    }

    // Буферы истории входного сигнала (по 1024 сэмпла)
    private val inHistoryL = FloatArray(FFT_SIZE)
    private val inHistoryR = FloatArray(FFT_SIZE)

    // Буферы накопления синтеза (Overlap-Add)
    private val outAccumL = FloatArray(FFT_SIZE)
    private val outAccumR = FloatArray(FFT_SIZE)

    // Рабочие массивы БПФ
    private val realL = FloatArray(FFT_SIZE)
    private val imagL = FloatArray(FFT_SIZE)
    private val realR = FloatArray(FFT_SIZE)
    private val imagR = FloatArray(FFT_SIZE)

    // Промежуточный входной FIFO-буфер
    private val inFifoL = FloatArray(FFT_SIZE * 4)
    private val inFifoR = FloatArray(FFT_SIZE * 4)
    private var inFifoCount = 0

    // Промежуточный выходной FIFO-буфер
    private val outFifoL = FloatArray(FFT_SIZE * 4)
    private val outFifoR = FloatArray(FFT_SIZE * 4)
    private var outFifoRead = 0
    private var outFifoWrite = 0
    private var outFifoCount = 0

    private var sampleRateHz = 44100f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        sampleRateHz = inputAudioFormat.sampleRate.toFloat().coerceAtLeast(8000f)
        onReset()

        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        return inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remainingBytes = inputBuffer.remaining()
        if (remainingBytes == 0) return

        if (!isEnabled) {
            // Режим выключен — сквозной пропуск без спектральной задержки
            val output = replaceOutputBuffer(remainingBytes)
            output.put(inputBuffer)
            output.flip()
            return
        }

        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val numSamples = remainingBytes / 4 // 16-bit stereo = 4 bytes per sample pair

        // 1. Помещаем входящие сэмплы во входной FIFO
        for (i in 0 until numSamples) {
            if (inFifoCount < inFifoL.size) {
                inFifoL[inFifoCount] = inputBuffer.short.toFloat()
                inFifoR[inFifoCount] = inputBuffer.short.toFloat()
                inFifoCount++
            }
        }

        // 2. Обрабатываем полными фреймами по 512 сэмплов через STFT
        while (inFifoCount >= HOP_SIZE) {
            processHop()
        }

        // 3. Выгружаем готовые сэмплы из выходного FIFO
        val outSamplesAvailable = outFifoCount
        val outputBytes = outSamplesAvailable * 4
        val output = replaceOutputBuffer(outputBytes)
        output.order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until outSamplesAvailable) {
            val l = outFifoL[outFifoRead].toInt().coerceIn(-32768, 32767).toShort()
            val r = outFifoR[outFifoRead].toInt().coerceIn(-32768, 32767).toShort()
            outFifoRead = (outFifoRead + 1) % outFifoL.size
            outFifoCount--

            output.putShort(l)
            output.putShort(r)
        }

        output.flip()
    }

    private fun processHop() {
        // Сдвигаем историю на 512 сэмплов влево и добавляем 512 новых сэмплов
        System.arraycopy(inHistoryL, HOP_SIZE, inHistoryL, 0, HOP_SIZE)
        System.arraycopy(inHistoryR, HOP_SIZE, inHistoryR, 0, HOP_SIZE)
        System.arraycopy(inFifoL, 0, inHistoryL, HOP_SIZE, HOP_SIZE)
        System.arraycopy(inFifoR, 0, inHistoryR, HOP_SIZE, HOP_SIZE)

        // Удаляем 512 сэмплов из inFifo
        inFifoCount -= HOP_SIZE
        if (inFifoCount > 0) {
            System.arraycopy(inFifoL, HOP_SIZE, inFifoL, 0, inFifoCount)
            System.arraycopy(inFifoR, HOP_SIZE, inFifoR, 0, inFifoCount)
        }

        // Применяем окно синуса
        for (i in 0 until FFT_SIZE) {
            val w = sineWindow[i]
            realL[i] = inHistoryL[i] * w
            imagL[i] = 0f
            realR[i] = inHistoryR[i] * w
            imagR[i] = 0f
        }

        // Прямое БПФ для обоих каналов
        fft.fft(realL, imagL)
        fft.fft(realR, imagR)

        // Спектральная маска подавления центрального вокала
        val binHz = sampleRateHz / FFT_SIZE
        val halfN = FFT_SIZE / 2

        for (k in 0..halfN) {
            val freq = k * binHz

            // Суб-бас (< 130 Гц) и ультра-верха (> 8500 Гц) оставляем нетронутыми
            if (freq < 130f || freq > 8500f) continue

            val rL = realL[k]; val iL = imagL[k]
            val rR = realR[k]; val iR = imagR[k]

            val magL = Math.sqrt((rL * rL + iL * iL).toDouble()).toFloat()
            val magR = Math.sqrt((rR * rR + iR * iR).toDouble()).toFloat()

            if (magL < 1e-4f || magR < 1e-4f) continue

            // Фазовая когерентность: скалярное произведение фазовых векторов
            val dot = rL * rR + iL * iR
            val cosPhase = (dot / (magL * magR)).coerceIn(-1f, 1f)

            // Симметрия амплитуд
            val minMag = minOf(magL, magR)
            val maxMag = maxOf(magL, magR)
            val magSym = minMag / (maxMag + 1e-5f)

            // Коэффициент центрированности (1.0 = точный моно-вокал по центру)
            val centerMetric = (maxOf(0f, cosPhase) * magSym).coerceIn(0f, 1f)

            // Кривая подавления: убираем центральную составляющую в этой частотной точке
            val mask = Math.pow((1.0 - centerMetric.toDouble()), 2.2).toFloat().coerceIn(0.04f, 1.0f)

            realL[k] = rL * mask
            imagL[k] = iL * mask
            realR[k] = rR * mask
            imagR[k] = iR * mask

            if (k > 0 && k < halfN) {
                // Комплексно-сопряженная симметрия для обратного БПФ
                val symK = FFT_SIZE - k
                realL[symK] = realL[k]
                imagL[symK] = -imagL[k]
                realR[symK] = realR[k]
                imagR[symK] = -imagR[k]
            }
        }

        // Обратное БПФ (IFFT)
        fft.ifft(realL, imagL)
        fft.fftScale(realL)
        fft.ifft(realR, imagR)
        fft.fftScale(realR)

        // Синтез с окном и накопление Overlap-Add
        for (i in 0 until FFT_SIZE) {
            val w = sineWindow[i]
            outAccumL[i] += realL[i] * w
            outAccumR[i] += realR[i] * w
        }

        // Отправляем первые 512 сэмплов в выходной FIFO
        for (i in 0 until HOP_SIZE) {
            if (outFifoCount < outFifoL.size) {
                outFifoL[outFifoWrite] = outAccumL[i]
                outFifoR[outFifoWrite] = outAccumR[i]
                outFifoWrite = (outFifoWrite + 1) % outFifoL.size
                outFifoCount++
            }
        }

        // Сдвигаем буфер накопления на 512 сэмплов
        System.arraycopy(outAccumL, HOP_SIZE, outAccumL, 0, HOP_SIZE)
        System.arraycopy(outAccumR, HOP_SIZE, outAccumR, 0, HOP_SIZE)
        for (i in HOP_SIZE until FFT_SIZE) {
            outAccumL[i] = 0f
            outAccumR[i] = 0f
        }
    }

    override fun onReset() {
        inHistoryL.fill(0f)
        inHistoryR.fill(0f)
        outAccumL.fill(0f)
        outAccumR.fill(0f)
        inFifoL.fill(0f)
        inFifoR.fill(0f)
        outFifoL.fill(0f)
        outFifoR.fill(0f)
        inFifoCount = 0
        outFifoRead = 0
        outFifoWrite = 0
        outFifoCount = 0
    }

    /** Быстрое вещественное БПФ Radix-2 с предрассчитанной таблицей синусов/косинусов. */
    private class FastRealFft(val n: Int) {
        private val cosTable = FloatArray(n / 2)
        private val sinTable = FloatArray(n / 2)
        private val bitRev = IntArray(n)
        private val invN = 1.0f / n

        init {
            val log2n = Integer.numberOfTrailingZeros(n)
            for (i in 0 until n) {
                var rev = 0
                var temp = i
                for (j in 0 until log2n) {
                    rev = (rev shl 1) or (temp and 1)
                    temp = temp shr 1
                }
                bitRev[i] = rev
            }
            for (i in 0 until n / 2) {
                val angle = (-2.0 * Math.PI * i / n)
                cosTable[i] = Math.cos(angle).toFloat()
                sinTable[i] = Math.sin(angle).toFloat()
            }
        }

        fun fft(real: FloatArray, imag: FloatArray) {
            for (i in 0 until n) {
                val j = bitRev[i]
                if (j > i) {
                    val tr = real[i]; real[i] = real[j]; real[j] = tr
                    val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
                }
            }

            var len = 2
            while (len <= n) {
                val half = len / 2
                val step = n / len
                var i = 0
                while (i < n) {
                    var k = 0
                    for (j in 0 until half) {
                        val c = cosTable[k]
                        val s = sinTable[k]
                        val tr = c * real[i + j + half] - s * imag[i + j + half]
                        val ti = s * real[i + j + half] + c * imag[i + j + half]
                        real[i + j + half] = real[i + j] - tr
                        imag[i + j + half] = imag[i + j] - ti
                        real[i + j] += tr
                        imag[i + j] += ti
                        k += step
                    }
                    i += len
                }
                len = len shl 1
            }
        }

        fun ifft(real: FloatArray, imag: FloatArray) {
            for (i in 0 until n) {
                imag[i] = -imag[i]
            }
            fft(real, imag)
            for (i in 0 until n) {
                imag[i] = -imag[i]
            }
        }

        fun fftScale(real: FloatArray) {
            for (i in 0 until n) {
                real[i] *= invN
            }
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
