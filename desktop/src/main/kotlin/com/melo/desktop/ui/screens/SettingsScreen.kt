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
import com.melo.desktop.ui.components.AsyncCoverImage
import java.awt.Desktop
import java.io.File

@Composable
fun SettingsScreen(
    onOpenAuth: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

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

        Spacer(modifier = Modifier.height(20.dp))

        // ── Секция: Аккаунт и профиль ─────────────────────────────────────────
        var isAuthDialogVisible by remember { mutableStateOf(false) }
        val isYtLoggedIn = com.melo.desktop.auth.DesktopYouTubeAuthManager.isLoggedIn
        val userName = com.melo.desktop.auth.DesktopYouTubeAuthManager.accountName 
            ?: com.melo.desktop.auth.DesktopAuthManager.name 
            ?: "Гость"
        val userAvatar = com.melo.desktop.auth.DesktopYouTubeAuthManager.accountAvatarUrl
        val userHandle = com.melo.desktop.auth.DesktopYouTubeAuthManager.accountHandle
        val scope = rememberCoroutineScope()
        var isSyncing by remember { mutableStateOf(false) }
        var syncStatusMessage by remember { mutableStateOf<String?>(null) }

        Text(
            text = "GOOGLE / YOUTUBE MUSIC",
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isYtLoggedIn) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MeloPrimary),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!userAvatar.isNullOrBlank()) {
                                AsyncCoverImage(
                                    url = userAvatar,
                                    modifier = Modifier.size(44.dp),
                                    shape = CircleShape,
                                )
                            } else {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0A2012),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                    }

                    Column {
                        Text(
                            text = if (isYtLoggedIn) userName else "Google / YouTube Music",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isYtLoggedIn) 
                                (syncStatusMessage ?: (userHandle?.let { "$it • Доступ к трекам 18+ и медиатеке" } ?: "Подключено. Доступ к трекам 18+ и медиатеке"))
                                else "Войдите для синхронизации плейлистов и снятия ограничений",
                            fontSize = 12.sp,
                            color = if (isYtLoggedIn) MeloPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (isYtLoggedIn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (isSyncing) return@Button
                                isSyncing = true
                                scope.launch {
                                    val res = com.melo.desktop.sync.DesktopYouTubeSyncManager.syncLibrary { msg ->
                                        syncStatusMessage = msg
                                    }
                                    isSyncing = false
                                    syncStatusMessage = if (res.error != null) res.error else "Синхронизировано: ${res.likedCount} лайков, ${res.playlistsCount} плейлистов"
                                }
                            },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MeloPrimary.copy(alpha = 0.25f)),
                        ) {
                            Text(
                                text = if (isSyncing) "Синхронизация..." else "Синхронизировать",
                                color = MeloPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                com.melo.desktop.auth.DesktopYouTubeAuthManager.logout()
                                syncStatusMessage = null
                            },
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Выйти", color = Color(0xFFEF4444), fontSize = 13.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = { onOpenAuth?.invoke() ?: run { isAuthDialogVisible = true } },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MeloPrimary),
                    ) {
                        Text(
                            text = "Войти в аккаунт",
                            color = Color(0xFF0A2012),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        if (isAuthDialogVisible && onOpenAuth == null) {
            com.melo.desktop.ui.components.AuthDialog(
                onDismiss = { isAuthDialogVisible = false },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Секция: Интеграции ────────────────────────────────────────────────
        Text(
            text = "ИНТЕГРАЦИИ",
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
                        text = "Discord Rich Presence",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Отображать играющий трек, исполнителя и статус воспроизведения в вашем профиле Discord",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                var discordRpcEnabled by DesktopStorage.discordRpcEnabled
                Switch(
                    checked = discordRpcEnabled,
                    onCheckedChange = {
                        discordRpcEnabled = it
                        DesktopStorage.saveSettings()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MeloPrimary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

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
