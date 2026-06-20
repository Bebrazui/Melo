package com.melo.music.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melo.music.audio.EqualizerManager
import com.melo.music.byedpi.ByeDpiProxy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    scGetId: () -> String?,
    onScSetManual: suspend (String) -> Boolean,
    onScRefresh: suspend () -> String?,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    // SoundCloud state
    var currentId by remember { mutableStateOf(scGetId()) }
    var manual by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // ByeDPI state
    var byeDpiExpanded by remember { mutableStateOf(false) }
    var byeDpiCmd by remember { mutableStateOf(ByeDpiProxy.getCommandLine()) }
    var byeDpiRunning by remember { mutableStateOf(ByeDpiProxy.isRunning()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
            // ── Для разработчиков (ByeDPI) ──────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { byeDpiExpanded = !byeDpiExpanded }
                    .padding(vertical = 12.dp),
            ) {
                Icon(
                    imageVector = if (byeDpiExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Для разработчиков",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (byeDpiRunning) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    ) {
                        Text(
                            "ON",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = byeDpiExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    Text(
                        "ByeDPI — обход DPI. Вставь командную строку (как в CLI).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = byeDpiCmd,
                        onValueChange = {
                            byeDpiCmd = it
                            ByeDpiProxy.setCommandLine(it)
                        },
                        label = { Text("Командная строка ByeDPI") },
                        placeholder = { Text("--disorder 1 --auto=torst --timeout 3") },
                        singleLine = false,
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    byeDpiRunning = ByeDpiProxy.start(byeDpiCmd)
                                    message = if (byeDpiRunning) "ByeDPI запущен ✓" else "Ошибка запуска"
                                }
                            },
                            enabled = !byeDpiRunning && byeDpiCmd.isNotBlank(),
                        ) { Text("Старт") }
                        OutlinedButton(
                            onClick = {
                                ByeDpiProxy.stop()
                                byeDpiRunning = false
                                message = "ByeDPI остановлен"
                            },
                            enabled = byeDpiRunning,
                        ) { Text("Стоп") }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Примеры:\n" +
                            "  --disorder 1\n" +
                            "  --fake -1 --ttl 8\n" +
                            "  --split 1+s --disorder 3+s\n" +
                            "  --auto=torst --timeout 3 --tlsrec 1+s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ── SoundCloud ───────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SoundCloud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = currentId?.let { "client_id: ✓ сохранён (${it.take(6)}…)" }
                    ?: "client_id: ✗ нет",
                style = MaterialTheme.typography.bodyMedium,
                color = if (currentId != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
            )

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    if (busy) return@OutlinedButton
                    scope.launch {
                        busy = true; message = "Пробую добыть…"
                        val r = onScRefresh(); currentId = r
                        message = if (r != null) "Готово ✓" else "Не вышло — вставь вручную"
                        busy = false
                    }
                },
                enabled = !busy,
            ) { Text("Обновить автоматически") }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = manual,
                onValueChange = { manual = it },
                label = { Text("client_id вручную") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val id = manual.trim()
                    if (busy || id.isEmpty()) return@Button
                    scope.launch {
                        busy = true; message = "Проверяю…"
                        val ok = onScSetManual(id)
                        if (ok) { currentId = id; manual = ""; message = "Сохранён ✓" }
                        else message = "Неверный client_id"
                        busy = false
                    }
                },
                enabled = !busy,
            ) { Text("Сохранить") }

            if (busy) {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── Эквалайзер ───────────────────────────────────────────
            EqualizerSection()
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

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
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
