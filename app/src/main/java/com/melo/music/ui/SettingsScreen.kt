package com.melo.music.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melo.music.audio.EqualizerManager
import com.melo.music.settings.AppSettings
import com.melo.music.settings.IconPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    scGetId: () -> String?,
    onScSetManual: suspend (String) -> Boolean,
    onScRefresh: suspend () -> String?,
    onBack: () -> Unit,
) {
    // Системный жест «назад» закрывает настройки (на главную).
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Эквалайзер ───────────────────────────────────────────
            EqualizerSection()

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── Усиление + реверберация ──────────────────────────────
            GainReverbSection()

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── Текст песни ──────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Rounded.Lyrics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Караоке-подсветка",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Слова в строке загораются по времени",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                androidx.compose.material3.Switch(
                    checked = AppSettings.karaoke,
                    onCheckedChange = { AppSettings.updateKaraoke(it) },
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Иконка приложения ────────────────────────────────────
            IconPickerSection()

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GainReverbSection() {
    var gain by remember { mutableIntStateOf(EqualizerManager.getGain()) }
    var reverb by remember { mutableIntStateOf(EqualizerManager.getReverbPreset()) }

    Column {
        // ── Усиление ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.VolumeUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Усиление", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                "+${gain / 100} dB",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        androidx.compose.material3.Slider(
            value = gain.toFloat(),
            onValueChange = {
                gain = it.toInt()
                EqualizerManager.setGain(gain)
            },
            valueRange = 0f..EqualizerManager.MAX_GAIN_MB.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        // ── Реверберация ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Waves,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Реверберация", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                EqualizerManager.reverbPresetNames.getOrElse(reverb) { "Выкл" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        androidx.compose.material3.Slider(
            value = reverb.toFloat(),
            onValueChange = {
                reverb = Math.round(it)
                EqualizerManager.setReverbPreset(reverb)
            },
            valueRange = 0f..EqualizerManager.reverbPresetNames.lastIndex.toFloat(),
            steps = EqualizerManager.reverbPresetNames.size - 2,
            modifier = Modifier.fillMaxWidth(),
        )
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

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Эквалайзер", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    EqualizerManager.setEnabled(it)
                },
            )
        }

        AnimatedVisibility(
            visible = enabled,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column {
                Spacer(Modifier.height(10.dp))

                // Пресеты
                Text("Пресет", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.DropdownMenu(
                    expanded = false,
                    onDismissRequest = { },
                ) { }
                // Простой выбор пресета через chips
                var expanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = presets.getOrElse(selectedPreset) { "Пользовательский" },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    presets.forEachIndexed { index, name ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedPreset = index
                                EqualizerManager.setPreset(index)
                                expanded = false
                            },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Полосы эквалайзера
                if (bandCount > 0) {
                    val levels = remember(enabled, selectedPreset) {
                        (0 until bandCount).map { EqualizerManager.getBandLevel(it).toInt() }
                    }
                    var bandLevels by remember { mutableStateOf(levels) }

                    Text(
                        "Частоты: ${frequencies.joinToString { "${it / 1000}kHz" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Диапазон: ${bandRange[0] / 100}…${bandRange[1] / 100} dB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))

                    bandLevels.forEachIndexed { index, level ->
                        val freq = frequencies.getOrElse(index) { 0 }
                        val freqLabel = if (freq >= 1000) "${freq / 1000}kHz" else "${freq}Hz"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                freqLabel,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(48.dp),
                            )
                            androidx.compose.material3.Slider(
                                value = level.toFloat(),
                                onValueChange = { newLevel ->
                                    bandLevels = bandLevels.toMutableList().apply { set(index, newLevel.toInt()) }
                                },
                                onValueChangeFinished = {
                                    EqualizerManager.setBandLevel(index, bandLevels[index].toShort())
                                },
                                valueRange = bandRange[0].toFloat()..bandRange[1].toFloat(),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${bandLevels[index] / 100}dB",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(40.dp),
                            )
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

    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Rounded.Lyrics,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Иконка приложения",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Выберите иконку для лаунчера",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconPreset.entries.forEach { preset ->
                val isSelected = current == preset.id
                val iconRes = when (preset) {
                    IconPreset.DEFAULT -> com.melo.music.R.mipmap.ic_launcher
                    IconPreset.THORNS -> com.melo.music.R.mipmap.ic_launcher_thorns
                    IconPreset.INVERTED -> com.melo.music.R.mipmap.ic_launcher_inverted
                    IconPreset.IOS6 -> com.melo.music.R.mipmap.ic_launcher_ios6
                }
                val bitmap = remember(iconRes) {
                    try {
                        BitmapFactory.decodeResource(context.resources, iconRes)?.asImageBitmap()
                    } catch (_: Exception) { null }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (!isSelected) {
                                AppSettings.switchIcon(context, preset.id)
                                current = preset.id
                            }
                        }
                        .padding(4.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(68.dp)
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            ),
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap!!,
                                contentDescription = preset.label,
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        preset.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (current != IconPreset.DEFAULT.id) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Иконка изменится после перезапуска приложения",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
