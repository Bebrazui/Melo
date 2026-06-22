package com.melo.music.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.melo.music.auth.AuthManager
import com.melo.music.extractor.TrackItem
import com.melo.music.profile.MeloProfile
import com.melo.music.profile.ProfilesRepository
import com.melo.music.profile.PublicLibrary
import com.melo.music.profile.RemotePlaylist
import kotlinx.coroutines.launch

/** Значки: верифицирован (галочка) и разработчик (шестерёнка). */
@Composable
fun Badges(isDeveloper: Boolean, isVerified: Boolean, size: Dp = 18.dp) {
    if (isVerified) {
        Spacer(Modifier.width(5.dp))
        Icon(Icons.Rounded.Verified, contentDescription = "Верифицирован", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size))
    }
    if (isDeveloper) {
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Rounded.Build, contentDescription = "Разработчик", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(size))
    }
}

/** Кружок аватара с заглушкой. */
@Composable
fun Avatar(url: String?, size: Dp, modifier: Modifier = Modifier) {
    if (url.isNullOrBlank()) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

/** Строка пользователя в результатах поиска. */
@Composable
fun UserRow(profile: MeloProfile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(profile.avatarUrl, 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(profile.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Badges(profile.isDeveloper, profile.isVerified, size = 16.dp)
            }
            Text("Профиль Melo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

/** Шапка профиля в «Аккаунте» (свой профиль) с кнопкой редактирования. */
@Composable
fun ProfileHeaderCard() {
    var edit by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(AuthManager.avatarUrl, 64.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    AuthManager.name ?: "Пользователь Melo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Badges(AuthManager.isDeveloper, AuthManager.isVerified)
            }
            Text(
                AuthManager.bio?.ifBlank { null } ?: "Расскажи о себе",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        IconButton(onClick = { edit = true }) {
            Icon(Icons.Rounded.Edit, contentDescription = "Редактировать профиль", tint = MaterialTheme.colorScheme.primary)
        }
    }
    if (edit) EditProfileDialog(onDismiss = { edit = false })
}

@Composable
private fun EditProfileDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(AuthManager.name ?: "") }
    var bio by remember { mutableStateOf(AuthManager.bio ?: "") }
    var picked by remember { mutableStateOf<android.net.Uri?>(null) }
    var busy by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) picked = uri
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Профиль", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    if (picked != null) {
                        AsyncImage(
                            model = picked,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    } else {
                        Avatar(AuthManager.avatarUrl, 96.dp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { picker.launch("image/*") }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Выбрать фото")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 200) bio = it },
                    label = { Text("О себе") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !busy) { Text("Отмена") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            busy = true
                            scope.launch {
                                val url = if (picked != null) {
                                    ProfilesRepository.uploadAvatar(context, picked!!) ?: AuthManager.avatarUrl
                                } else {
                                    AuthManager.avatarUrl
                                }
                                AuthManager.saveProfile(name, url, bio)
                                busy = false
                                onDismiss()
                            }
                        },
                        enabled = !busy,
                    ) {
                        if (busy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Сохранить")
                        }
                    }
                }
            }
        }
    }
}

/** Экран чужого профиля (пока плейлисты — заглушка). */
@Composable
fun ProfileScreen(
    profile: MeloProfile,
    onPlay: (List<TrackItem>, Int) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    var favorites by remember(profile.userId) { mutableStateOf<List<TrackItem>?>(null) }
    var playlists by remember(profile.userId) { mutableStateOf<List<RemotePlaylist>?>(null) }
    var full by remember { mutableStateOf<Pair<String, List<TrackItem>>?>(null) }

    LaunchedEffect(profile.userId) {
        favorites = PublicLibrary.favorites(profile.userId)
        playlists = PublicLibrary.playlists(profile.userId)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 36.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                    Text("Профиль", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Avatar(profile.avatarUrl, 110.dp)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Badges(profile.isDeveloper, profile.isVerified, size = 22.dp)
                    }
                    profile.bio?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            val favs = favorites
            if (!favs.isNullOrEmpty()) {
                item {
                    LaneHeader(
                        title = "${profile.name} нравится это",
                        onFull = { full = "${profile.name} — лайки" to favs },
                    )
                }
                item { TrackLane(favs, onPlay) }
            }

            playlists?.forEach { pl ->
                item(key = "pl_" + pl.id) {
                    LaneHeader(title = pl.name, onFull = { full = pl.name to pl.tracks })
                }
                item(key = "lane_" + pl.id) { TrackLane(pl.tracks, onPlay) }
            }

            // Загрузка / пусто.
            if (favorites == null || playlists == null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 2.dp)
                    }
                }
            } else if (favs.isNullOrEmpty() && playlists!!.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Тут пока пусто", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "У пользователя нет открытых плейлистов или лайков",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    full?.let { (title, tracks) ->
        FullTrackList(title = title, tracks = tracks, onPlay = onPlay, onClose = { full = null })
    }
}

/** Заголовок полки: «… нравится это» + кнопка «Полностью». */
@Composable
private fun LaneHeader(title: String, onFull: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onFull) { Text("Полностью") }
    }
}

/** Горизонтальная лента треков (как на главной). */
@Composable
private fun TrackLane(tracks: List<TrackItem>, onPlay: (List<TrackItem>, Int) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(tracks, key = { it.url + "@" + it.speed }) { t ->
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .clickable { onPlay(tracks, tracks.indexOf(t)) },
            ) {
                AsyncImage(
                    model = t.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.height(6.dp))
                Text(t.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                t.uploader?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

/** Полный вертикальный список треков полки. */
@Composable
private fun FullTrackList(
    title: String,
    tracks: List<TrackItem>,
    onPlay: (List<TrackItem>, Int) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 36.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                items(tracks, key = { it.url + "@" + it.speed }) { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(tracks, tracks.indexOf(t)) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = t.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, fontWeight = FontWeight.Medium, maxLines = 1)
                            t.uploader?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
                        }
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
