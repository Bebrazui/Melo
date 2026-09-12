package com.melo.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.melo.desktop.auth.BrowserYouTubeAuthHelper
import com.melo.desktop.auth.DesktopAuthManager
import com.melo.desktop.auth.DesktopYouTubeAuthManager
import com.melo.desktop.auth.YouTubeWebLoginWindow
import com.melo.desktop.ui.theme.MeloPrimary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

@Composable
fun AuthDialog(
    onDismiss: () -> Unit,
) {
    val isYtLoggedIn = DesktopYouTubeAuthManager.isLoggedIn
    val isMeloLoggedIn = DesktopAuthManager.isLoggedIn
    val isAnyLoggedIn = isYtLoggedIn || isMeloLoggedIn

    var selectedTab by remember { mutableStateOf(0) } // 0: YouTube Music, 1: Melo / Почта

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .width(460.dp)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = { /* stop click propagation */ },
                )
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
            color = Color(0xFF161E19),
            shadowElevation = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                // Заголовок и кнопка закрытия
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (isAnyLoggedIn) "Мой профиль" else "Вход в аккаунт",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Закрыть", tint = Color.White.copy(alpha = 0.6f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isAnyLoggedIn) {
                    UserProfileView(onDismiss = onDismiss)
                } else {
                    // Вкладки выбора метода авторизации для неавторизованных
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF1E2822),
                        contentColor = MeloPrimary,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MeloPrimary,
                                height = 3.dp,
                            )
                        },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("YouTube Music", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Melo / Почта", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    when (selectedTab) {
                        0 -> YouTubeMusicAuthView(onSuccess = onDismiss)
                        1 -> EmailAuthView(onSuccess = onDismiss)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserProfileView(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val isYtLoggedIn = DesktopYouTubeAuthManager.isLoggedIn
    val isMeloLoggedIn = DesktopAuthManager.isLoggedIn
    val userName = DesktopYouTubeAuthManager.accountName ?: DesktopAuthManager.name ?: "Пользователь"
    val userHandle = DesktopYouTubeAuthManager.accountHandle ?: DesktopAuthManager.email.orEmpty()
    val userAvatar = DesktopYouTubeAuthManager.accountAvatarUrl

    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var showAddMelo by remember { mutableStateOf(false) }
    var showAddYt by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Профиль карточка с аватаркой и ником
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MeloPrimary),
                contentAlignment = Alignment.Center,
            ) {
                if (!userAvatar.isNullOrBlank()) {
                    AsyncCoverImage(
                        url = userAvatar,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                    )
                } else {
                    Text(
                        text = userName.take(1).uppercase(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A2012),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                )
                if (userHandle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = userHandle,
                        fontSize = 13.sp,
                        color = MeloPrimary,
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Блок YouTube Music
        if (isYtLoggedIn) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF0000).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color(0xFFFF4E4E),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("YouTube Music", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Сессия активна", fontSize = 11.sp, color = MeloPrimary)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            DesktopYouTubeAuthManager.logout()
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("Отключить", color = Color(0xFFEF4444), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Кнопка синхронизации медиатеки
                Button(
                    onClick = {
                        if (isSyncing) return@Button
                        isSyncing = true
                        scope.launch {
                            val res = com.melo.desktop.sync.DesktopYouTubeSyncManager.syncLibrary { msg ->
                                syncMessage = msg
                            }
                            isSyncing = false
                            syncMessage = if (res.error != null) res.error else "Синхронизировано: ${res.likedCount} лайков, ${res.playlistsCount} плейлистов"
                        }
                    },
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = MeloPrimary),
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF0A2012))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Синхронизация...", color = Color(0xFF0A2012), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Синхронизировать медиатеку", color = Color(0xFF0A2012), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (syncMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = syncMessage!!,
                        fontSize = 12.sp,
                        color = if (syncMessage!!.contains("Ошибка", ignoreCase = true)) Color(0xFFEF4444) else MeloPrimary,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        } else if (!showAddYt) {
            OutlinedButton(
                onClick = { showAddYt = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color(0xFFFF4E4E), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Подключить YouTube Music", color = Color.White, fontSize = 13.sp)
            }
        } else {
            YouTubeMusicAuthView(onSuccess = { showAddYt = false })
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Блок Melo Cloud
        if (isMeloLoggedIn) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Column {
                    Text("Melo Cloud", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(DesktopAuthManager.email.orEmpty(), fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                }
                OutlinedButton(
                    onClick = {
                        scope.launch { DesktopAuthManager.logout() }
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("Выйти", color = Color(0xFFEF4444), fontSize = 11.sp)
                }
            }
        } else if (!showAddMelo) {
            TextButton(
                onClick = { showAddMelo = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("+ Привязать аккаунт Melo (облако)", color = MeloPrimary, fontSize = 13.sp)
            }
        } else {
            EmailAuthView(onSuccess = { showAddMelo = false })
        }
    }
}

@Composable
private fun EmailAuthView(onSuccess: () -> Unit) {
    var isRegister by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (DesktopAuthManager.isLoggedIn) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MeloPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = MeloPrimary, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = DesktopAuthManager.name ?: "Пользователь Melo",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = DesktopAuthManager.email ?: "",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        DesktopAuthManager.logout()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Выйти из аккаунта", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color(0xFFEF4444),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        if (isRegister) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Ваше имя") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = MeloPrimary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MeloPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                ),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Электронная почта") },
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = MeloPrimary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MeloPrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = MeloPrimary) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MeloPrimary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            ),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Заполните все поля"
                    return@Button
                }
                isLoading = true
                errorMessage = null
                scope.launch {
                    val result = if (isRegister) {
                        DesktopAuthManager.registerWithEmail(email, password, name)
                    } else {
                        DesktopAuthManager.loginWithEmail(email, password)
                    }
                    isLoading = false
                    result.onSuccess { onSuccess() }
                        .onFailure { errorMessage = it.message ?: "Ошибка авторизации" }
                }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = MeloPrimary),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF0A2012))
            } else {
                Text(
                    text = if (isRegister) "Зарегистрироваться" else "Войти",
                    color = Color(0xFF0A2012),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = {
                isRegister = !isRegister
                errorMessage = null
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = if (isRegister) "Уже есть аккаунт? Войти" else "Нет аккаунта? Зарегистрироваться",
                color = MeloPrimary,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun GoogleAuthView(onSuccess: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Вход в Google / YouTube",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Откроется официальная форма авторизации Google. Введите логин и пароль своего аккаунта — сессия сохранится автоматически:",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            lineHeight = 18.sp,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                YouTubeWebLoginWindow.open(
                    onSuccess = { onSuccess() }
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = MeloPrimary),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Войти через Google", color = Color(0xFF0A2012), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun YouTubeMusicAuthView(onSuccess: () -> Unit) {
    var cookieText by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Войдите в свой YouTube Music аккаунт, чтобы синхронизировать плейлисты, медиатеку и рекомендации:",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.75f),
            lineHeight = 18.sp,
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Основная кнопка: Запуск окна веб-авторизации
        Button(
            onClick = {
                YouTubeWebLoginWindow.open(
                    onSuccess = { onSuccess() }
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Войти в YouTube Music",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Вторая кнопка: Вход через внешний браузер
        OutlinedButton(
            onClick = {
                BrowserYouTubeAuthHelper.openBrowserLogin(
                    onSuccess = { onSuccess() }
                )
            },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                "Войти через браузер по умолчанию",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = { showManualInput = !showManualInput },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                if (showManualInput) "Скрыть ручной ввод" else "Или ввести Cookie вручную...",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
            )
        }

        if (showManualInput) {
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            
            Text(
                text = "Если страница входа не загружается из-за блокировок, вы можете вставить Cookie из браузера (содержащие SAPISID / __Secure-3PAPISID и SID / LOGIN_INFO):",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.65f),
                lineHeight = 16.sp,
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = cookieText,
                onValueChange = { cookieText = it },
                label = { Text("SAPISID=...; SID=...") },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(
                onClick = {
                    clipboardManager.getText()?.text?.let { clipText ->
                        cookieText = clipText
                    }
                },
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text("Вставить из буфера обмена", color = MeloPrimary, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (cookieText.isNotBlank()) {
                        DesktopYouTubeAuthManager.saveSession(
                            cookies = cookieText,
                            name = accountName.trim().ifBlank { "YouTube Music" },
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            DesktopYouTubeAuthManager.fetchUserProfile()
                        }
                        onSuccess()
                    }
                },
                enabled = cookieText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MeloPrimary),
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Сохранить куки", color = Color(0xFF0A2012), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
