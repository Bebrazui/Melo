package com.melo.music.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.melo.music.ui.theme.bouncyOverscroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.material.icons.rounded.Waves
import com.melo.music.byedpi.ByeDpiProxy
import com.melo.music.ui.sound.ClickFeedback
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.music.audio.EqualizerManager
import com.melo.music.settings.AppSettings
import com.melo.music.settings.IconPreset

@Composable
fun SettingsScreen(
    scGetId: () -> String?,
    onScSetManual: suspend (String) -> Boolean,
    onScRefresh: suspend () -> String?,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1411))
            .bouncyOverscroll()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        // Выразительная шапка (Material 3 Expressive Header)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(
                "Настройки",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    letterSpacing = (-0.5).sp,
                ),
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
        }

        // ── 🎛️ Эквалайзер (Bento Card) ───────────────────────────
        EqualizerSection()

        Spacer(Modifier.height(16.dp))

        // ── 🎧 Пространственный звук 3D (Bento Card) ──────────────
        SpatialAudioSection()

        Spacer(Modifier.height(16.dp))

        // ── 🔊 Усиление и Реверберация (Bento Card) ──────────────
        GainReverbSection()

        Spacer(Modifier.height(16.dp))

        // ── 🎤 Тексты и Караоке (Bento Card) ─────────────────────
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.primaryContainer,
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Караоке-подсветка",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Слова песни загораются в такт музыке",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
                Switch(
                    checked = AppSettings.karaoke,
                    onCheckedChange = {
                        ClickFeedback.play()
                        AppSettings.updateKaraoke(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = cs.onPrimary,
                        checkedTrackColor = cs.primary,
                    ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 📳 Тактильный отклик и щелчок (Bento Card) ────────────
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.primaryContainer,
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Тактильный отклик",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Звук щелчка и виброотклик при нажатии кнопок",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
                Switch(
                    checked = com.melo.music.settings.AppSettings.hapticFeedback,
                    onCheckedChange = {
                        com.melo.music.settings.AppSettings.updateHapticFeedback(it)
                        if (it) ClickFeedback.play()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = cs.onPrimary,
                        checkedTrackColor = cs.primary,
                    ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 📥 Автозагрузка избранного (Bento Card) ───────────────
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.primaryContainer,
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.DownloadForOffline,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Скачивать избранное",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Автоматически сохранять лайкнутые треки для офлайн-прослушивания",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
                Switch(
                    checked = com.melo.music.settings.AppSettings.autoDownloadFavorites,
                    onCheckedChange = {
                        com.melo.music.settings.AppSettings.updateAutoDownloadFavorites(it)
                        ClickFeedback.play()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = cs.onPrimary,
                        checkedTrackColor = cs.primary,
                    ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 💿 Виниловая пластинка (Bento Card) ───────────────────
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.primaryContainer,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Album,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Виниловая пластинка",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Крутящаяся виниловая пластинка с дорожками вместо квадратной обложки в плеере",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
                Switch(
                    checked = com.melo.music.settings.AppSettings.vinylRecord,
                    onCheckedChange = {
                        ClickFeedback.play()
                        com.melo.music.settings.AppSettings.updateVinylRecord(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = cs.onPrimary,
                        checkedTrackColor = cs.primary,
                    ),
                )
            }
        }

        // ── 🛡️ Обход блокировок (ByeDPI) — скрыт в сборке RuStore ──
        if (com.melo.music.BuildConfig.FLAVOR != "ruStore") {
            Spacer(Modifier.height(16.dp))
            var byedpiActive by remember { mutableStateOf(ByeDpiProxy.isEnabled()) }
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = cs.primaryContainer,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.VpnLock,
                                contentDescription = null,
                                tint = cs.onPrimaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Обход блокировок (ByeDPI)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Автономный обход ограничений без VPN. Можно отключить при нестабильной мобильной сети/в машине.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    }
                    Switch(
                        checked = byedpiActive,
                        onCheckedChange = {
                            ClickFeedback.play()
                            byedpiActive = it
                            ByeDpiProxy.setEnabled(it)
                            if (it) {
                                Thread { ByeDpiProxy.start() }.start()
                            } else {
                                ByeDpiProxy.stop()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = cs.onPrimary,
                            checkedTrackColor = cs.primary,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 🔍 Масштаб интерфейса (DPI) ──────────────────────────
        val currentScale = com.melo.music.settings.AppSettings.uiScale
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = cs.primaryContainer,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AspectRatio,
                                contentDescription = null,
                                tint = cs.onPrimaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Масштаб интерфейса (DPI)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Регулировка размера элементов и текста под магнитолы и экраны с высоким DPI",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    }
                    Text(
                        "${kotlin.math.round(currentScale * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.primary,
                    )
                }

                Spacer(Modifier.height(14.dp))

                Slider(
                    value = currentScale,
                    onValueChange = {
                        com.melo.music.settings.AppSettings.updateUiScale(it)
                    },
                    onValueChangeFinished = {
                        ClickFeedback.play()
                    },
                    valueRange = 0.75f..1.35f,
                    colors = SliderDefaults.colors(
                        thumbColor = cs.primary,
                        activeTrackColor = cs.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(6.dp))

                // Быстрые пресеты
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "85%" to 0.85f,
                        "100%" to 1.00f,
                        "115%" to 1.15f,
                        "130%" to 1.30f,
                    ).forEach { (label, presetScale) ->
                        val isSelected = kotlin.math.abs(currentScale - presetScale) < 0.03f
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) cs.primary else Color.White.copy(alpha = 0.08f),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    ClickFeedback.play()
                                    com.melo.music.settings.AppSettings.updateUiScale(presetScale)
                                }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) cs.onPrimary else Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 🎨 Иконка приложения (Bento Card) ────────────────────
        IconPickerSection()

        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun SpatialAudioSection() {
    var enabled by remember { mutableStateOf(EqualizerManager.isSpatialEnabled()) }
    var strength by remember { mutableIntStateOf(EqualizerManager.getSpatialStrength()) }
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.primaryContainer,
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Пространственный звук 3D",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (enabled) "Melo 3D Surround активен" else "Выключен",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) cs.primary else Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        ClickFeedback.play()
                        enabled = it
                        EqualizerManager.setSpatialEnabled(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = cs.onPrimary,
                        checkedTrackColor = cs.primary,
                    ),
                )
            }

            AnimatedVisibility(
                visible = enabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(Modifier.height(18.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Глубина виртуализации сцены",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f),
                        )
                        Surface(
                            shape = CircleShape,
                            color = cs.primaryContainer.copy(alpha = 0.7f),
                        ) {
                            Text(
                                "${strength / 10}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = cs.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Slider(
                        value = strength.toFloat(),
                        onValueChange = {
                            strength = it.toInt()
                            EqualizerManager.setSpatialStrength(strength)
                        },
                        valueRange = 0f..1000f,
                        colors = SliderDefaults.colors(
                            thumbColor = cs.primary,
                            activeTrackColor = cs.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun GainReverbSection() {
    var gain by remember { mutableIntStateOf(EqualizerManager.getGain()) }
    var reverb by remember { mutableIntStateOf(EqualizerManager.getReverbPreset()) }
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // ── Усиление (Gain) ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.primaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Усиление звука",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = cs.primaryContainer.copy(alpha = 0.7f),
                ) {
                    Text(
                        "+${gain / 100} dB",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Slider(
                value = gain.toFloat(),
                onValueChange = {
                    gain = it.toInt()
                    EqualizerManager.setGain(gain)
                },
                valueRange = 0f..EqualizerManager.MAX_GAIN_MB.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = cs.primary,
                    activeTrackColor = cs.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(18.dp))

            // ── Реверберация (Reverb) ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.primaryContainer,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Waves,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Пространство",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = cs.primaryContainer.copy(alpha = 0.7f),
                ) {
                    Text(
                        EqualizerManager.reverbPresetNames.getOrElse(reverb) { "Выкл" },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Slider(
                value = reverb.toFloat(),
                onValueChange = {
                    reverb = Math.round(it)
                    EqualizerManager.setReverbPreset(reverb)
                },
                valueRange = 0f..EqualizerManager.reverbPresetNames.lastIndex.toFloat(),
                steps = EqualizerManager.reverbPresetNames.size - 2,
                colors = SliderDefaults.colors(
                    thumbColor = cs.primary,
                    activeTrackColor = cs.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EqualizerSection() {
    var enabled by remember { mutableStateOf(EqualizerManager.isEnabled()) }
    var selectedPreset by remember { mutableIntStateOf(EqualizerManager.getPreset()) }
    val bandCount = EqualizerManager.bandCount
    val bandRange = EqualizerManager.bandLevelRange
    val frequencies = EqualizerManager.bandFrequencies
    val presets = EqualizerManager.presetNames
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.primaryContainer,
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Эквалайзер",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (enabled) "Активен" else "Выключен",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) cs.primary else Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        ClickFeedback.play()
                        enabled = it
                        EqualizerManager.setEnabled(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = cs.onPrimary,
                        checkedTrackColor = cs.primary,
                    ),
                )
            }

            AnimatedVisibility(
                visible = enabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(Modifier.height(18.dp))

                    Text(
                        "Пресеты звучания",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    Spacer(Modifier.height(10.dp))

                    // Горизонтальный скролл пресетов в виде тактильных пилюль
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        presets.forEachIndexed { index, name ->
                            val isSelected = selectedPreset == index
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) cs.primary else Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) cs.primary else Color.White.copy(alpha = 0.08f),
                                ),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        selectedPreset = index
                                        EqualizerManager.setPreset(index)
                                    },
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) cs.onPrimary else Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // Полосы частот
                    if (bandCount > 0) {
                        val levels = remember(enabled, selectedPreset) {
                            (0 until bandCount).map { EqualizerManager.getBandLevel(it).toInt() }
                        }
                        var bandLevels by remember { mutableStateOf(levels) }

                        Text(
                            "Точная настройка частот",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                        Spacer(Modifier.height(10.dp))

                        bandLevels.forEachIndexed { index, level ->
                            val freq = frequencies.getOrElse(index) { 0 }
                            val freqLabel = if (freq >= 1000) "${freq / 1000} kHz" else "$freq Hz"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            ) {
                                Text(
                                    freqLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.width(56.dp),
                                )
                                Slider(
                                    value = level.toFloat(),
                                    onValueChange = { newLevel ->
                                        bandLevels = bandLevels.toMutableList().apply { set(index, newLevel.toInt()) }
                                    },
                                    onValueChangeFinished = {
                                        EqualizerManager.setBandLevel(index, bandLevels[index].toShort())
                                    },
                                    valueRange = bandRange[0].toFloat()..bandRange[1].toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = cs.primary,
                                        activeTrackColor = cs.primary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                                    ),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${bandLevels[index] / 100} dB",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = cs.primary,
                                    modifier = Modifier.width(46.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconPickerSection() {
    val context = LocalContext.current
    var current by remember { mutableStateOf(AppSettings.launcherIcon) }
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Иконка приложения",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Выберите стиль иконки для рабочего стола",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f),
            )

            Spacer(Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconPreset.entries.forEach { preset ->
                    val isSelected = current == preset.id
                    val iconRes = when (preset) {
                        IconPreset.DEFAULT -> com.melo.music.R.drawable.ic_preview_classic
                        IconPreset.THORNS -> com.melo.music.R.drawable.ic_preview_thorns
                        IconPreset.INVERTED -> com.melo.music.R.drawable.ic_preview_inverted
                        IconPreset.IOS6 -> com.melo.music.R.drawable.ic_preview_ios6
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (!isSelected) {
                                    AppSettings.switchIcon(context, preset.id)
                                    current = preset.id
                                }
                            }
                            .padding(4.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(68.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) cs.primaryContainer else Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) cs.primary else Color.White.copy(alpha = 0.1f),
                                ),
                                modifier = Modifier.size(64.dp),
                            ) {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(iconRes),
                                    contentDescription = preset.label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                                )
                            }
                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = cs.primary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.BottomEnd),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = cs.onPrimary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            preset.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) cs.primary else Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            if (current != IconPreset.DEFAULT.id) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Иконка изменится после перезапуска лаунчера",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.primary.copy(alpha = 0.8f),
                )
            }
        }
    }
}

