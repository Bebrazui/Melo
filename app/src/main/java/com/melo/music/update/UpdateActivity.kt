package com.melo.music.update

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.luminance
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.music.MainActivity
import com.melo.music.ui.theme.MeloTheme
import kotlinx.coroutines.delay

class UpdateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MeloTheme {
                UpdateScreen(
                    onRestartApp = {
                        val intent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    },
                    onCancel = {
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
fun UpdateScreen(
    onRestartApp: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cs = MaterialTheme.colorScheme
    val updateInfo = UpdateManager.availableUpdate

    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("Подготовка к обновлению...") }
    var isDone by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    // Анимация вращения и дыхания иконки
    val transition = rememberInfiniteTransition(label = "updatePulse")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot",
    )
    val pulseScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )

    LaunchedEffect(Unit) {
        if (updateInfo == null) {
            statusText = "Поиск свежих релизов..."
            val found = UpdateManager.checkForUpdates(context)
            if (found == null) {
                statusText = "Обновлений не найдено"
                delay(1200)
                onCancel()
                return@LaunchedEffect
            }
        }

        val target = UpdateManager.availableUpdate ?: return@LaunchedEffect
        val success = UpdateManager.downloadAndInstall(
            context = context,
            info = target,
            onProgress = { step, p ->
                statusText = step
                progress = p
            },
        )

        if (success) {
            isDone = true
            statusText = "Установщик запущен!"
        } else {
            hasError = true
            statusText = UpdateManager.downloadStatusText ?: "Не удалось загрузить обновление"
        }
    }

    val isDark = cs.background.luminance() < 0.5f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (isDark) {
                        listOf(
                            cs.background,
                            cs.surfaceContainerLow,
                            cs.surfaceContainerLowest,
                        )
                    } else {
                        listOf(
                            cs.surfaceContainerLowest,
                            cs.surfaceContainerLow,
                            cs.surfaceContainer,
                        )
                    }
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(32.dp))

            // ── Центральная анимация Material 3 (Ореол и иконка) ──────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(170.dp),
            ) {
                // Внешний пульсирующий ореол
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(cs.primary.copy(alpha = 0.08f)),
                )
                // Средний круг
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .scale(pulseScale * 0.98f)
                        .clip(CircleShape)
                        .background(cs.primary.copy(alpha = 0.16f)),
                )
                // Внутренняя выразительная плашка с иконкой
                Surface(
                    shape = CircleShape,
                    color = cs.primaryContainer,
                    shadowElevation = 12.dp,
                    modifier = Modifier.size(86.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            isDone -> {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = cs.onPrimaryContainer,
                                    modifier = Modifier.size(44.dp),
                                )
                            }
                            hasError -> {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(44.dp),
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = null,
                                    tint = cs.onPrimaryContainer,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .graphicsLayer { rotationZ = rotation },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Заголовки и статус ──────────────────────────────────────
            Text(
                text = "Обновление Melo",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                fontWeight = FontWeight.Black,
                color = Color.White,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (hasError) Color(0xFFEF4444) else Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // ── Индикатор прогресса (Material 3 Linear) ─────────────────
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = cs.primary,
                trackColor = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = cs.primary,
            )

            // ── Описание обновления (Changelog) ─────────────────────────
            updateInfo?.changelog?.takeIf { it.isNotBlank() }?.let { log ->
                Spacer(Modifier.height(24.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth(0.92f),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = cs.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Что нового:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 18.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Кнопки управления ───────────────────────────────────────
            if (hasError) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    ) {
                        Text("Закрыть", color = Color.White)
                    }
                    Button(
                        onClick = {
                            hasError = false
                            progress = 0f
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    ) {
                        Text("Повторить", color = cs.onPrimary)
                    }
                }
            } else if (!isDone) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                ) {
                    Text("Отмена", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}
