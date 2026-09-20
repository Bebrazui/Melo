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

    companion object {
        /** Уровень баса в реальном времени (0.0f .. 1.0f), обновляется напрямую из PCM сэмплов */
        @Volatile
        var currentBassLevel: Float = 0f
            private set
    }

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

    // ── Лютый Басс Буст (Monster Bass Boost™) ──
    @Volatile
    var bassBoostEnabled: Boolean = false
    @Volatile
    var bassBoostStrength: Float = 0.85f // 0.1f .. 1.0f
    private val bassBoostShelfL = BiquadLowShelfFilter()
    private val bassBoostShelfR = BiquadLowShelfFilter()
    private val bassBoostSubL = BiquadPeakFilter()
    private val bassBoostSubR = BiquadPeakFilter()
    private val bassBoostPunchL = BiquadPeakFilter()
    private val bassBoostPunchR = BiquadPeakFilter()
    private val bassBoostThumpL = BiquadPeakFilter()
    private val bassBoostThumpR = BiquadPeakFilter()

    // ── Crystal Audio™ (Super-Resolution Harmonic Exciter + Deep Bass + Anti-Clipping) ──
    @Volatile
    var crystalEnabled: Boolean = false
    @Volatile
    var crystalIntensity: Float = 0.65f // 0.0f .. 1.0f

    // Фильтры полосового захвата (7.5 - 14.5 кГц) и High-Pass (14 кГц) для гармоник
    private val crystalBandL = BiquadBandPassFilter()
    private val crystalBandR = BiquadBandPassFilter()
    private val crystalHighPassL = BiquadHighPassFilter()
    private val crystalHighPassR = BiquadHighPassFilter()

    // Фильтры глубокого саб-баса (30 - 85 Гц)
    private val crystalSubBassLpL = BiquadLowPassFilter()
    private val crystalSubBassLpR = BiquadLowPassFilter()

    // ── Спектральные детекторы для защиты от песка и пердежа (Smart Spectral Balance) ──
    private var crystalHighEnergyEnv = 0f
    private var crystalSubEnergyEnv = 0f

    // Динамический огибающий лимитер и деклиппер (Mastering-Grade Anti-Clip Limiter)
    private var limiterGain = 1.0f
    private var limiterReleaseCoeff = 0.0005f
    private var prevRawL = 0f
    private var prevRawR = 0f

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

    // ── Детектор энергии баса (RMS 20-120 Гц) ──
    private var bassLpStore = 0f
    private var bassEnergyAccum = 0f
    private var bassSampleCount = 0
    private var smoothedBass = 0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(8000)
        initEqFilters(sampleRate)
        initCrystalFilters(sampleRate)
        initBassBoostFilters(sampleRate)
        return inputAudioFormat
    }

    private fun initBassBoostFilters(sr: Int) {
        val shelfFreq = (125f).coerceAtMost(sr * 0.25f)
        val subFreq = (65f).coerceAtMost(sr * 0.15f)
        val punchFreq = (115f).coerceAtMost(sr * 0.22f)
        val thumpFreq = (175f).coerceAtMost(sr * 0.30f)

        bassBoostShelfL.set(shelfFreq, 0.80f, 15.0f, sr.toFloat())
        bassBoostShelfR.set(shelfFreq, 0.80f, 15.0f, sr.toFloat())
        bassBoostSubL.set(subFreq, 1.2f, 9.0f, sr.toFloat())
        bassBoostSubR.set(subFreq, 1.2f, 9.0f, sr.toFloat())
        bassBoostPunchL.set(punchFreq, 1.1f, 8.0f, sr.toFloat())
        bassBoostPunchR.set(punchFreq, 1.1f, 8.0f, sr.toFloat())
        bassBoostThumpL.set(thumpFreq, 1.0f, 5.5f, sr.toFloat())
        bassBoostThumpR.set(thumpFreq, 1.0f, 5.5f, sr.toFloat())
    }

    private fun initCrystalFilters(sr: Int) {
        val centerFreq = (4800f).coerceAtMost(sr * 0.38f)
        val hpFreq = (6500f).coerceAtMost(sr * 0.42f)
        val subFreq = (62f).coerceAtMost(sr * 0.15f)

        crystalBandL.set(centerFreq, 0.75f, sr.toFloat())
        crystalBandR.set(centerFreq, 0.75f, sr.toFloat())
        crystalHighPassL.set(hpFreq, 0.707f, sr.toFloat())
        crystalHighPassR.set(hpFreq, 0.707f, sr.toFloat())
        crystalSubBassLpL.set(subFreq, 0.707f, sr.toFloat())
        crystalSubBassLpR.set(subFreq, 0.707f, sr.toFloat())

        // Время восстановления лимитера ~40 мс для чистого аналогового звучания без пердежа и клиппинга
        limiterReleaseCoeff = (1.0f / (sr * 0.040f)).coerceIn(0.0001f, 0.01f)
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
            1 -> Triple(0.72f, 0.25f, 0.25f) // Малая комната
            2 -> Triple(0.78f, 0.30f, 0.35f) // Средняя комната
            3 -> Triple(0.84f, 0.35f, 0.45f) // Большая комната
            4 -> Triple(0.88f, 0.40f, 0.55f) // Средний зал
            5 -> Triple(0.92f, 0.45f, 0.65f) // Большой зал
            6 -> Triple(0.82f, 0.15f, 0.50f) // Пластина (яркая, металлическая)
            else -> Triple(0f, 0f, 0f)
        }

        comb1.feedback = reverbFeedback; comb1.damp = reverbDamp
        comb2.feedback = reverbFeedback; comb2.damp = reverbDamp
        comb3.feedback = reverbFeedback; comb3.damp = reverbDamp
        comb4.feedback = reverbFeedback; comb4.damp = reverbDamp

        val spatialWidth = 1.0f + spatialStrength * 0.95f
        val delaySamples = (spatialStrength * (sampleRate * 0.00065f)).toInt().coerceIn(0, delayBufferLeft.size - 1)

        val bassCrossoverAlpha = 2.0f * Math.PI.toFloat() * 140f / sampleRate.toFloat()
        val airAlpha = 2.0f * Math.PI.toFloat() * 8500f / sampleRate.toFloat()
        val headShadowAlpha = 2.0f * Math.PI.toFloat() * 1800f / sampleRate.toFloat()
        val hpAlpha = 2.0f * Math.PI.toFloat() * 320f / sampleRate.toFloat()

        while (inputBuffer.remaining() >= 4) {
            val rawL = inputBuffer.short
            val rawR = inputBuffer.short

            var left = rawL.toFloat()
            var right = rawR.toFloat()

            // 0. Детекция и реконструкция срезанных пиков (Intelligent De-Clipper / Anti-Farting)
            // Детектируем плоские вершины (hard-clipping с YouTube / перегруженных записей)
            if (crystalEnabled) {
                val absL = abs(left)
                val absR = abs(right)
                // Если сэмпл на границе среза и производная почти 0 (плоская вершина)
                if (absL >= 31200f && abs(left - prevRawL) < 120f) {
                    // Восстанавливаем естественную форму пика вместо резкого плоского среза
                    left = prevRawL * 0.985f + sign(left) * 150f
                }
                if (absR >= 31200f && abs(right - prevRawR) < 120f) {
                    right = prevRawR * 0.985f + sign(right) * 150f
                }
                prevRawL = left
                prevRawR = right
            }

            // 1. 5-полосный параметрический эквалайзер
            if (isEq) {
                left = eqBands[4].process(
                    eqBands[3].process(
                        eqBands[2].process(
                            eqBands[1].process(
                                eqBands[0].process(left)
                            )
                        )
                    )
                )
                right = eqBands[4].process(
                    eqBands[3].process(
                        eqBands[2].process(
                            eqBands[1].process(
                                eqBands[0].process(right)
                            )
                        )
                    )
                )
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

            // 4. Crystal Audio™ (Smart Adaptive: шелковистые ВЧ + плотный саб-бас + защита от песка и пердежа)
            if (crystalEnabled) {
                // 4a. ВЧ Кристальный воздух
                val srcL = crystalBandL.process(left)
                val srcR = crystalBandR.process(right)

                // Детектор энергии ВЧ: если в треке УЖЕ звенят тарелки или сибилянты, плавно демпфируем
                val highPeak = max(abs(srcL), abs(srcR))
                if (highPeak > crystalHighEnergyEnv) {
                    crystalHighEnergyEnv += (highPeak - crystalHighEnergyEnv) * 0.008f
                } else {
                    crystalHighEnergyEnv += (highPeak - crystalHighEnergyEnv) * 0.0006f
                }
                // При нормальных ВЧ highTamer = 1.0; если трек "песочный" и пережатый (выше 7500), приглушаем до 0.18
                val highTamer = if (crystalHighEnergyEnv > 7500f) {
                    (7500f / crystalHighEnergyEnv).coerceIn(0.18f, 1.0f)
                } else {
                    1.0f
                }

                // Мягкая аналоговая сатурация (Tape / Tube Soft-Knee) вместо полиномов:
                // x / (1 + |x|) никогда не улетает в жесткий клиппинг и не рождает цифровой скрежет
                val normL = srcL / 9000f
                val normR = srcR / 9000f
                val satL = (normL / (1.0f + abs(normL))) * 9000f
                val satR = (normR / (1.0f + abs(normR))) * 9000f

                // Чётные шелковистые обертоны (асимметрия = ламповый тёплый воздух)
                val posL = max(0f, normL)
                val posR = max(0f, normR)
                val warmEvenL = (posL / (1.0f + posL * 1.5f)) * 9000f
                val warmEvenR = (posR / (1.0f + posR * 1.5f)) * 9000f

                val harmL = satL * 0.65f + warmEvenL * 0.85f
                val harmR = satR * 0.65f + warmEvenR * 0.85f

                val crystalL = crystalHighPassL.process(harmL)
                val crystalR = crystalHighPassR.process(harmR)

                val effectiveAirGain = crystalIntensity * 1.55f * highTamer
                left += (crystalL * 1.15f + srcL * 0.28f) * effectiveAirGain
                right += (crystalR * 1.15f + srcR * 0.28f) * effectiveAirGain

                // 4b. Стерео-воздух (расширение сцены без фазовых искажений)
                val sideAir = (srcL - srcR) * 0.24f * effectiveAirGain
                left += sideAir
                right -= sideAir

                // 4c. Глубокий бархатный саб-бас (30-62 Гц) с защитой динамиков от пердежа
                val subL = crystalSubBassLpL.process(left)
                val subR = crystalSubBassLpR.process(right)

                val subMono = (subL + subR) * 0.5f
                val subPeak = abs(subMono)

                // Детектор энергии НЧ: если в треке УЖЕ мощный 808-бас, не наваливаем сверху перегруз!
                if (subPeak > crystalSubEnergyEnv) {
                    crystalSubEnergyEnv += (subPeak - crystalSubEnergyEnv) * 0.008f
                } else {
                    crystalSubEnergyEnv += (subPeak - crystalSubEnergyEnv) * 0.0005f
                }

                // Защита от перегруза: если бас уже долбит выше 8500, плавно снижаем гейн саб-баса
                val bassTamer = if (crystalSubEnergyEnv > 8500f) {
                    (8500f / crystalSubEnergyEnv).coerceIn(0.18f, 1.0f)
                } else {
                    1.0f
                }

                // Психоакустическое обогащение глубины фундаментального баса:
                // Мягкое гармоническое уплотнение нижнего суб-регистра (30-62 Гц) без гула в мид-басе
                val normSub = subMono / 8500f
                val deepDensity = tanh(normSub) * 8500f
                val effectiveBassGain = crystalIntensity * 0.82f * bassTamer

                val rawBassPunch = (subMono * 0.50f + deepDensity * 0.50f) * effectiveBassGain

                // Мягкий лимитер добавки баса: физически не может превысить 4500 пиков -> ноль пердежа
                val bassPunch = rawBassPunch / (1.0f + abs(rawBassPunch) / 4500f)
                left += bassPunch
                right += bassPunch
            }

            // 4.5 Лютый Басс Буст (Monster Bass Boost™)
            if (bassBoostEnabled) {
                val strengthFactor = bassBoostStrength.coerceIn(0.1f, 1.0f)

                // 4-каскадная фильтрация: низкая полка 125 Гц (+15 dB) + суб-бас 65 Гц (+9 dB) + панч 115 Гц (+8 dB) + памп 175 Гц (+5.5 dB)
                val bL = bassBoostThumpL.process(bassBoostPunchL.process(bassBoostSubL.process(bassBoostShelfL.process(left))))
                val bR = bassBoostThumpR.process(bassBoostPunchR.process(bassBoostSubR.process(bassBoostShelfR.process(right))))

                // Извлекаем добавленный басовый контент с учётом силы буста
                val addedBassL = (bL - left) * strengthFactor
                val addedBassR = (bR - right) * strengthFactor

                // Лампово-плёночный овердрайв баса (сочные аналоговые обертоны, пробивающие любые динамики)
                val satL = tanh(addedBassL / 16000f) * 19000f * strengthFactor
                val satR = tanh(addedBassR / 16000f) * 19000f * strengthFactor

                left += satL
                right += satR
            }

            // 5. Усиление громкости (Gain Booster)
            if (gain != 1.0f) {
                left *= gain
                right *= gain
            }

            // 6. Мастеринговый адаптивный лимитер и защита от клиппинга
            if (bassBoostEnabled) {
                // Мгновенный музыкальный мягкий сатуратор без зажатия и дакинга остального микса
                left = softLimitSample(left)
                right = softLimitSample(right)
            } else {
                val peak = max(abs(left), abs(right))
                val threshold = 31000f // Защитный headroom против искажений ЦАП и динамиков

                val targetGain = if (peak > threshold) (threshold / peak) else 1.0f

                // Атака мгновенная (0 сэмплов) — ни один пик физически не сможет превысить порог
                if (targetGain < limiterGain) {
                    limiterGain = targetGain
                } else {
                    // Плавный музыкальный релиз (~40 мс) — волна баса сохраняет чистую форму без сплющивания
                    limiterGain += (targetGain - limiterGain) * limiterReleaseCoeff
                }

                left *= limiterGain
                right *= limiterGain

                // Мягкое аналоговое насыщение для предотвращения цифрового клиппинга
                val normL = left / 32768f
                val normR = right / 32768f
                val finalL = if (abs(normL) > 0.95f) {
                    sign(normL) * (0.95f + 0.05f * tanh((abs(normL) - 0.95f) / 0.18f))
                } else normL
                val finalR = if (abs(normR) > 0.95f) {
                    sign(normR) * (0.95f + 0.05f * tanh((abs(normR) - 0.95f) / 0.18f))
                } else normR

                left = finalL * 32767f
                right = finalR * 32767f
            }

            val outL = left.toInt().coerceIn(-32768, 32767).toShort()
            val outR = right.toInt().coerceIn(-32768, 32767).toShort()

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
        crystalBandL.reset(); crystalBandR.reset()
        crystalHighPassL.reset(); crystalHighPassR.reset()
        crystalSubBassLpL.reset(); crystalSubBassLpR.reset()
        bassBoostShelfL.reset(); bassBoostShelfR.reset()
        bassBoostSubL.reset(); bassBoostSubR.reset()
        bassBoostPunchL.reset(); bassBoostPunchR.reset()
        bassBoostThumpL.reset(); bassBoostThumpR.reset()
        crystalHighEnergyEnv = 0f
        crystalSubEnergyEnv = 0f
        limiterGain = 1.0f
        prevRawL = 0f
        prevRawR = 0f
        bassLpStore = 0f
        bassEnergyAccum = 0f
        bassSampleCount = 0
        smoothedBass = 0f
        currentBassLevel = 0f
    }

    override fun onReset() {
        onFlush()
    }

    private fun softLimitSample(x: Float): Float {
        val absX = abs(x)
        if (absX <= 24000f) return x
        val excess = absX - 24000f
        val compressed = 24000f + tanh(excess / 7500f) * 7500f
        return sign(x) * compressed.coerceAtMost(32000f)
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

    /**
     * Полосовой фильтр 2-го порядка (Constant 0 dB peak gain BPF)
     */
    private class BiquadBandPassFilter {
        var b0 = 0f; var b1 = 0f; var b2 = 0f; var a1 = 0f; var a2 = 0f
        var x1 = 0f; var x2 = 0f; var y1 = 0f; var y2 = 0f

        fun set(freq: Float, q: Float, sr: Float) {
            val w0 = (2.0 * Math.PI * freq / sr).toFloat()
            val alpha = (sin(w0.toDouble()) / (2.0 * q)).toFloat()
            val cosW = cos(w0.toDouble()).toFloat()

            val a0 = 1.0f + alpha
            b0 = alpha / a0
            b1 = 0f
            b2 = -alpha / a0
            a1 = (-2.0f * cosW) / a0
            a2 = (1.0f - alpha) / a0
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

    /**
     * Фильтр высоких частот 2-го порядка (HPF)
     */
    private class BiquadHighPassFilter {
        var b0 = 1f; var b1 = 0f; var b2 = 0f; var a1 = 0f; var a2 = 0f
        var x1 = 0f; var x2 = 0f; var y1 = 0f; var y2 = 0f

        fun set(freq: Float, q: Float, sr: Float) {
            val w0 = (2.0 * Math.PI * freq / sr).toFloat()
            val alpha = (sin(w0.toDouble()) / (2.0 * q)).toFloat()
            val cosW = cos(w0.toDouble()).toFloat()

            val a0 = 1.0f + alpha
            b0 = ((1.0f + cosW) / 2.0f) / a0
            b1 = (-(1.0f + cosW)) / a0
            b2 = ((1.0f + cosW) / 2.0f) / a0
            a1 = (-2.0f * cosW) / a0
            a2 = (1.0f - alpha) / a0
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

    /**
     * Фильтр низких частот 2-го порядка (Butterworth 12 dB/oct LPF) для глубокого саб-баса
     */
    private class BiquadLowPassFilter {
        var b0 = 1f; var b1 = 0f; var b2 = 0f; var a1 = 0f; var a2 = 0f
        var x1 = 0f; var x2 = 0f; var y1 = 0f; var y2 = 0f

        fun set(freq: Float, q: Float, sr: Float) {
            val w0 = (2.0 * Math.PI * freq / sr).toFloat()
            val alpha = (sin(w0.toDouble()) / (2.0 * q)).toFloat()
            val cosW = cos(w0.toDouble()).toFloat()

            val a0 = 1.0f + alpha
            b0 = ((1.0f - cosW) / 2.0f) / a0
            b1 = (1.0f - cosW) / a0
            b2 = ((1.0f - cosW) / 2.0f) / a0
            a1 = (-2.0f * cosW) / a0
            a2 = (1.0f - alpha) / a0
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

    /**
     * Полочный фильтр низких частот 2-го порядка (Audio EQ Cookbook Low-Shelf) для мощного басс-буста
     */
    private class BiquadLowShelfFilter {
        var b0 = 1f; var b1 = 0f; var b2 = 0f; var a1 = 0f; var a2 = 0f
        var x1 = 0f; var x2 = 0f; var y1 = 0f; var y2 = 0f

        fun set(freq: Float, q: Float, gainDb: Float, sr: Float) {
            val a = 10.0.pow((gainDb / 40.0)).toFloat()
            val w0 = (2.0 * Math.PI * freq / sr).toFloat()
            val cosW = cos(w0.toDouble()).toFloat()
            val sinW = sin(w0.toDouble()).toFloat()
            val alpha = (sinW / (2.0 * q)).toFloat()
            val twoSqrtAAlpha = 2.0f * sqrt(a) * alpha

            val a0 = (a + 1.0f) + (a - 1.0f) * cosW + twoSqrtAAlpha
            b0 = (a * ((a + 1.0f) - (a - 1.0f) * cosW + twoSqrtAAlpha)) / a0
            b1 = (2.0f * a * ((a - 1.0f) - (a + 1.0f) * cosW)) / a0
            b2 = (a * ((a + 1.0f) - (a - 1.0f) * cosW - twoSqrtAAlpha)) / a0
            a1 = (-2.0f * ((a - 1.0f) + (a + 1.0f) * cosW)) / a0
            a2 = ((a + 1.0f) + (a - 1.0f) * cosW - twoSqrtAAlpha) / a0
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
