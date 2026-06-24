package com.melo.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Публичный адрес политики конфиденциальности (см. репозиторий privacy-melo). */
const val PRIVACY_URL = "https://bebrazui.github.io/privacy-melo/privacy"

/**
 * Экран приветствия/входа: email+пароль, Google, Telegram (скоро), «продолжить локально».
 * Один и тот же экран для первого запуска и для входа из вкладки «Аккаунт».
 */
@Composable
fun WelcomeScreen(
    onLogin: suspend (email: String, password: String) -> Result<Unit>,
    onStartRegister: suspend (email: String, password: String, name: String) -> Result<String>,
    onConfirmCode: suspend (userId: String, code: String) -> Result<Unit>,
    onGoogle: suspend () -> Result<Unit>,
    onLocal: () -> Unit,
    onSuccess: () -> Unit,
    onClose: (() -> Unit)? = null,
) {
    onClose?.let { BackHandler(onBack = it) }
    val scope = rememberCoroutineScope()

    var showPrivacy by remember { mutableStateOf(false) }
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeStep by remember { mutableStateOf(false) }
    var pendingUserId by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (busy) return
        error = null
        // Шаг подтверждения кода из письма.
        if (codeStep) {
            if (code.isBlank()) { error = "Введите код из письма"; return }
            busy = true
            scope.launch {
                val r = onConfirmCode(pendingUserId, code)
                busy = false
                r.onSuccess { onSuccess() }.onFailure { error = it.message ?: "Неверный код" }
            }
            return
        }
        if (email.isBlank() || password.length < 8) {
            error = "Введите email и пароль (минимум 8 символов)"
            return
        }
        if (register) {
            busy = true
            scope.launch {
                val r = onStartRegister(email, password, name)
                busy = false
                r.onSuccess { pendingUserId = it; codeStep = true }
                    .onFailure { error = it.message ?: "Не удалось зарегистрироваться" }
            }
        } else {
            val lock = com.melo.music.auth.LoginGuard.lockedRemainingMs()
            if (lock > 0) {
                error = "Слишком много попыток. Подождите ${formatRemaining(lock)}"
                return
            }
            busy = true
            scope.launch {
                val r = onLogin(email, password)
                busy = false
                r.onSuccess { com.melo.music.auth.LoginGuard.reset(); onSuccess() }
                    .onFailure {
                        com.melo.music.auth.LoginGuard.recordFailure()
                        val rem = com.melo.music.auth.LoginGuard.lockedRemainingMs()
                        error = if (rem > 0) {
                            "Слишком много попыток. Вход заблокирован на ${formatRemaining(rem)}"
                        } else {
                            it.message ?: "Не удалось войти"
                        }
                    }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070F0B)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Melo",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    codeStep -> "Подтверди почту"
                    register -> "Создай аккаунт"
                    else -> "С возвращением"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.65f),
            )
            Spacer(Modifier.height(28.dp))

            if (codeStep) {
                Text(
                    "Код отправлен на $email",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Код из письма") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                if (register) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Имя (необязательно)") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { submit() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text(
                        when {
                            codeStep -> "Подтвердить"
                            register -> "Зарегистрироваться"
                            else -> "Войти"
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            if (codeStep) {
                TextButton(onClick = { codeStep = false; code = ""; error = null }) {
                    Text("Изменить почту", color = Color.White.copy(alpha = 0.85f))
                }
            } else {
                TextButton(onClick = { register = !register; error = null }) {
                    Text(
                        if (register) "Уже есть аккаунт? Войти" else "Нет аккаунта? Зарегистрируйтесь",
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.12f)))
                Text("  или  ", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall)
                Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.12f)))
            }
            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    if (busy) return@OutlinedButton
                    busy = true; error = null
                    scope.launch {
                        val r = onGoogle()
                        busy = false
                        r.onSuccess { onSuccess() }.onFailure { error = it.message ?: "Google: не удалось" }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Войти через Google")
            }

            Spacer(Modifier.height(22.dp))
            TextButton(onClick = onLocal, enabled = !busy) {
                Text("Продолжить локально", color = Color.White.copy(alpha = 0.55f))
            }

            Spacer(Modifier.height(14.dp))
            val consent = buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.42f))) {
                    append("Продолжая, вы соглашаетесь с ")
                }
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("Политикой конфиденциальности")
                }
            }
            Text(
                consent,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clickable { showPrivacy = true },
            )
        }
    }

    if (showPrivacy) {
        PrivacyPolicyScreen(onClose = { showPrivacy = false })
    }
}

private fun formatRemaining(ms: Long): String {
    val totalMin = (ms / 60000).toInt()
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "$h ч $m мин" else "$m мин"
}
