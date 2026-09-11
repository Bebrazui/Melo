package com.melo.desktop.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ── Фирменная тёмно-хвойная палитра Melo (Material 3 Expressive) ─────────────
val MeloDarkBackground = Color(0xFF101512)
val MeloDarkSurface = Color(0xFF161E19)
val MeloCardSurface = Color(0xFF1F2B23)
val MeloSurfaceVariant = Color(0xFF26362C)
val MeloPrimary = Color(0xFF98DFAC)              // Мятно-шалфейный акцент плеера
val MeloOnPrimary = Color(0xFF0A2012)
val MeloPrimaryContainer = Color(0xFF2B4134)
val MeloSecondaryContainer = Color(0xFF24362B)
val MeloTextWhite = Color(0xFFF1F5F2)
val MeloTextSub = Color(0xFFA1B5A8)
val MeloYoutubeRed = Color(0xFFEF4444)
val MeloSoundCloudOrange = Color(0xFFFF5500)

private val MeloDarkColorScheme = darkColorScheme(
    primary = MeloPrimary,
    onPrimary = MeloOnPrimary,
    primaryContainer = MeloPrimaryContainer,
    onPrimaryContainer = Color(0xFFC7F2D3),

    secondary = Color(0xFFA7E8BA),
    onSecondary = Color(0xFF133821),
    secondaryContainer = MeloSecondaryContainer,
    onSecondaryContainer = Color(0xFFE2F7E7),

    background = MeloDarkBackground,
    onBackground = MeloTextWhite,

    surface = MeloDarkSurface,
    onSurface = MeloTextWhite,
    surfaceVariant = MeloSurfaceVariant,
    onSurfaceVariant = MeloTextSub,

    outline = Color(0xFF334639),
    outlineVariant = Color(0xFF1F2E25),
)

val MeloExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun MeloDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MeloDarkColorScheme,
        shapes = MeloExpressiveShapes,
        content = content,
    )
}
