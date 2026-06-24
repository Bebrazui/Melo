package com.melo.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Контактная почта для вопросов конфиденциальности. */
private const val CONTACT_EMAIL = "ttt793647@gmail.com"

private val BG = Color(0xFF070F0B)
private val TEXT = Color(0xFFE9F2EC)
private val MUTED = Color(0xFF9FB3A8)

/**
 * Политика конфиденциальности прямо в приложении (офлайн, без обращения в сеть).
 * Текст синхронизирован с сайтом privacy-melo.
 */
@Composable
fun PrivacyPolicyScreen(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Surface(modifier = Modifier.fillMaxSize(), color = BG) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 36.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад", tint = TEXT)
                }
                Text("Политика конфиденциальности", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TEXT, maxLines = 1)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
            ) {
                Text("Приложение «Melo» · Обновлено 22 июня 2026 г.", style = MaterialTheme.typography.bodySmall, color = MUTED)
                Spacer(Modifier.height(16.dp))

                Para(
                    "Настоящая Политика описывает, какие данные обрабатывает приложение Melo, " +
                        "для чего и как вы можете ими управлять. Используя Приложение, вы соглашаетесь с этой Политикой.",
                )

                Card(
                    "Коротко: приложением можно пользоваться полностью без регистрации и без передачи " +
                        "каких-либо данных — в локальном режиме. Данные обрабатываются, только если вы сами " +
                        "включаете соответствующие функции (вход в аккаунт, синхронизация, «Карта музыки»). " +
                        "Рекламы, продажи данных и сторонней слежки нет.",
                )

                Heading("1. Кто обрабатывает данные")
                Para("Оператор — разработчик Приложения. По вопросам конфиденциальности и удаления данных: $CONTACT_EMAIL.")

                Heading("2. Какие данные и зачем")
                Bullet("Вход в аккаунт (по желанию): обрабатываются ваш email и имя — только для авторизации и привязки библиотеки. Пароли в открытом виде не хранятся.")
                Bullet("Синхронизация библиотеки (по желанию): если вы вошли, ваши плейлисты и избранное (названия треков и ссылки) сохраняются на сервере для доступа с других устройств. Локальные файлы с устройства не передаются.")
                Bullet("«Карта музыки» (по желанию): когда вы оставляете трек на карте, сохраняются выбранные координаты и данные трека; пин виден другим. Геолокация запрашивается только в этот момент и только с вашего разрешения.")
                Bullet("Локальный режим: плейлисты, избранное, история, скачанные и импортированные треки хранятся только на вашем устройстве.")
                Para("Приложение не использует рекламу, не ведёт слежку и не продаёт ваши данные.")

                Heading("3. Разрешения устройства")
                Bullet("Интернет — воспроизведение и поиск музыки из открытых публичных источников.")
                Bullet("Местоположение — только для «Карты музыки», когда вы сами оставляете трек.")
                Bullet("Уведомления — управление плеером (название трека, пауза/перемотка) во время воспроизведения.")
                Bullet("Доступ к файлам/папке — только для импорта вашей музыки и сохранения скачанного, по вашему выбору.")

                Heading("4. Передача данных третьим лицам")
                Para("Для работы перечисленных функций используются сторонние сервисы исключительно в технических целях:")
                Bullet("Appwrite — облачный бэкенд (аккаунт, синхронизация библиотеки, пины карты).")
                Bullet("Google Sign-In — если вы выбираете вход через Google.")
                Para("Музыка воспроизводится из открытых публичных источников через соединение вашего устройства; Приложение не размещает и не хранит аудиофайлы на своих серверах.")

                Heading("5. Хранение и удаление")
                Para("Данные аккаунта и синхронизированной библиотеки хранятся, пока существует ваш аккаунт. Вы можете в любой момент:")
                Bullet("выйти из аккаунта и продолжить пользоваться локально;")
                Bullet("очистить локальные данные через настройки устройства (удаление данных приложения);")
                Bullet("запросить удаление аккаунта и связанных данных, написав на $CONTACT_EMAIL.")

                Heading("6. Дети")
                Para("Приложение не предназначено для детей младше возраста, установленного применимым законодательством, и целенаправленно не собирает их данные.")

                Heading("7. Изменения политики")
                Para("Мы можем обновлять эту Политику. Актуальная версия всегда доступна в приложении; дата обновления указана выше.")

                Heading("8. Контакты")
                Para("По любым вопросам, связанным с конфиденциальностью: $CONTACT_EMAIL.")

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Spacer(Modifier.height(22.dp))
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TEXT)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Para(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCDDED4), lineHeight = MaterialTheme.typography.bodyMedium.lineHeight)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text("•  ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFCDDED4))
    }
}

@Composable
private fun Card(text: String) {
    Spacer(Modifier.height(12.dp))
    Surface(
        color = Color(0x14FFFFFF),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = TEXT,
            modifier = Modifier.padding(16.dp),
        )
    }
    Spacer(Modifier.height(12.dp))
}
