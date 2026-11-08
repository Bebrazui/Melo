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
internal fun AccountTab(
    onOpenPlaylist: (Playlist) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenLogin: () -> Unit,
    refreshKey: Int,
    onOpenArtist: (TrackItem) -> Unit = {},
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val accountScope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    var playlists by remember(refreshKey) { mutableStateOf(PlaylistManager.getAll().toList()) }
    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }
    var menuTarget by remember { mutableStateOf<Playlist?>(null) }
    val isDark = cs.background.luminance() < 0.5f
    val cardBg = cs.surfaceContainer
    val cardBorder: BorderStroke? = null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .bouncyOverscroll()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp)
            .padding(bottom = bottomInset),
    ) {
        // Шапка: заголовок в стиле M3 Expressive + шестерёнка настроек справа.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Аккаунт",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp,
                    ),
                    fontWeight = FontWeight.Black,
                    color = cs.onBackground,
                )
                com.melo.music.auth.AuthManager.email?.let { mail ->
                    Text(
                        mail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (com.melo.music.auth.AuthManager.loggedIn) {
                    Surface(
                        shape = CircleShape,
                        color = cs.surfaceContainerHigh,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { accountScope.launch { com.melo.music.auth.AuthManager.logout() } },
                    ) {
                        Text(
                            "Выйти",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        color = cs.primaryContainer,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenLogin),
                    ) {
                        Text(
                            "Войти",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = cs.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = cs.surfaceContainerHigh,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onOpenSettings),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "Настройки",
                            tint = cs.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        // Профиль (Hero Card) — для вошедших и для гостей.
        if (com.melo.music.auth.AuthManager.loggedIn) {
            ProfileHeaderCard()
            val uid = com.melo.music.auth.AuthManager.userId
            val clipboard = LocalClipboardManager.current
            if (uid != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "ID: $uid (тап — копировать)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { clipboard.setText(AnnotatedString(uid)) },
                    )
                }
            }
        } else {
            // Bento Hero карточка для Гостя (в едином стиле темы приложения)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = cardBg,
                border = cardBorder,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = cs.primaryContainer,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = cs.onPrimaryContainer,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Гостевой профиль",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                fontWeight = FontWeight.Bold,
                                color = cs.onSurface,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Синхронизируй плейлисты и делись треками на карте",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onOpenLogin,
                        shape = RoundedCornerShape(24.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Войти в аккаунт", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Bento-сетка быстрой статистики (Любимое + Подписки + Плейлисты)
        val favCount = remember(refreshKey) { com.melo.music.favorites.FavoritesManager.getAll().size }
        val subVersion = com.melo.music.artists.ArtistSubscriptionManager.version
        val subscriptions = remember(refreshKey, subVersion) { com.melo.music.artists.ArtistSubscriptionManager.getAll() }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Карточка «Избранное»
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = cardBorder,
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE53935).copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFFF6E6E),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "$favCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                    )
                    Text(
                        "В Избранном",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            // Карточка «Подписки»
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = cardBorder,
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = cs.tertiaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = cs.onTertiaryContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${subscriptions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                    )
                    Text(
                        "Подписки",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            // Карточка «Плейлисты»
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cardBg,
                border = cardBorder,
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = cs.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.LibraryMusic,
                                contentDescription = null,
                                tint = cs.onPrimaryContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "${playlists.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                    )
                    Text(
                        "Плейлисты",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }

        if (subscriptions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Подписки на исполнителей",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                )
                Text(
                    "${subscriptions.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 18.dp),
            ) {
                items(subscriptions, key = { "acc_sub_${it.url}" }) { artist ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(84.dp)
                            .clickable { onOpenArtist(artist) },
                    ) {
                        Artwork(
                            url = artist.thumbnailUrl,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = artist.cleanTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            color = cs.onSurface,
                        )
                    }
                }
            }
        }

        // Секция «Мои плейлисты»
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Мои плейлисты",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    letterSpacing = (-0.3).sp,
                ),
                fontWeight = FontWeight.ExtraBold,
                color = cs.onBackground,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = CircleShape,
                color = cs.primaryContainer,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { showCreate = true },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        tint = cs.onPrimaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Создать",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onPrimaryContainer,
                    )
                }
            }
        }

        // Синхронизация с YouTube Music (если выполнен вход)
        if (com.melo.music.auth.YouTubeAccountManager.isLoggedIn) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            var isSyncingLib by remember { mutableStateOf(false) }
            var syncMsg by remember { mutableStateOf<String?>(null) }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = cardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable {
                        if (isSyncingLib) return@clickable
                        ClickFeedback.play()
                        isSyncingLib = true
                        accountScope.launch {
                            val res = com.melo.music.sync.YouTubeSyncManager.syncLibrary(ctx) {
                                syncMsg = it
                            }
                            isSyncingLib = false
                            playlists = PlaylistManager.getAll().toList()
                            syncMsg = if (res.error != null) res.error else "Синхронизировано: ${res.likedCount} лайков, ${res.playlistsCount} плейлистов"
                        }
                    },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = cs.primaryContainer,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.CloudDownload,
                                contentDescription = null,
                                tint = cs.onPrimaryContainer,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isSyncingLib) "Синхронизация с YouTube..." else "Синхронизировать YouTube Music",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface,
                        )
                        Text(
                            syncMsg ?: "Подтянуть «Понравившиеся» и личные плейлисты",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (syncMsg != null) cs.primary else cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Импорт плейлиста из других сервисов (Material 3 Expressive Frosted Card)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = cardBorder,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onOpenImport),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Импорт плейлиста",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                    )
                    Text(
                        "Из YouTube Music или SoundCloud",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = cs.onSurfaceVariant,
                )
            }
        }

        if (playlists.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(cs.surfaceContainer)
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Здесь будут твои плейлисты",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Добавляй треки долгим тапом → «В плейлист»",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = { showCreate = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Создать плейлист")
                }
            }
        } else {
            // Квадратные карточки по 2 в ряд.
            playlists.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    for (pl in row) {
                        PlaylistCard(
                            playlist = pl,
                            onClick = { onOpenPlaylist(pl) },
                            onLongClick = { menuTarget = pl },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        MeloDialog(onDismiss = { showCreate = false }) {
            Text(
                "Новый плейлист",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { showCreate = false }) { Text("Отмена") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            PlaylistManager.create(name)
                            playlists = PlaylistManager.getAll().toList()
                        }
                        showCreate = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Создать") }
            }
        }
    }

    menuTarget?.let { target ->
        MeloDialog(onDismiss = { menuTarget = null }) {
            Text(target.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        PlaylistManager.setPublic(target.id, !target.isPublic)
                        playlists = PlaylistManager.getAll().toList()
                        menuTarget = null
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (target.isPublic) Icons.Rounded.Lock else Icons.Rounded.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(if (target.isPublic) "Сделать закрытым" else "Сделать открытым", fontWeight = FontWeight.Medium)
                    Text(
                        if (target.isPublic) "Сейчас виден другим" else "Сейчас скрыт",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { deleteTarget = target; menuTarget = null }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(14.dp))
                Text("Удалить плейлист", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }
        }
    }

    deleteTarget?.let { target ->
        MeloDialog(onDismiss = { deleteTarget = null }) {
            Text(
                "Удалить плейлист?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "«${target.name}» будет удалён безвозвратно.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { deleteTarget = null }) { Text("Отмена") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        PlaylistManager.delete(target.id)
                        playlists = PlaylistManager.getAll().toList()
                        deleteTarget = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Удалить") }
            }
        }
    }
}

@Composable
internal fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val placeholderBg = Color(0xFF26262C)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        if (playlist.coverUrl != null) {
            Artwork(url = playlist.coverUrl, modifier = Modifier.fillMaxSize())
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(placeholderBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.28f),
                    modifier = Modifier.size(46.dp),
                )
            }
        }

        // Скрим снизу для читаемости текста.
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.5f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.72f),
                ),
            ),
        )

        // Бейдж «закрытый» в углу.
        if (!playlist.isPublic) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(6.dp),
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = "Закрытый", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        ) {
            Text(
                playlist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
            )
            Text(
                "${playlist.tracks.size} треков",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

/** Стилизованный диалог в духе приложения (скруглённый, с тенью и пружинистой анимацией). */
@Composable
internal fun MeloDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val animState = remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = animState,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                initialScale = 0.84f,
            ) + fadeIn(tween(180)),
            exit = scaleOut(tween(140), targetScale = 0.84f) + fadeOut(tween(140)),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
                tonalElevation = 6.dp,
            ) {
                Column(modifier = Modifier.padding(24.dp), content = content)
            }
        }
    }
}

