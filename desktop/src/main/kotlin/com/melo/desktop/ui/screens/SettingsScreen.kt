package com.melo.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.desktop.audio.cache.AudioCacheManager
import com.melo.desktop.byedpi.ByeDpiManager
import com.melo.desktop.storage.DesktopStorage
import com.melo.desktop.storage.DpiEngine
import com.melo.desktop.ui.theme.MeloPrimary
import com.melo.desktop.ui.theme.MeloSurfaceVariant
import com.melo.desktop.zapret.ZapretManager
import com.melo.desktop.zapret.ZapretPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    var dpiEngine by DesktopStorage.dpiEngine
    var zapretPreset by DesktopStorage.zapretPreset
    var zapretCustomArgs by DesktopStorage.zapretCustomArgs
    var zapretCustomPath by DesktopStorage.zapretCustomPath

    var byedpiEnabled by DesktopStorage.byedpiEnabled
    var byedpiCmd by DesktopStorage.byedpiCmd

    var isZapretRunning by remember { mutableStateOf(ZapretManager.isRunning()) }
    var isByeDpiRunning by remember { mutableStateOf(ByeDpiManager.isRunning()) }

    // Периодический опрос статусов процессов
    LaunchedEffect(Unit) {
        while (true) {
            isZapretRunning = ZapretManager.isRunning(forceRefresh = true)
            isByeDpiRunning = ByeDpiManager.isRunning()
            delay(2000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
    ) {
        Text(
            text = "Настройки",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Секция: Выбор режима DPI ─────────────────────────────────────────
        Text(
            text = "СЕТЬ И ОБХОД БЛОКИРОВОК (DPI)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MeloPrimary,
            letterSpacing = 1.sp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(20.dp),
        ) {
            Text(
                text = "Режим работы обхода DPI",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Melo поддерживает как системный перехват Zapret (flowseal), так и локальный прокси ByeDPI.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            DpiEngine.values().forEach { engine ->
                val isSelected = dpiEngine == engine
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MeloPrimary.copy(alpha = 0.12f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) MeloPrimary.copy(alpha = 0.4f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .clickable {
                            dpiEngine = engine
                            DesktopStorage.saveSettings()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            dpiEngine = engine
                            DesktopStorage.saveSettings()
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MeloPrimary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = engine.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = engine.subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Секция: Zapret (Flowseal) ─────────────────────────────────────────
        if (dpiEngine == DpiEngine.AUTO || dpiEngine == DpiEngine.ZAPRET) {
            Text(
                text = "ZAPRET (FLOWSEAL / WINWS)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MeloPrimary,
                letterSpacing = 1.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(20.dp),
            ) {
                // Статус winws.exe
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isZapretRunning) Color(0xFF10B981) else Color(0xFF6B7280)),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isZapretRunning) "winws.exe активен (WinDivert фильтрация пакетов)" else "winws.exe не запущен",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Пресеты оптимизированы проектом flowseal/zapret-discord-youtube для обхода замедления YouTube, звонков Discord и стриминга аудио на уровне ядра Windows без прокси.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Выбор стратегии Flowseal
                Text(
                    text = "Выберите стратегию Zapret:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))

                ZapretPreset.values().forEach { preset ->
                    val isPresetSelected = zapretPreset == preset
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPresetSelected) MeloPrimary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable {
                                zapretPreset = preset
                                DesktopStorage.saveSettings()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        RadioButton(
                            selected = isPresetSelected,
                            onClick = {
                                zapretPreset = preset
                                DesktopStorage.saveSettings()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MeloPrimary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = preset.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = preset.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (zapretPreset == ZapretPreset.CUSTOM) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Пользовательские аргументы winws.exe:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = zapretCustomArgs,
                        onValueChange = {
                            zapretCustomArgs = it
                            DesktopStorage.saveSettings()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MeloPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Путь к бинарникам winws.exe
                val locatedBinary = ZapretManager.findWinwsBinary(zapretCustomPath)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Расположение winws.exe: ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = locatedBinary?.absolutePath ?: "Не найден",
                        fontSize = 12.sp,
                        color = if (locatedBinary != null) Color(0xFF10B981) else Color(0xFFEF4444),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = zapretCustomPath,
                    onValueChange = {
                        zapretCustomPath = it
                        DesktopStorage.saveSettings()
                    },
                    placeholder = { Text("Пользовательский путь к winws.exe или папке zapret", fontSize = 12.sp) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MeloPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Кнопки управления Zapret
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                ZapretManager.start(
                                    preset = zapretPreset,
                                    customArgs = zapretCustomArgs,
                                    customPath = zapretCustomPath,
                                )
                                isZapretRunning = ZapretManager.isRunning(forceRefresh = true)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MeloPrimary),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Запустить Zapret (UAC)")
                    }

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                ZapretManager.stop()
                                isZapretRunning = ZapretManager.isRunning(forceRefresh = true)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Остановить Zapret")
                    }

                    OutlinedButton(
                        onClick = {
                            val zapretDir = File(System.getProperty("user.home"), ".melo/bin/zapret")
                            zapretDir.mkdirs()
                            try {
                                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(zapretDir)
                                else ProcessBuilder("explorer.exe", zapretDir.absolutePath).start()
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Открыть папку bin/zapret")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── Секция: Обход DPI (ByeDPI) ────────────────────────────────────────
        if (dpiEngine == DpiEngine.AUTO || dpiEngine == DpiEngine.BYEDPI) {
            Text(
                text = "BYEDPI (CIADPI / SOCKS5)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MeloPrimary,
                letterSpacing = 1.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(20.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Локальный ByeDPI прокси",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Десинхронизация TCP/TLS на 127.0.0.1:1080 без прав администратора.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Switch(
                        checked = byedpiEnabled,
                        onCheckedChange = {
                            byedpiEnabled = it
                            ByeDpiManager.isEnabled = it
                            if (it) ByeDpiManager.start(byedpiCmd) else ByeDpiManager.stop()
                            DesktopStorage.saveSettings()
                            isByeDpiRunning = ByeDpiManager.isRunning()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MeloPrimary,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Статус подключения
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isByeDpiRunning) Color(0xFF10B981) else Color(0xFF6B7280)),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isByeDpiRunning) "SOCKS5 порт 1080 активен и отвечает" else "SOCKS5 порт 1080 не активен",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Параметры десинхронизации (CLI аргументы)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = byedpiCmd,
                    onValueChange = {
                        byedpiCmd = it
                        DesktopStorage.saveSettings()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MeloPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // ── Секция: Кэш и данные ──────────────────────────────────────────────
        Text(
            text = "ХРАНИЛИЩЕ И КЭШ",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MeloPrimary,
            letterSpacing = 1.sp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(20.dp),
        ) {
            val meloDir = File(System.getProperty("user.home"), ".melo")
            var cacheSizeBytes by remember { mutableStateOf(AudioCacheManager.getCacheSizeBytes()) }

            Text(
                text = "Директория профиля и кэш",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = meloDir.absolutePath,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Размер кэша аудио: ${cacheSizeBytes / (1024 * 1024)} МБ (до 2 ГБ)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MeloPrimary,
            )
            Text(
                text = "Прослушанные треки сохраняются на диск. Следующий трек в очереди предзагружается в фоне для мгновенного переключения (0 мс).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        AudioCacheManager.clearCache()
                        val cache = File(meloDir, "cache")
                        if (cache.exists()) cache.deleteRecursively()
                        cacheSizeBytes = 0L
                    },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Очистить кэш треков и обложек")
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val audioDir = File(meloDir, "cache/audio")
                            audioDir.mkdirs()
                            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(audioDir)
                            else ProcessBuilder("explorer.exe", audioDir.absolutePath).start()
                        } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Открыть папку кэша аудио")
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Секция: О программе ───────────────────────────────────────────────
        Text(
            text = "О ПРИЛОЖЕНИИ",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MeloPrimary,
            letterSpacing = 1.sp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(20.dp),
        ) {
            Text(
                text = "Melo Desktop 1.0.0",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Музыкальный стриминговый плеер с поддержкой YouTube Music, SoundCloud, встроенным обходом DPI (Zapret / ByeDPI) без VPN, синхронизированными текстами и умной персональной волной.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
