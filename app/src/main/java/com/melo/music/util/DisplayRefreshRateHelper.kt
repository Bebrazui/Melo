package com.melo.music.util

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import kotlin.math.roundToInt

object DisplayRefreshRateHelper {

    /**
     * Принудительно запрашивает у системы и SurfaceFlinger максимальную доступную
     * частоту обновления дисплея (120 Гц / 144 Гц / 90 Гц).
     *
     * Это гарантирует работу скролла, анимаций Compose и физики жестов с частотой 120 FPS
     * даже на оболочках с агрессивным троттлингом (OriginOS/FuntouchOS, HyperOS, OneUI).
     */
    fun applyRefreshRate(activity: Activity, enableHighRate: Boolean) {
        val window = activity.window ?: return
        val layoutParams = window.attributes

        if (!enableHighRate) {
            layoutParams.preferredDisplayModeId = 0
            layoutParams.preferredRefreshRate = 60f
            window.attributes = layoutParams
            return
        }

        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        }

        val supportedModes = display?.supportedModes ?: emptyArray()
        val currentMode = display?.mode

        // Ищем поддерживаемый режим с максимальной герцовкой при сохранении разрешения экрана
        val maxMode = supportedModes
            .filter { mode ->
                if (currentMode != null) {
                    (mode.physicalWidth == currentMode.physicalWidth && mode.physicalHeight == currentMode.physicalHeight) ||
                    (mode.physicalWidth == currentMode.physicalHeight && mode.physicalHeight == currentMode.physicalWidth)
                } else true
            }
            .maxByOrNull { it.refreshRate }

        if (maxMode != null && maxMode.refreshRate >= 65f) {
            layoutParams.preferredDisplayModeId = maxMode.modeId
            layoutParams.preferredRefreshRate = maxMode.refreshRate
        } else {
            // Фолбэк на стандартную высокую частоту 120 Гц
            layoutParams.preferredRefreshRate = 120f
        }

        window.attributes = layoutParams
    }

    /**
     * Возвращает текущую активную частоту обновления экрана в Гц (напр. 120, 60, 90).
     */
    fun getCurrentRefreshRate(context: Context): Int {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }
        return display?.refreshRate?.roundToInt() ?: 60
    }
}
