package com.melo.desktop.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Фирменная форма Melo «Цветок» / Scalloped Starburst в стиле Material 3 Expressive.
 * В точности как на телефоне: используется для кнопок плеера (Play/Pause, Prev, Next, Art)
 * и акцентных элементов.
 */
class ScallopedShape(
    val petals: Int = 8,
    val depth: Float = 0.14f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = min(cx, cy)
        val minR = maxR * (1f - depth)
        val totalPoints = petals * 2
        val step = (2.0 * Math.PI / totalPoints).toFloat()

        val points = (0 until totalPoints).map { i ->
            val angle = i * step - (Math.PI / 2).toFloat()
            val r = if (i % 2 == 0) maxR else minR
            Offset(
                cx + r * cos(angle.toDouble()).toFloat(),
                cy + r * sin(angle.toDouble()).toFloat(),
            )
        }

        path.moveTo(
            (points[0].x + points.last().x) / 2f,
            (points[0].y + points.last().y) / 2f,
        )

        for (i in points.indices) {
            val p = points[i]
            val next = points[(i + 1) % points.size]
            val midX = (p.x + next.x) / 2f
            val midY = (p.y + next.y) / 2f
            path.quadraticTo(p.x, p.y, midX, midY)
        }

        path.close()
        return Outline.Generic(path)
    }
}
