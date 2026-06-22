package com.melo.music.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melo.music.extractor.PlaylistImporter
import com.melo.music.playlists.PlaylistManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onImported: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var service by remember { mutableStateOf<PlaylistImporter.Service?>(null) }
    var mode by remember { mutableStateOf<PlaylistImporter.Mode?>(null) }
    var url by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var success by remember { mutableStateOf<String?>(null) }

    // Системный выбор папки (Folder Picker → «использовать эту папку»).
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            busy = true; success = null; status = "Читаю папку…"; done = 0; total = 0
            scope.launch {
                runCatching {
                    val result = PlaylistImporter.importLocalFolder(context, uri)
                    val pl = PlaylistManager.create(result.name)
                    result.tracks.forEach { PlaylistManager.addTrack(pl.id, it) }
                    result
                }.onSuccess {
                    success = "Импортировано: ${it.tracks.size} треков → «${it.name}»"
                    status = null
                    onImported()
                }.onFailure {
                    status = "Ошибка: ${it.message}"
                }
                busy = false
            }
        }
    }

    val urlHint = when {
        service == PlaylistImporter.Service.YOUTUBE && mode == PlaylistImporter.Mode.FAVORITES ->
            "Ссылка на публичный плейлист (лайки YouTube приватны)"
        service == PlaylistImporter.Service.YOUTUBE ->
            "Ссылка на плейлист YouTube Music"
        service == PlaylistImporter.Service.SOUNDCLOUD && mode == PlaylistImporter.Mode.FAVORITES ->
            "Ссылка на твой профиль SoundCloud"
        else -> "Ссылка на сет SoundCloud"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Импорт плейлиста", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Откуда импортируем", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChoiceCard(
                    icon = Icons.Rounded.MusicNote,
                    label = "YouTube Music",
                    selected = service == PlaylistImporter.Service.YOUTUBE,
                    modifier = Modifier.weight(1f),
                ) { service = PlaylistImporter.Service.YOUTUBE; success = null }
                ChoiceCard(
                    icon = Icons.Rounded.Cloud,
                    label = "SoundCloud",
                    selected = service == PlaylistImporter.Service.SOUNDCLOUD,
                    modifier = Modifier.weight(1f),
                ) { service = PlaylistImporter.Service.SOUNDCLOUD; success = null }
                ChoiceCard(
                    icon = Icons.Rounded.Folder,
                    label = "На устройстве",
                    selected = service == PlaylistImporter.Service.LOCAL,
                    modifier = Modifier.weight(1f),
                ) { service = PlaylistImporter.Service.LOCAL; mode = null; success = null }
            }

            // Локальный импорт: выбор папки системным пикером.
            if (service == PlaylistImporter.Service.LOCAL) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Выбери папку с музыкой — все аудиофайлы из неё станут плейлистом " +
                        "и будут играть офлайн.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { if (!busy) folderPicker.launch(null) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (busy) "Импортирую…" else "Выбрать папку")
                }
            }

            if (service != null && service != PlaylistImporter.Service.LOCAL) {
                Spacer(Modifier.height(20.dp))
                Text("Что импортируем", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChoiceCard(
                        icon = Icons.Rounded.LibraryMusic,
                        label = "Плейлист",
                        selected = mode == PlaylistImporter.Mode.PLAYLIST,
                        modifier = Modifier.weight(1f),
                    ) { mode = PlaylistImporter.Mode.PLAYLIST }
                    ChoiceCard(
                        icon = Icons.Rounded.Favorite,
                        label = "Любимые треки",
                        selected = mode == PlaylistImporter.Mode.FAVORITES,
                        modifier = Modifier.weight(1f),
                    ) { mode = PlaylistImporter.Mode.FAVORITES }
                }
            }

            if (service != null && mode != null) {
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(urlHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val svc = service ?: return@Button
                        val md = mode ?: return@Button
                        success = null
                        busy = true
                        status = "Начинаю…"; done = 0; total = 0
                        scope.launch {
                            runCatching {
                                val result = PlaylistImporter.import(context, svc, md, url) { p ->
                                    status = p.phase; done = p.done; total = p.total
                                }
                                if (result.tracks.isEmpty()) error("Треков не найдено")
                                val pl = PlaylistManager.create(result.name)
                                result.tracks.forEach { PlaylistManager.addTrack(pl.id, it) }
                                result
                            }.onSuccess {
                                success = "Импортировано: ${it.tracks.size} треков → «${it.name}»"
                                status = null
                                onImported()
                            }.onFailure {
                                status = "Ошибка: ${it.message}"
                            }
                            busy = false
                        }
                    },
                    enabled = !busy && url.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (busy) "Импортирую…" else "Импортировать") }
            }

            if (busy) {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(status ?: "…", style = MaterialTheme.typography.bodyMedium)
                        if (total > 0) {
                            Text(
                                "$done / $total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            success?.let {
                Spacer(Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        it,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            if (!busy && success == null && status != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    status!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
            .height(96.dp)
            .border(
                width = if (selected) 1.6.dp else 1.dp,
                color = if (selected) accent else Color.Transparent,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
