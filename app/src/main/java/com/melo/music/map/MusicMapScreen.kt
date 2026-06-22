package com.melo.music.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.Color as AColor
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.melo.music.extractor.TrackItem
import com.melo.music.favorites.FavoritesManager
import com.melo.music.playlists.Playlist
import com.melo.music.playlists.PlaylistManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker
import kotlin.math.ln
import kotlin.math.max

private val MAP_BG = Color(0xFF0B1610)
private val GLASS = Color(0xF2141F19)
private val SOLID = Color(0xFF18241E)

/** Состояние карты, доступное из не-Compose колбэков (слушатель карты). */
private class MapHolder {
    var drops: List<MapDrop> = emptyList()
    val artCache = HashMap<String, Bitmap>()
    var render: () -> Unit = {}
}

/**
 * Карта музыки: пины с треками на реальной OSM-карте. Оставить можно только
 * по своей геолокации (трек выбираешь из избранного/плейлистов/поиска);
 * слушать — что угодно по всему миру. Близкие пины при отдалении сливаются
 * в кластер с числом.
 */
@Composable
fun MusicMapScreen(
    onSearch: (String) -> Flow<List<TrackItem>>,
    onPlay: (TrackItem) -> Unit,
    onClose: () -> Unit,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
    nowPlayingBar: @Composable () -> Unit = {},
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = MaterialTheme.colorScheme.primary.toArgb()
    val res = context.resources

    var selected by remember { mutableStateOf<MapDrop?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var captionFor by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }
    var pendingTrack by remember { mutableStateOf<TrackItem?>(null) }
    var mapQuery by remember { mutableStateOf("") }
    var recentCache by remember { mutableStateOf<List<MapDrop>>(emptyList()) }
    var searchHits by remember { mutableStateOf<List<MapDrop>>(emptyList()) }
    val ms = remember { MapHolder() }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(4.5)
            controller.setCenter(GeoPoint(48.0, 15.0))
        }
    }
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    // Рендер маркеров с кластеризацией. Читает ms.drops (актуальные данные).
    ms.render = render@{
        val proj: Projection = mapView.projection ?: return@render
        val clusters = clusterDrops(ms.drops, proj, context.px(64f))
        mapView.overlays.clear()
        for (cl in clusters) {
            if (cl.items.size == 1) {
                val d = cl.items[0]
                val m = Marker(mapView)
                m.position = GeoPoint(d.lat, d.lng)
                m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                m.infoWindow = null
                m.icon = BitmapDrawable(res, makePinBitmap(context, ms.artCache[d.thumbnailUrl], accent))
                m.setOnMarkerClickListener { _, _ -> selected = d; true }
                mapView.overlays.add(m)
                val url = d.thumbnailUrl
                if (!url.isNullOrBlank() && ms.artCache[url] == null) {
                    scope.launch {
                        val bmp = loadArtBitmap(context, url)
                        if (bmp != null) {
                            ms.artCache[url] = bmp
                            m.icon = BitmapDrawable(res, makePinBitmap(context, bmp, accent))
                            mapView.invalidate()
                        }
                    }
                }
            } else {
                val center = cl.center()
                val m = Marker(mapView)
                m.position = center
                m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                m.infoWindow = null
                m.icon = BitmapDrawable(res, makeClusterBitmap(context, cl.items.size, accent))
                m.setOnMarkerClickListener { _, _ ->
                    mapView.controller.animateTo(center)
                    mapView.controller.setZoom(mapView.zoomLevelDouble + 2.2)
                    true
                }
                mapView.overlays.add(m)
            }
        }
        mapView.invalidate()
    }

    fun reload() {
        scope.launch {
            val bb = mapView.boundingBox ?: return@launch
            val list = runCatching {
                DropsRepository.listInArea(bb.latSouth, bb.latNorth, bb.lonWest, bb.lonEast)
            }.getOrDefault(emptyList())
            ms.drops = list
            status = if (list.isEmpty()) "Здесь пока пусто — оставь первый трек" else "${list.size} рядом · двигай карту"
            ms.render()
        }
    }

    // Первичная загрузка + дебаунс при движении карты.
    DisposableEffect(Unit) {
        var reloadJob: Job? = null
        var renderJob: Job? = null
        mapView.post { reload() }
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                reloadJob?.cancel(); reloadJob = scope.launch { delay(600); reload() }
                return false
            }
            override fun onZoom(event: ZoomEvent?): Boolean {
                renderJob?.cancel(); renderJob = scope.launch { delay(120); ms.render() }
                reloadJob?.cancel(); reloadJob = scope.launch { delay(600); reload() }
                return false
            }
        }
        mapView.addMapListener(listener)
        onDispose { mapView.removeMapListener(listener); reloadJob?.cancel(); renderJob?.cancel() }
    }

    // Глобальный поиск песен по карте (по свежим пинам, фильтр на клиенте).
    LaunchedEffect(mapQuery) {
        val q = mapQuery.trim()
        if (q.length < 2) { searchHits = emptyList(); return@LaunchedEffect }
        if (recentCache.isEmpty()) {
            recentCache = runCatching { DropsRepository.recent() }.getOrDefault(emptyList())
        }
        val lq = q.lowercase()
        searchHits = recentCache.filter {
            it.title.lowercase().contains(lq) || (it.artist?.lowercase()?.contains(lq) == true)
        }.take(20)
    }

    fun flyTo(d: MapDrop) {
        mapView.controller.animateTo(GeoPoint(d.lat, d.lng))
        mapView.controller.setZoom(13.0)
        selected = d
        mapQuery = ""
        searchHits = emptyList()
    }

    fun fetchLocationThenCaption(track: TrackItem) {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        status = "Определяю геолокацию…"
        fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc == null) { status = "Не удалось получить геолокацию"; return@addOnSuccessListener }
                status = null
                pendingTrack = track
                captionFor = loc.latitude to loc.longitude
            }
            .addOnFailureListener { status = "Геолокация недоступна" }
    }

    var trackToDrop by remember { mutableStateOf<TrackItem?>(null) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val t = trackToDrop
        if (granted && t != null) fetchLocationThenCaption(t) else status = "Нужен доступ к геолокации"
    }

    fun startDrop(track: TrackItem) {
        pickerOpen = false
        trackToDrop = track
        val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (ok) fetchLocationThenCaption(track) else permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Box(modifier = Modifier.fillMaxSize().background(MAP_BG)) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // Верхняя панель — заголовок + поиск песен по карте.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xF20B1610), Color(0xCC0B1610), Color(0x000B1610))))
                .padding(start = 6.dp, end = 14.dp, top = 36.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp)) {
                Surface(
                    color = SOLID,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(42.dp).clickable(onClick = onClose),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, contentDescription = "Закрыть", tint = Color.White)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Карта музыки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        status ?: "${ms.drops.size} рядом · двигай карту",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            TextField(
                value = mapQuery,
                onValueChange = { mapQuery = it },
                placeholder = { Text("Искать песни на карте", color = Color.White.copy(alpha = 0.45f)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (mapQuery.isNotEmpty()) {
                        IconButton(onClick = { mapQuery = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Очистить", tint = Color.White.copy(alpha = 0.7f))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SOLID,
                    unfocusedContainerColor = SOLID,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            )
            if (searchHits.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = GLASS,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 6.dp).heightIn(max = 300.dp),
                ) {
                    LazyColumn(modifier = Modifier.padding(6.dp)) {
                        items(searchHits, key = { it.id }) { d ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { flyTo(d) }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AsyncImage(
                                    model = d.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.06f)),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(d.title, color = Color.White, maxLines = 1, fontWeight = FontWeight.Medium)
                                    d.artist?.let { Text(it, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                                }
                                Icon(Icons.Rounded.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Низ карты: карточка пина → мини-плеер → кнопка во всю ширину.
        var lastSel by remember { mutableStateOf<MapDrop?>(null) }
        LaunchedEffect(selected) { if (selected != null) lastSel = selected }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = bottomInset),
        ) {
            AnimatedVisibility(
                visible = selected != null,
                enter = slideInVertically(tween(260)) { it } + fadeIn(),
                exit = slideOutVertically(tween(220)) { it } + fadeOut(),
            ) {
                val d = lastSel
                if (d != null) {
                    Surface(
                        color = GLASS,
                        shape = RoundedCornerShape(22.dp),
                        tonalElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = d.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)),
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(d.title, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
                                    d.artist?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), maxLines = 1) }
                                }
                                IconButton(onClick = { selected = null }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Закрыть", tint = Color.White.copy(alpha = 0.8f))
                                }
                            }
                            if (d.caption.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text("«${d.caption}»", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                            }
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { onPlay(d.toTrackItem()) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Слушать", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            // Мини-плеер (если что-то играет) — над кнопкой.
            nowPlayingBar()
            // Кнопка во всю ширину.
            DropButton(
                onClick = { pickerOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 16.dp),
            )
        }

        // Выбор трека для пина — затемнение + лист, выезжающий снизу.
        AnimatedVisibility(visible = pickerOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable { pickerOpen = false })
        }
        AnimatedVisibility(
            visible = pickerOpen,
            enter = slideInVertically(tween(300)) { it } + fadeIn(),
            exit = slideOutVertically(tween(240)) { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            TrackPickerSheet(
                onSearch = onSearch,
                onPick = { startDrop(it) },
            )
        }
        if (pickerOpen) BackHandler { pickerOpen = false }

        // Диалог описания при создании пина.
        captionFor?.let { (lat, lng) ->
            val track = pendingTrack
            var caption by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { captionFor = null }) {
                Surface(shape = RoundedCornerShape(24.dp), color = GLASS) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Оставить здесь", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            track?.let { "${it.uploader?.let { u -> "$u — " } ?: ""}${it.title}" } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = caption,
                            onValueChange = { if (it.length <= 200) caption = it },
                            label = { Text("Описание (необязательно)") },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { captionFor = null }) { Text("Отмена") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = {
                                val t = track ?: return@Button
                                captionFor = null
                                status = "Публикую…"
                                scope.launch {
                                    runCatching { DropsRepository.create(lat, lng, t, caption.trim()) }
                                        .onSuccess { status = "Готово ✓"; reload() }
                                        .onFailure { status = "Ошибка: ${it.message}" }
                                }
                            }) { Text("Оставить") }
                        }
                    }
                }
            }
        }
    }
}

/** Кнопка «оставить трек» во всю ширину — градиент + лёгкое покачивание иконки. */
@Composable
private fun DropButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "drop")
    val bob by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val accent = MaterialTheme.colorScheme.primary
    val accentLight = androidx.compose.ui.graphics.lerp(accent, Color.White, 0.28f)

    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(accent, accentLight)))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(22.dp).graphicsLayer { translationY = -bob * 3f },
        )
        Spacer(Modifier.width(9.dp))
        Text("Оставить трек здесь", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
    }
}

/** Нижний лист выбора трека: избранное / плейлисты / поиск. */
@Composable
private fun TrackPickerSheet(
    onSearch: (String) -> Flow<List<TrackItem>>,
    onPick: (TrackItem) -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    var openPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TrackItem>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    val favorites = remember { FavoritesManager.getAll() }
    val playlists = remember { PlaylistManager.getAll() }

    LaunchedEffect(query) {
        if (query.isBlank()) { results = emptyList(); searching = false; return@LaunchedEffect }
        searching = true
        delay(350)
        onSearch(query).collectLatest { results = it; searching = false }
    }

    Surface(
        color = MAP_BG,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                )
                Spacer(Modifier.height(14.dp))
                Text("Что оставить на карте?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(14.dp))

                // Сегменты-вкладки.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PickerTab("Избранное", Icons.Rounded.Favorite, tab == 0, Modifier.weight(1f)) { tab = 0; openPlaylist = null }
                    PickerTab("Плейлисты", Icons.Rounded.QueueMusic, tab == 1, Modifier.weight(1f)) { tab = 1 }
                    PickerTab("Поиск", Icons.Rounded.Search, tab == 2, Modifier.weight(1f)) { tab = 2 }
                }
                Spacer(Modifier.height(14.dp))

                when (tab) {
                    0 -> TrackList(favorites, onPick)
                    1 -> {
                        val pl = openPlaylist
                        if (pl == null) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(playlists, key = { it.id }) { p ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { openPlaylist = p }
                                            .padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AsyncImage(
                                            model = p.coverUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.06f)),
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(p.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            Text("${p.tracks.size} треков", color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(Modifier.fillMaxSize()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { openPlaylist = null }) {
                                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад", tint = Color.White)
                                    }
                                    Text(pl.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }
                                TrackList(pl.tracks, onPick)
                            }
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Поиск трека") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        if (searching) {
                            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                            }
                        }
                        TrackList(results, onPick)
                    }
                }
            }
        }
}

@Composable
private fun PickerTab(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.06f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(44.dp).clickable(onClick = onClick),
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = if (active) Color(0xFF0C3A26) else Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.width(6.dp))
            Text(label, color = if (active) Color(0xFF0C3A26) else Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TrackList(tracks: List<TrackItem>, onPick: (TrackItem) -> Unit) {
    if (tracks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Пусто", color = Color.White.copy(alpha = 0.4f))
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(tracks, key = { it.url + "@" + it.speed }) { t ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(t) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = t.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.06f)),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, color = Color.White, maxLines = 1, fontWeight = FontWeight.Medium)
                    t.uploader?.let { Text(it, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.bodySmall, maxLines = 1) }
                }
                Icon(Icons.Rounded.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ---------- Кластеризация ----------

private class Cluster {
    val items = ArrayList<MapDrop>()
    fun center(): GeoPoint = GeoPoint(items.sumOf { it.lat } / items.size, items.sumOf { it.lng } / items.size)
}

private fun clusterDrops(list: List<MapDrop>, proj: Projection, radiusPx: Float): List<Cluster> {
    if (list.isEmpty()) return emptyList()
    val pts = list.map { proj.toPixels(GeoPoint(it.lat, it.lng), null) ?: Point(0, 0) }
    val used = BooleanArray(list.size)
    val out = ArrayList<Cluster>()
    val r2 = radiusPx * radiusPx
    for (i in list.indices) {
        if (used[i]) continue
        used[i] = true
        val cl = Cluster().apply { items.add(list[i]) }
        for (j in i + 1 until list.size) {
            if (used[j]) continue
            val dx = (pts[j].x - pts[i].x).toFloat()
            val dy = (pts[j].y - pts[i].y).toFloat()
            if (dx * dx + dy * dy <= r2) { used[j] = true; cl.items.add(list[j]) }
        }
        out.add(cl)
    }
    return out
}

// ---------- Отрисовка маркеров ----------

private fun Context.px(dp: Float): Float = dp * resources.displayMetrics.density

private suspend fun loadArtBitmap(context: Context, url: String): Bitmap? = runCatching {
    val req = ImageRequest.Builder(context).data(url).allowHardware(false).size(140).build()
    val r = context.imageLoader.execute(req)
    (r as? SuccessResult)?.drawable?.let { (it as? BitmapDrawable)?.bitmap }
}.getOrNull()

/** Круглый пин с обложкой (или акцентной заливкой и нотой) и белым хвостиком снизу. */
private fun makePinBitmap(context: Context, art: Bitmap?, accent: Int): Bitmap {
    val diam = context.px(46f)
    val ring = context.px(3f)
    val tail = context.px(11f)
    val w = diam.toInt()
    val h = (diam + tail).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = w / 2f
    val cy = diam / 2f
    val r = diam / 2f
    val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AColor.WHITE }

    // Хвостик-капля.
    val path = Path().apply {
        moveTo(cx - r * 0.46f, cy + r * 0.62f)
        lineTo(cx, h.toFloat())
        lineTo(cx + r * 0.46f, cy + r * 0.62f)
        close()
    }
    c.drawPath(path, white)
    c.drawCircle(cx, cy, r, white)

    val ir = r - ring
    if (art != null) {
        val cropped = scaleCrop(art, (ir * 2).toInt())
        val shader = BitmapShader(cropped, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(Matrix().apply { setTranslate(cx - ir, cy - ir) })
        c.drawCircle(cx, cy, ir, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
    } else {
        c.drawCircle(cx, cy, ir, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AColor.WHITE; textAlign = Paint.Align.CENTER
            textSize = ir * 1.15f; typeface = Typeface.DEFAULT_BOLD
        }
        val fm = tp.fontMetrics
        c.drawText("♪", cx, cy - (fm.ascent + fm.descent) / 2, tp)
    }
    return bmp
}

/** Кружок кластера с числом; растёт с количеством. */
private fun makeClusterBitmap(context: Context, count: Int, accent: Int): Bitmap {
    val base = context.px(40f)
    val grow = context.px((ln(count.toDouble()) * 6.5f).toFloat())
    val diam = (base + grow).coerceAtMost(context.px(80f))
    val halo = context.px(7f)
    val total = (diam + halo * 2).toInt()
    val bmp = Bitmap.createBitmap(total, total, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = total / 2f
    val r = diam / 2f

    // Внешнее свечение.
    c.drawCircle(cx, cx, r + halo * 0.7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(accent, 0x4D) })
    // Градиентная заливка.
    val grad = LinearGradient(cx - r, cx - r, cx + r, cx + r, lighten(accent, 0.35f), accent, Shader.TileMode.CLAMP)
    c.drawCircle(cx, cx, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = grad })
    // Тонкая белая обводка.
    c.drawCircle(cx, cx, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = context.px(2f); color = withAlpha(AColor.WHITE, 0x66)
    })

    val label = if (count >= 1000) "${count / 1000}k+" else count.toString()
    val factor = when {
        label.length >= 4 -> 0.58f
        label.length == 3 -> 0.74f
        else -> 0.92f
    }
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.WHITE; textAlign = Paint.Align.CENTER
        textSize = r * factor; typeface = Typeface.DEFAULT_BOLD
    }
    val fm = tp.fontMetrics
    c.drawText(label, cx, cx - (fm.ascent + fm.descent) / 2, tp)
    return bmp
}

private fun scaleCrop(src: Bitmap, size: Int): Bitmap {
    if (size <= 0) return src
    val scale = max(size.toFloat() / src.width, size.toFloat() / src.height)
    val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val cv = Canvas(out)
    val m = Matrix().apply {
        setScale(scale, scale)
        postTranslate((size - src.width * scale) / 2f, (size - src.height * scale) / 2f)
    }
    cv.drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    return out
}

private fun lighten(c: Int, f: Float): Int = AColor.rgb(
    (AColor.red(c) + (255 - AColor.red(c)) * f).toInt(),
    (AColor.green(c) + (255 - AColor.green(c)) * f).toInt(),
    (AColor.blue(c) + (255 - AColor.blue(c)) * f).toInt(),
)

private fun withAlpha(c: Int, a: Int): Int = AColor.argb(a, AColor.red(c), AColor.green(c), AColor.blue(c))
