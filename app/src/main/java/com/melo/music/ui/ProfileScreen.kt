package com.melo.music.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.melo.music.ui.theme.bouncyOverscroll
import com.melo.music.ui.theme.bouncyHorizontalOverscroll
import com.melo.music.ui.theme.carouselCenterItemEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Verified
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.melo.music.auth.AuthManager
import com.melo.music.extractor.TrackItem
import com.melo.music.profile.MeloProfile
import com.melo.music.profile.ProfilesRepository
import com.melo.music.profile.PublicLibrary
import com.melo.music.profile.RemotePlaylist
import kotlinx.coroutines.launch

/** Значки: верифицирован (галочка) и разработчик (шестерёнка) в стиле Material 3 Expressive. */
@Composable
fun Badges(isDeveloper: Boolean, isVerified: Boolean, size: Dp = 18.dp) {
    if (isVerified) {
        Spacer(Modifier.width(6.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
            modifier = Modifier.size(size + 4.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Verified,
                    contentDescription = "Верифицирован",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(size),
                )
            }
        }
    }
    if (isDeveloper) {
        Spacer(Modifier.width(4.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(size + 4.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Build,
                    contentDescription = "Разработчик",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size - 2.dp),
                )
            }
        }
    }
}

/** Кружок аватара с заглушкой и тактильным оформлением. */
@Composable
fun Avatar(url: String?, size: Dp, modifier: Modifier = Modifier) {
    if (url.isNullOrBlank()) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), CircleShape),
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
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f)), CircleShape),
        )
    }
}

/** Строка пользователя в результатах поиска (Material 3 Expressive). */
@Composable
fun UserRow(profile: MeloProfile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(profile.avatarUrl, 50.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                    )
                    Badges(profile.isDeveloper, profile.isVerified, size = 16.dp)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Профиль Melo",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
            Icon(
                Icons.Rounded.Person,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Шапка профиля в «Аккаунте» (свой профиль) с кнопкой редактирования (Material 3 Expressive). */
@Composable
fun ProfileHeaderCard() {
    var edit by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(AuthManager.avatarUrl, 68.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        AuthManager.name ?: "Пользователь Melo",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            letterSpacing = (-0.3).sp,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                    )
                    Badges(AuthManager.isDeveloper, AuthManager.isVerified)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    AuthManager.bio?.ifBlank { null } ?: "Расскажи о себе миру",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = cs.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable { edit = true },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Редактировать профиль",
                        tint = cs.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
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
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            tonalElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Редактировать профиль",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.height(18.dp))
                Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    if (picked != null) {
                        AsyncImage(
                            model = picked,
                            contentDescription = null,
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), CircleShape),
                        )
                    } else {
                        Avatar(AuthManager.avatarUrl, 104.dp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = { picker.launch("image/*") },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Выбрать фото", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 200) bio = it },
                    label = { Text("О себе") },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(22.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !busy) { Text("Отмена") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        shape = RoundedCornerShape(20.dp),
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                        } else {
                            Text("Сохранить", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** Экран чужого профиля в стиле Material 3 Expressive. */
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().bouncyOverscroll(),
            contentPadding = PaddingValues(bottom = 36.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 16.dp, top = 40.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClose),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Назад",
                                tint = Color.White,
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "Профиль",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Аватар с двойным кольцом
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)), CircleShape)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Avatar(profile.avatarUrl, 108.dp)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Имя в крупной выразительной типографике
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            profile.name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                letterSpacing = (-0.5).sp,
                            ),
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Badges(profile.isDeveloper, profile.isVerified, size = 22.dp)
                    }

                    profile.bio?.ifBlank { null }?.let {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.padding(horizontal = 12.dp),
                        ) {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }

            val favs = favorites
            if (!favs.isNullOrEmpty()) {
                item {
                    LaneHeader(
                        title = "${profile.name} нравится",
                        onFull = { full = "${profile.name} — лайки" to favs },
                    )
                }
                item { TrackLane(favs, onPlay) }
                item { Spacer(Modifier.height(16.dp)) }
            }

            playlists?.forEach { pl ->
                item(key = "pl_" + pl.id) {
                    LaneHeader(title = pl.name, onFull = { full = pl.name to pl.tracks })
                }
                item(key = "lane_" + pl.id) { TrackLane(pl.tracks, onPlay) }
                item { Spacer(Modifier.height(16.dp)) }
            }

            // Загрузка / пусто.
            if (favorites == null || playlists == null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                    }
                }
            } else if (favs.isNullOrEmpty() && playlists!!.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(28.dp))
                            .padding(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.LibraryMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Тут пока пусто",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "У пользователя нет открытых плейлистов или лайков",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
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

/** Заголовок полки: «… нравится» + тактильный чип «Полностью». */
@Composable
private fun LaneHeader(title: String, onFull: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 20.sp,
                letterSpacing = (-0.3).sp,
            ),
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onFull),
        ) {
            Text(
                "Все",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

/** Горизонтальная лента треков в стиле Material 3 Expressive. */
@Composable
private fun TrackLane(tracks: List<TrackItem>, onPlay: (List<TrackItem>, Int) -> Unit) {
    val laneState = rememberLazyListState()
    LazyRow(
        state = laneState,
        modifier = Modifier.bouncyHorizontalOverscroll(),
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        itemsIndexed(tracks, key = { _, it -> it.url + "@" + it.speed }) { index, t ->
            Column(
                modifier = Modifier
                    .carouselCenterItemEffect(laneState, index)
                    .width(148.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onPlay(tracks, index) },
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.size(148.dp),
                ) {
                    Artwork(
                        url = t.thumbnailUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    t.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                t.uploader?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** Полный вертикальный список треков полки (Material 3 Expressive). */
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp, top = 40.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(tracks, key = { it.url + "@" + it.speed }) { t ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onPlay(tracks, tracks.indexOf(t)) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Artwork(
                                url = t.thumbnailUrl,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    t.title,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                t.uploader?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.65f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
