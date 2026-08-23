package com.melo.music.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Загрузчик обложек для уведомления Media3.
 *
 * Media3 по умолчанию тянет artwork своим внутренним загрузчиком БЕЗ нашего
 * ByeDPI-прокси, поэтому хосты SoundCloud/YouTube (sndcdn, googleusercontent)
 * недоступны и в уведомлении остаётся иконка приложения. Грузим через Coil —
 * тот же проксированный ImageLoader, что и для обложек в списках (даунскейл + ретраи),
 * и отдаём готовый Bitmap в уведомление.
 */
class CoilBitmapLoader(private val context: Context) : BitmapLoader {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            try {
                android.util.Log.e("MeloArt", "loadBitmap START $uri")
                // Синглтон из MeloApp.newImageLoader(): ByeDPI-прокси, UA, ретраи, кэш.
                val loader = context.applicationContext.imageLoader
                val request = ImageRequest.Builder(context.applicationContext)
                    .data(uri)
                    .size(512, 512)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                val drawable = (result as? SuccessResult)?.drawable
                if (drawable != null) {
                    android.util.Log.e("MeloArt", "loadBitmap OK ${drawable.intrinsicWidth}x${drawable.intrinsicHeight}")
                    future.set(drawable.toBitmap())
                } else {
                    android.util.Log.e("MeloArt", "loadBitmap EMPTY for $uri (${result::class.java.simpleName})")
                    future.setException(Exception("coil empty result for $uri"))
                }
            } catch (e: Exception) {
                android.util.Log.e("MeloArt", "loadBitmap FAIL $uri: ${e.javaClass.simpleName}: ${e.message}")
                future.setException(e)
            }
        }
        return future
    }

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap> {
        android.util.Log.e("MeloArt", "loadBitmapFromMetadata uri=${metadata.artworkUri} dataLen=${metadata.artworkData?.size}")
        metadata.artworkUri?.let { return loadBitmap(it) }
        val data = metadata.artworkData
        if (data != null) {
            val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
            if (bmp != null) return Futures.immediateFuture(bmp)
        }
        return Futures.immediateFailedFuture(IllegalStateException("no artwork in metadata"))
    }

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
            ?: throw IllegalArgumentException("cannot decode bitmap from ${data.size} bytes")
        return Futures.immediateFuture(bmp)
    }
}
