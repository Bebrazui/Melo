package com.melo.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.music.update.UpdateInfo
import com.melo.music.update.UpdateManager
import kotlinx.coroutines.launch

/**
 * Информативная и аккуратная плашка обновления Melo:
 * - Показывает доступную версию и ченджлог.
 * - По кнопке «Обновить» самостоятельно скачивает APK с прогресс-баром и запускает установщик.
 * - Имеет опцию «Не показывать больше» для пропуска текущей версии.
 */
@Composable
fun UpdatePillBanner(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val isDownloading = UpdateManager.isDownloading
    val progress = UpdateManager.downloadProgress
    val statusText = UpdateManager.downloadStatusText

    var dontShowAgain by remember { mutableStateOf(false) }

    val bannerBg = if (isDark) cs.surfaceContainerHigh.copy(alpha = 0.96f) else cs.surfaceContainerLowest
    val borderColor = cs.primary.copy(alpha = if (isDark) 0.35f else 0.2f)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = bannerBg,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .animateContentSize(),
    ) {
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
        ) {
            // Верхняя строка: иконка, инфо о версии, крестик
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDownloading) Icons.Rounded.Download else Icons.Rounded.RocketLaunch,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Доступно обновление Melo ${updateInfo.versionName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isDownloading && !statusText.isNullOrBlank()) {
                            statusText
                        } else {
                            updateInfo.changelog
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = if (isDownloading) cs.primary else cs.onSurfaceVariant,
                        maxLines = if (isDownloading) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (!isDownloading) {
                    IconButton(
                        onClick = {
                            if (dontShowAgain) {
                                UpdateManager.ignoreVersion(context, updateInfo.versionCode, updateInfo.versionName)
                            }
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Скрыть",
                            tint = cs.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Прогресс скачивания
            if (isDownloading) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = cs.primary,
                    trackColor = cs.primary.copy(alpha = 0.18f),
                )
            } else {
                Spacer(Modifier.height(8.dp))
                // Нижняя строка: чекбокс "Не показывать больше" + кнопки
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Чекбокс "Не показывать больше"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { dontShowAgain = !dontShowAgain }
                            .padding(vertical = 4.dp),
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = cs.primary,
                                uncheckedColor = cs.onSurfaceVariant.copy(alpha = 0.6f),
                            ),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Не показывать больше",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = cs.onSurfaceVariant,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        TextButton(
                            onClick = {
                                if (dontShowAgain) {
                                    UpdateManager.ignoreVersion(context, updateInfo.versionCode, updateInfo.versionName)
                                }
                                onDismiss()
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "Позже",
                                style = MaterialTheme.typography.labelMedium,
                                color = cs.onSurfaceVariant,
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    UpdateManager.downloadAndInstall(context, updateInfo)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cs.primary,
                                contentColor = cs.onPrimary,
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp),
                        ) {
                            Text(
                                text = "Обновить",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
