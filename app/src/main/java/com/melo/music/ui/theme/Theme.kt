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
    primary = Color(0xFFA3E3B7),
    onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF12512E),
    onPrimaryContainer = Color(0xFFC6FED6),
    secondary = Color(0xFFC5D9CB),
    secondaryContainer = Color(0xFF3A4B3F),
    onSecondaryContainer = Color(0xFFE1F6E7),
    tertiary = Color(0xFF1E4D54),
    tertiaryContainer = Color(0xFF1E4D54),
    onTertiaryContainer = Color(0xFFCEF8FF),
    background = Color(0xFF101411),
    surface = Color(0xFF101411),
    surfaceVariant = Color(0xFF272B28),
    surfaceContainerLow = Color(0xFF181C1A),
    surfaceContainer = Color(0xFF1C211E),
    surfaceContainerHigh = Color(0xFF272B28),
    surfaceContainerHighest = Color(0xFF313633),
    onSurface = Color(0xFFDEE4E0),
    onSurfaceVariant = Color(0xFFCBD8CE),
    outline = Color(0xFFA2AEA5),
    outlineVariant = Color(0xFF6E7A71),
    inverseSurface = Color(0xFFDEE4E0),
    inverseOnSurface = Color(0xFF2D312E),
    inversePrimary = Color(0xFF2E6A45),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
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
