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

/** Виджет «сейчас играет» на главном экране: обложка, название, управление. */
class MeloWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        WidgetUpdater.render(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetUpdater.ACTION_TOGGLE -> WidgetUpdater.sendKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            WidgetUpdater.ACTION_NEXT -> WidgetUpdater.sendKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
            WidgetUpdater.ACTION_PREV -> WidgetUpdater.sendKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }
}

/**
 * Состояние виджета + отрисовка. Источник данных — UI (текущий трек) и сервис
 * (play/pause). Обложку грузим через тот же ImageLoader (ByeDPI + уменьшение).
 */
object WidgetUpdater {
    const val ACTION_TOGGLE = "com.melo.music.widget.TOGGLE"
    const val ACTION_NEXT = "com.melo.music.widget.NEXT"
    const val ACTION_PREV = "com.melo.music.widget.PREV"

    @Volatile private var title: String? = null
    @Volatile private var artist: String? = null
    @Volatile private var coverUrl: String? = null
    @Volatile private var isPlaying = false

    @Volatile private var cachedBitmap: Bitmap? = null
    @Volatile private var cachedUrl: String? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun setNowPlaying(context: Context, title: String?, artist: String?, coverUrl: String?, isPlaying: Boolean) {
        this.title = title
        this.artist = artist
        this.coverUrl = coverUrl
        this.isPlaying = isPlaying
        render(context)
    }

    fun setPlaying(context: Context, playing: Boolean) {
        isPlaying = playing
        render(context)
    }

    /** Отправляет медиа-кнопку в PlaybackService (он уже обрабатывает onMediaButtonEvent). */
    fun sendKey(context: Context, code: Int) {
        runCatching {
            val i = Intent(Intent.ACTION_MEDIA_BUTTON).setClass(context, PlaybackService::class.java)
            i.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, code))
            context.startService(i)
        }
    }

    fun render(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, MeloWidget::class.java))
        if (ids.isEmpty()) return
        val bmp = coverUrl?.takeIf { it == cachedUrl }?.let { cachedBitmap }
        mgr.updateAppWidget(ids, buildViews(context, bmp))
        if (coverUrl != null && coverUrl != cachedUrl) loadCover(context, coverUrl!!)
    }

    private fun buildViews(context: Context, bitmap: Bitmap?): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_player).apply {
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
            setOnClickPendingIntent(R.id.widget_play, broadcast(context, ACTION_TOGGLE))
            setOnClickPendingIntent(R.id.widget_next, broadcast(context, ACTION_NEXT))
            setOnClickPendingIntent(R.id.widget_prev, broadcast(context, ACTION_PREV))
            setOnClickPendingIntent(R.id.widget_root, openApp(context))
        }

    private fun loadCover(context: Context, url: String) {
        scope.launch {
            val req = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false) // bitmap пойдёт в RemoteViews → нельзя hardware
                .size(160)
                .transformations(RoundedCornersTransformation(24f))
                .build()
            val drawable = runCatching { context.imageLoader.execute(req).drawable }.getOrNull() ?: return@launch
            val bitmap = runCatching { drawable.toBitmap() }.getOrNull() ?: return@launch
            cachedBitmap = bitmap
            cachedUrl = url
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, MeloWidget::class.java))
            if (ids.isNotEmpty()) mgr.updateAppWidget(ids, buildViews(context, bitmap))
        }
    }

    private fun broadcast(context: Context, action: String): PendingIntent {
        val i = Intent(context, MeloWidget::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context, action.hashCode(), i,
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
