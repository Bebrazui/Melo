@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.melo.music.ui

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import com.melo.music.extractor.NewPipeResolver
import com.melo.music.extractor.cleanArtistName
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ripple
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.melo.music.R
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.melo.music.audio.EqualizerManager
import com.melo.music.extractor.ItemKind
import com.melo.music.history.HistoryManager
import com.melo.music.lyrics.LrcLine
import com.melo.music.lyrics.Lyrics
import com.melo.music.playlists.Playlist
import com.melo.music.playlists.PlaylistManager
import com.melo.music.ui.theme.Motion
import com.melo.music.ui.theme.ShapeCache
import com.melo.music.ui.theme.pressScale
import com.melo.music.ui.theme.carouselCenterItemEffect
import com.melo.music.ui.theme.carouselCenterGridItemEffect
import com.melo.music.ui.theme.verticalScrollEdgeItemEffect
import com.melo.music.ui.theme.bouncyOverscroll
import com.melo.music.ui.theme.bouncyHorizontalOverscroll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import com.melo.music.extractor.ResolvedTrack
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import com.melo.music.favorites.FavoritesManager
import com.melo.music.recommend.Recommender
import com.melo.music.recommend.SkipTracker
import com.melo.music.recommend.TasteProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.melo.music.ui.sound.ClickFeedback
import java.util.Locale
import kotlin.math.roundToInt


@Composable
internal fun ArtistScreen(
    artist: TrackItem,
    onLoadTracks: suspend () -> List<TrackItem>,
    onLoadAlbums: suspend () -> List<TrackItem>,
    onLoadSimilar: suspend (List<TrackItem>) -> List<TrackItem>,
    nowPlayingUrl: String?,
    isPlaying: Boolean,
    resolvingUrl: String?,
    audioSessionId: Int,
    onPlay: (List<TrackItem>, Int) -> Unit,
    onShuffle: (List<TrackItem>) -> Unit,
    onLoadAlbumTracks: suspend (String) -> List<TrackItem>,
    onTrackLongClick: (TrackItem) -> Unit,
    onOpenSimilar: (TrackItem) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    var tracks by remember(artist.url) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var albums by remember(artist.url) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var similar by remember(artist.url) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var loading by remember(artist.url) { mutableStateOf(true) }
    var error by remember(artist.url) { mutableStateOf<String?>(null) }

    LaunchedEffect(artist.url) {
        loading = true
        error = null
        runCatching { onLoadTracks() }
            .onSuccess { tracks = it }
            .onFailure { error = it.message }
        loading = false
    }
    LaunchedEffect(artist.url) {
        albums = runCatching { onLoadAlbums() }.getOrDefault(emptyList())
    }
    LaunchedEffect(tracks) {
        if (tracks.isEmpty() || similar.isNotEmpty()) return@LaunchedEffect
        similar = runCatching { onLoadSimilar(tracks) }.getOrDefault(emptyList())
    }

    val popular = remember(tracks) { tracks.sortedByDescending { it.viewCount }.take(5) }
    val popularUrls = remember(popular) { popular.map { it.url }.toSet() }
    val otherTracks = tracks.filter { it.url !in popularUrls }

    val hiRes = remember(artist.thumbnailUrl) { upscaleThumb(artist.thumbnailUrl, 700) }
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val white = if (isDark) Color.White else cs.onBackground

    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.primary
    var accent by remember(artist.url) { mutableStateOf(fallback) }
    LaunchedEffect(hiRes) {
        val url = hiRes ?: return@LaunchedEffect
        runCatching {
            val req = ImageRequest.Builder(context).data(url).allowHardware(false).size(256).build()
            val drawable = context.imageLoader.execute(req).drawable
            val bmp = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            if (bmp != null) {
                val palette = androidx.palette.graphics.Palette.from(bmp).generate()
                val rgb = palette.vibrantSwatch?.rgb
                    ?: palette.lightVibrantSwatch?.rgb
                    ?: palette.dominantSwatch?.rgb
                if (rgb != null) accent = Color(rgb)
            }
        }
    }

    val animatedAccent by animateColorAsState(targetValue = accent, animationSpec = tween(500), label = "artistAccent")
    val cardTint = if (isDark) lerp(animatedAccent, Color(0xFF141218), 0.78f) else lerp(animatedAccent, cs.surfaceContainerLow, 0.85f)
    val onAccent = if (animatedAccent.luminance() > 0.45f) Color.Black else Color.White
    val pageBg = if (isDark) Color(0xFF0E0C11) else cs.background

    Box(modifier = Modifier.fillMaxSize().background(pageBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().bouncyOverscroll(),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp),
                ) {
                    Artwork(
                        url = hiRes ?: artist.thumbnailUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to (if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Transparent),
                                    0.40f to animatedAccent.copy(alpha = if (isDark) 0.20f else 0.12f),
                                    0.75f to pageBg.copy(alpha = 0.88f),
                                    1f to pageBg,
                                ),
                            ),
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 14.dp, start = 16.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClose),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Назад",
                                tint = white,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            ) {
                                SourceBadge(artist.source, Modifier.size(13.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "АРТИСТ · ${sourceLabel(artist.source).uppercase()}",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = white.copy(alpha = 0.90f),
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = cleanArtistName(artist.title) ?: artist.title,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = 34.sp,
                                lineHeight = 36.sp,
                                letterSpacing = (-0.6).sp,
                            ),
                            fontWeight = FontWeight.Black,
                            color = white,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = if (tracks.isNotEmpty()) {
                                "${tracks.size} треков${if (albums.isNotEmpty()) " · ${albums.size} альбомов" else ""}"
                            } else {
                                "Загрузка дискографии..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = white.copy(alpha = 0.70f),
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(26.dp),
                                color = animatedAccent,
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .clickable(enabled = tracks.isNotEmpty()) { onPlay(tracks, 0) },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 22.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = onAccent,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Слушать",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = onAccent,
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = tracks.isNotEmpty()) { onShuffle(tracks) },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shuffle,
                                        contentDescription = "Перемешать",
                                        tint = white,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (popular.isNotEmpty()) {
                item(key = "popular_header") {
                    Text(
                        text = "Популярное",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            letterSpacing = (-0.3).sp,
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = white,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
                    )
                }
                itemsIndexed(popular, key = { _, t -> "pop_" + t.url }) { i, t ->
                    val isCurrent = nowPlayingUrl == t.url
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isCurrent) animatedAccent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, if (isCurrent) animatedAccent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .combinedClickable(
                                onClick = { onPlay(tracks, tracks.indexOf(t)) },
                                onLongClick = { onTrackLongClick(t) },
                            ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${i + 1}",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) animatedAccent else white.copy(alpha = 0.40f),
                                modifier = Modifier.width(24.dp),
                            )
                            Artwork(t.thumbnailUrl, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = t.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = white,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val sub = listOfNotNull(
                                    formatViews(t.viewCount).takeIf { it.isNotBlank() },
                                    formatDuration(t.durationSeconds).takeIf { it.isNotBlank() },
                                    HistoryManager.playCount(t.url).takeIf { it > 0 }?.let { "слушали $it раз" },
                                ).joinToString(" · ")
                                if (sub.isNotBlank()) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = white.copy(alpha = 0.60f),
                                        maxLines = 1,
                                    )
                                }
                            }
                            if (resolvingUrl == t.url) {
                                LoadingIndicator(color = animatedAccent, modifier = Modifier.size(20.dp))
                            } else if (isCurrent && isPlaying) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = animatedAccent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (albums.isNotEmpty()) {
                item(key = "albums_header") {
                    Text(
                        text = "Альбомы и релизы",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            letterSpacing = (-0.3).sp,
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = white,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
                    )
                }
                items(albums.distinctBy { it.url }, key = { it.url }) { al ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                        AlbumCard(
                            album = al,
                            accent = animatedAccent,
                            onAccent = onAccent,
                            cardTint = cardTint,
                            nowPlayingUrl = nowPlayingUrl,
                            isPlaying = isPlaying,
                            resolvingUrl = resolvingUrl,
                            onLoadTracks = { onLoadAlbumTracks(al.url) },
                            onPlay = onPlay,
                            onTrackLongClick = onTrackLongClick,
                        )
                    }
                }
            }

            when {
                loading -> item(key = "tracks_loading") {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator(color = animatedAccent, modifier = Modifier.size(48.dp))
                    }
                }
                error != null && tracks.isEmpty() -> item(key = "tracks_error") {
                    Text(
                        text = "Не удалось загрузить дискографию: $error",
                        color = white.copy(alpha = 0.8f),
                        modifier = Modifier.padding(24.dp),
                    )
                }
                tracks.isEmpty() -> item(key = "tracks_empty") {
                    Text(
                        text = "Треки не найдены",
                        color = white.copy(alpha = 0.8f),
                        modifier = Modifier.padding(24.dp),
                    )
                }
                otherTracks.isNotEmpty() -> {
                    item(key = "all_tracks_header") {
                        Text(
                            text = "Все треки",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 21.sp,
                                letterSpacing = (-0.3).sp,
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = white,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
                        )
                    }
                    val rows = otherTracks.chunked(2)
                    itemsIndexed(rows, key = { i, row -> i.toString() + row.joinToString("|") { it.url } }) { _, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            row.forEach { t ->
                                ArtistTrackTile(
                                    item = t,
                                    playing = nowPlayingUrl == t.url,
                                    resolving = resolvingUrl == t.url,
                                    accent = animatedAccent,
                                    onClick = { onPlay(tracks, tracks.indexOf(t)) },
                                    onLongClick = { onTrackLongClick(t) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            if (similar.isNotEmpty()) {
                item(key = "similar_header") {
                    Text(
                        text = "Похожие артисты",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            letterSpacing = (-0.3).sp,
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = white,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
                    )
                }
                item(key = "similar_row") {
                    val simRowState = rememberLazyListState()
                    LazyRow(
                        state = simRowState,
                        modifier = Modifier.bouncyHorizontalOverscroll(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        itemsIndexed(similar, key = { _, a -> "sim_" + a.url }) { index, a ->
                            SimilarArtistTile(
                                item = a,
                                accent = animatedAccent,
                                onClick = { onOpenSimilar(a) },
                                modifier = Modifier.carouselCenterItemEffect(simRowState, index),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AlbumCard(
    album: TrackItem,
    accent: Color,
    onAccent: Color,
    cardTint: Color,
    nowPlayingUrl: String?,
    isPlaying: Boolean,
    resolvingUrl: String?,
    onLoadTracks: suspend () -> List<TrackItem>,
    onPlay: (List<TrackItem>, Int) -> Unit,
    onTrackLongClick: (TrackItem) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val white = if (isDark) Color.White else cs.onBackground
    var expanded by remember(album.url) { mutableStateOf(false) }
    var tracks by remember(album.url) { mutableStateOf<List<TrackItem>>(emptyList()) }
    var loading by remember(album.url) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load(then: (List<TrackItem>) -> Unit = {}) {
        if (tracks.isNotEmpty()) { then(tracks); return }
        if (loading) return
        loading = true
        scope.launch {
            val r = runCatching { onLoadTracks() }.getOrDefault(emptyList())
            tracks = r
            loading = false
            then(r)
        }
    }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chev")

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardTint,
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else cs.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded; if (expanded) load() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(album.thumbnailUrl, Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)))
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = white,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (tracks.isNotEmpty()) "Альбом · ${tracks.size} треков" else "Альбом",
                        style = MaterialTheme.typography.bodySmall,
                        color = white.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = accent,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { load { if (it.isNotEmpty()) onPlay(it, 0) } },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Играть альбом",
                            tint = onAccent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                IconButton(onClick = { expanded = !expanded; if (expanded) load() }) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = white,
                        modifier = Modifier.rotate(rotation),
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(bottom = 8.dp, start = 8.dp, end = 8.dp)) {
                    if (loading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            LoadingIndicator(
                                color = accent, modifier = Modifier.size(28.dp),
                            )
                        }
                    } else {
                        tracks.forEachIndexed { i, t ->
                            val isTrackCurrent = nowPlayingUrl == t.url
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isTrackCurrent) accent.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .combinedClickable(
                                        onClick = { onPlay(tracks, i) },
                                        onLongClick = { onTrackLongClick(t) },
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Artwork(t.thumbnailUrl, Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)))
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = t.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = white,
                                            maxLines = 1,
                                            fontWeight = if (isTrackCurrent) FontWeight.Bold else FontWeight.Normal,
                                        )
                                        t.uploader?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = white.copy(alpha = 0.65f),
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                    if (resolvingUrl == t.url) {
                                        LoadingIndicator(
                                            color = accent, modifier = Modifier.size(20.dp),
                                        )
                                    } else if (isTrackCurrent && isPlaying) {
                                        Icon(
                                            Icons.Rounded.MusicNote, contentDescription = null,
                                            tint = accent, modifier = Modifier.size(18.dp),
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
}

/** Плитка трека в 2-колоночной сетке «Все треки» (Material 3 Expressive). */
@Composable
internal fun ArtistTrackTile(
    item: TrackItem,
    playing: Boolean,
    resolving: Boolean,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val white = if (isDark) Color.White else cs.onSurface
    val cardBg = if (isDark) Color.White.copy(alpha = 0.05f) else cs.surfaceContainerLow
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.06f) else cs.outlineVariant.copy(alpha = 0.35f)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (playing) accent.copy(alpha = 0.18f) else cardBg,
        border = BorderStroke(1.dp, if (playing) accent.copy(alpha = 0.40f) else cardBorder),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Artwork(item.thumbnailUrl, Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)))
                if (resolving) {
                    Box(
                        modifier = Modifier.size(46.dp).background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator(color = accent, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.cleanTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (playing) FontWeight.Bold else FontWeight.Medium,
                    color = white,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.cleanUploader?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = white.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (playing) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** Круглая выразительная плитка похожего артиста с неоновым обрамлением. */
@Composable
internal fun SimilarArtistTile(
    item: TrackItem,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(2.dp, accent.copy(alpha = 0.50f)),
            modifier = Modifier.size(76.dp),
        ) {
            Artwork(
                url = item.thumbnailUrl,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Короткий формат просмотров: 1234567 → «1,2 млн». */
private fun formatViews(v: Long): String = when {
    v >= 1_000_000 -> String.format(Locale.getDefault(), "%.1f млн", v / 1_000_000.0)
    v >= 1_000 -> String.format(Locale.getDefault(), "%.0f тыс.", v / 1_000.0)
    v > 0 -> v.toString()
    else -> ""
}

