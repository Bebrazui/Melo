package com.melo.desktop.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melo.desktop.extractor.DesktopExtractor
import com.melo.desktop.extractor.ItemKind
import com.melo.desktop.extractor.TrackItem
import com.melo.desktop.ui.components.MeloTrackCard
import com.melo.desktop.ui.theme.MeloSurfaceVariant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    onPlayTrack: (TrackItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var results by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("Все") }

    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun doSearch(text: String) {
        if (text.isBlank()) return
        query = text
        suggestions = emptyList()
        isSearching = true
        searchJob?.cancel()
        searchJob = scope.launch {
            DesktopExtractor.search(text).collectLatest { items ->
                results = items
                isSearching = false
                com.melo.desktop.extractor.DesktopPrefetcher.prefetchAll(items.take(3).map { it.url })
            }
        }
    }

    LaunchedEffect(query) {
        if (query.length >= 2 && results.isEmpty()) {
            delay(250)
            suggestions = DesktopExtractor.getSuggestions(query).take(6)
        } else {
            suggestions = emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 20.dp),
    ) {
        // ── Поле поиска (M3 Pill shape) ───────────────────────────────────────
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                if (it.isBlank()) results = emptyList()
            },
            placeholder = {
                Text(
                    "Поиск трека или исполнителя",
                    color = Color.White.copy(alpha = 0.55f),
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.size(22.dp),
                )
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = {
                        query = ""
                        results = emptyList()
                        suggestions = emptyList()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Очистить",
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { doSearch(query) }),
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MeloSurfaceVariant,
                unfocusedContainerColor = MeloSurfaceVariant,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        )

        // ── Выпадающие подсказки ──────────────────────────────────────────────
        if (suggestions.isNotEmpty() && results.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MeloSurfaceVariant)
                    .padding(vertical = 6.dp),
            ) {
                suggestions.forEach { sug ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { doSearch(sug) }
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = sug,
                            fontSize = 15.sp,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Чипы фильтрации ───────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Все", "Треки", "Исполнители", "Альбомы").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 13.sp) },
                    shape = RoundedCornerShape(18.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MeloSurfaceVariant,
                        labelColor = Color.White,
                    ),
                    border = null,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Результаты поиска (MeloTrackCard) ─────────────────────────────────
        if (isSearching && results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (results.isEmpty() && query.isNotBlank() && !isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "По запросу ничего не найдено",
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
        } else {
            val filtered = when (selectedFilter) {
                "Треки" -> results.filter { it.kind == ItemKind.TRACK }
                "Исполнители" -> results.filter { it.kind == ItemKind.ARTIST }
                "Альбомы" -> results.filter { it.kind == ItemKind.ALBUM }
                else -> results
            }

            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(filtered) { track ->
                    MeloTrackCard(
                        item = track,
                        onClick = { onPlayTrack(track) },
                    )
                }
            }
        }
    }
}
