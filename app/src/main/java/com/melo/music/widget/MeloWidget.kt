package com.melo.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.melo.music.MainActivity
import com.melo.music.R
import com.melo.music.playback.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Маленький квадратный виджет 2×2 */
class MeloWidgetSmall : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        WidgetUpdater.render(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        WidgetUpdater.handleAction(context, intent.action)
    }
}

/** Средний горизонтальный виджет 4×1 / 4×2 */
class MeloWidgetMedium : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        WidgetUpdater.render(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        WidgetUpdater.handleAction(context, intent.action)
    }
}

/** Большой расширенный медиа-центр 4×3 / 4×4 */
class MeloWidgetLarge : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        WidgetUpdater.render(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        WidgetUpdater.handleAction(context, intent.action)
    }
}

/** Для обратной совместимости со старыми установленными виджетами */
class MeloWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        WidgetUpdater.render(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        WidgetUpdater.handleAction(context, intent.action)
    }
}

/**
 * Состояние и централизованный рендеринг всех 3 размеров виджетов.
 */
object WidgetUpdater {
    const val ACTION_TOGGLE = "com.melo.music.widget.TOGGLE"
    const val ACTION_NEXT = "com.melo.music.widget.NEXT"
    const val ACTION_PREV = "com.melo.music.widget.PREV"
    const val ACTION_LIKE = "com.melo.music.widget.LIKE"
    const val ACTION_SHUFFLE = "com.melo.music.widget.SHUFFLE"

    @Volatile private var title: String? = null
    @Volatile private var artist: String? = null
    @Volatile private var coverUrl: String? = null
    @Volatile private var isPlaying = false
    @Volatile private var isLiked = false

    @Volatile private var cachedBitmap: Bitmap? = null
    @Volatile private var cachedUrl: String? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun setNowPlaying(
        context: Context,
        title: String?,
        artist: String?,
        coverUrl: String?,
        isPlaying: Boolean,
        isLiked: Boolean = false,
    ) {
        this.title = title
        this.artist = artist
        this.coverUrl = coverUrl
        this.isPlaying = isPlaying
        this.isLiked = isLiked
        render(context)
    }

    fun setPlaying(context: Context, playing: Boolean) {
        isPlaying = playing
        render(context)
    }

    fun handleAction(context: Context, action: String?) {
        when (action) {
            ACTION_TOGGLE -> sendKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            ACTION_NEXT -> sendKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
            ACTION_PREV -> sendKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            ACTION_LIKE -> runCatching { PlaybackService.onToggleFavorite?.invoke() }
            ACTION_SHUFFLE -> sendKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    fun sendKey(context: Context, code: Int) {
        runCatching {
            val i = Intent(Intent.ACTION_MEDIA_BUTTON).setClass(context, PlaybackService::class.java)
            i.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, code))
            context.startService(i)
        }
    }

    fun render(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val bmp = coverUrl?.takeIf { it == cachedUrl }?.let { cachedBitmap }

        // 1. Маленькие 2×2
        val smallIds = mgr.getAppWidgetIds(ComponentName(context, MeloWidgetSmall::class.java))
        if (smallIds.isNotEmpty()) {
            mgr.updateAppWidget(smallIds, buildViewsSmall(context, bmp))
        }

        // 2. Средние 4×2
        val mediumIds = mgr.getAppWidgetIds(ComponentName(context, MeloWidgetMedium::class.java))
        if (mediumIds.isNotEmpty()) {
            mgr.updateAppWidget(mediumIds, buildViewsMedium(context, bmp))
        }

        // 3. Большие 4×3
        val largeIds = mgr.getAppWidgetIds(ComponentName(context, MeloWidgetLarge::class.java))
        if (largeIds.isNotEmpty()) {
            mgr.updateAppWidget(largeIds, buildViewsLarge(context, bmp))
        }

        // Фолбэк для классического виджета MeloWidget
        val legacyIds = mgr.getAppWidgetIds(ComponentName(context, MeloWidget::class.java))
        if (legacyIds.isNotEmpty()) {
            mgr.updateAppWidget(legacyIds, buildViewsMedium(context, bmp))
        }

        if (coverUrl != null && coverUrl != cachedUrl) {
            loadCover(context, coverUrl!!)
        }
    }

    private fun buildViewsSmall(context: Context, bitmap: Bitmap?): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_small).apply {
            setTextViewText(R.id.widget_title, title ?: "Ничего не играет")
            setTextViewText(R.id.widget_artist, artist ?: "Melo")
            setImageViewResource(
                R.id.widget_play,
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )
            if (bitmap != null) {
                setImageViewBitmap(R.id.widget_cover, bitmap)
            } else {
                setImageViewResource(R.id.widget_cover, R.drawable.ic_widget_cover_placeholder)
            }
            setOnClickPendingIntent(R.id.widget_play, broadcast(context, MeloWidgetSmall::class.java, ACTION_TOGGLE))
            setOnClickPendingIntent(R.id.widget_root, openApp(context))
        }

    private fun buildViewsMedium(context: Context, bitmap: Bitmap?): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_medium).apply {
            setTextViewText(R.id.widget_title, title ?: "Ничего не играет")
            setTextViewText(R.id.widget_artist, artist ?: "Melo")
            setImageViewResource(
                R.id.widget_play,
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )
            setImageViewResource(
                R.id.widget_like,
                if (isLiked) R.drawable.ic_widget_like_filled else R.drawable.ic_widget_like_outline,
            )
            if (bitmap != null) {
                setImageViewBitmap(R.id.widget_cover, bitmap)
            } else {
                setImageViewResource(R.id.widget_cover, R.drawable.ic_widget_cover_placeholder)
            }
            setOnClickPendingIntent(R.id.widget_prev, broadcast(context, MeloWidgetMedium::class.java, ACTION_PREV))
            setOnClickPendingIntent(R.id.widget_play, broadcast(context, MeloWidgetMedium::class.java, ACTION_TOGGLE))
            setOnClickPendingIntent(R.id.widget_next, broadcast(context, MeloWidgetMedium::class.java, ACTION_NEXT))
            setOnClickPendingIntent(R.id.widget_like, broadcast(context, MeloWidgetMedium::class.java, ACTION_LIKE))
            setOnClickPendingIntent(R.id.widget_root, openApp(context))
        }

    private fun buildViewsLarge(context: Context, bitmap: Bitmap?): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_large).apply {
            setTextViewText(R.id.widget_title, title ?: "Ничего не играет")
            setTextViewText(R.id.widget_artist, artist ?: "Melo")
            setImageViewResource(
                R.id.widget_play,
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )
            setImageViewResource(
                R.id.widget_like,
                if (isLiked) R.drawable.ic_widget_like_filled else R.drawable.ic_widget_like_outline,
            )
            if (bitmap != null) {
                setImageViewBitmap(R.id.widget_cover, bitmap)
            } else {
                setImageViewResource(R.id.widget_cover, R.drawable.ic_widget_cover_placeholder)
            }
            setOnClickPendingIntent(R.id.widget_shuffle, broadcast(context, MeloWidgetLarge::class.java, ACTION_SHUFFLE))
            setOnClickPendingIntent(R.id.widget_prev, broadcast(context, MeloWidgetLarge::class.java, ACTION_PREV))
            setOnClickPendingIntent(R.id.widget_play, broadcast(context, MeloWidgetLarge::class.java, ACTION_TOGGLE))
            setOnClickPendingIntent(R.id.widget_next, broadcast(context, MeloWidgetLarge::class.java, ACTION_NEXT))
            setOnClickPendingIntent(R.id.widget_like, broadcast(context, MeloWidgetLarge::class.java, ACTION_LIKE))
            setOnClickPendingIntent(R.id.widget_root, openApp(context))
        }

    private fun loadCover(context: Context, url: String) {
        scope.launch {
            val req = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false) // bitmap пойдёт в RemoteViews → нельзя hardware
                .size(240)
                .transformations(RoundedCornersTransformation(28f))
                .build()
            val drawable = runCatching { context.imageLoader.execute(req).drawable }.getOrNull() ?: return@launch
            val bitmap = runCatching { drawable.toBitmap() }.getOrNull() ?: return@launch
            cachedBitmap = bitmap
            cachedUrl = url
            render(context)
        }
    }

    private fun broadcast(context: Context, cls: Class<*>, action: String): PendingIntent {
        val i = Intent(context, cls).setAction(action)
        return PendingIntent.getBroadcast(
            context, action.hashCode() xor cls.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openApp(context: Context): PendingIntent {
        val i = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, 1, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
