package com.melo.music.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Сочная «музыкальная» палитра-фолбэк (когда динамический цвет недоступен/выключен).
private val Purple = Color(0xFF7C4DFF)
private val Magenta = Color(0xFFE5446D)
private val Teal = Color(0xFF00C2A8)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB69DFF),
    onPrimary = Color(0xFF2A1769),
    primaryContainer = Color(0xFF45348A),
    onPrimaryContainer = Color(0xFFE8DEFF),
    secondary = Color(0xFFFF9CB6),
    secondaryContainer = Color(0xFF6E2742),
    tertiary = Color(0xFF5FE3CE),
    background = Color(0xFF12101A),
    surface = Color(0xFF1A1722),
    surfaceVariant = Color(0xFF332F40),
)

private val LightColors = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DEFF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = Magenta,
    secondaryContainer = Color(0xFFFFD9E1),
    tertiary = Teal,
    tertiaryContainer = Color(0xFFB6FFEF),
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDE7F4),
)

// Крупные «expressive»-скругления.
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun MeloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = ExpressiveShapes,
        content = content,
    )
}
