package com.melo.music.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.melo.music.R
import com.melo.music.settings.AppSettings

/**
 * Менеджер звукового и тактильного отклика при нажатиях на кнопки интерфейса.
 */
object ClickFeedback {

    private var soundPool: SoundPool? = null
    private var soundId: Int = 0
    private var isLoaded = false
    private var vibrator: Vibrator? = null

    fun init(context: Context) {
        val appContext = context.applicationContext
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()

            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    isLoaded = true
                }
            }

            soundId = pool.load(appContext, R.raw.ui_click, 1)
            soundPool = pool

            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Проиграть звуковой щелчок и тактильный виброотклик при клике */
    fun play() {
        if (!AppSettings.hapticFeedback) return

        // 1. Короткий звуковой щелчок (SoundPool)
        try {
            if (isLoaded && soundId != 0) {
                soundPool?.play(soundId, 0.9f, 0.9f, 1, 0, 1.0f)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Тактильный отклик (Haptic Click)
        try {
            val v = vibrator ?: return
            if (v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(15)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
