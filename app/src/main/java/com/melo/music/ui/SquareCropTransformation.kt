package com.melo.music.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import coil.intercept.Interceptor
import coil.request.ImageResult
import coil.size.Size
import coil.transform.Transformation

/**
 * Умная обрезка для YouTube/YouTube Music обложек:
 * 1. Если картинка 4:3 с черными полосами (hqdefault.jpg 480x360),
 *    вырезает полезную 16:9 область (без черных полос 45px сверху/снизу),
 *    а затем берет центральный квадрат 1:1 (без боковых тематических плашек).
 * 2. Если картинка 16:9 (1280x720, 640x360),
 *    вырезает центральный квадрат 1:1 (где находится реальная обложка альбома).
 * 3. Если картинка уже квадратная (1:1) — оставляет без изменений.
 */
class SquareCropTransformation : Transformation {
    override val cacheKey: String = "com.melo.music.ui.SquareCropTransformation"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val w = input.width
        val h = input.height
        if (w <= 0 || h <= 0) return input

        val aspect = w.toFloat() / h.toFloat()

        // 1. YouTube hqdefault.jpg (4:3 соотношение ~1.33) -> имеет черные полосы сверху/снизу по 12.5%
        if (aspect in 1.30f..1.38f) {
            val topBlack = (h * 0.125f).toInt()
            val activeHeight = h - topBlack * 2
            val cropSize = activeHeight
            val left = (w - cropSize) / 2

            val srcRect = Rect(left, topBlack, left + cropSize, topBlack + cropSize)
            val outBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outBitmap)
            val dstRect = Rect(0, 0, cropSize, cropSize)
            canvas.drawBitmap(input, srcRect, dstRect, null)
            return outBitmap
        }

        // 2. YouTube 16:9 (~1.777) -> квадратная обложка альбома находится строго по центру (1:1)
        if (aspect in 1.70f..1.85f) {
            val cropSize = h
            val left = (w - cropSize) / 2
            val srcRect = Rect(left, 0, left + cropSize, h)
            val outBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outBitmap)
            val dstRect = Rect(0, 0, cropSize, cropSize)
            canvas.drawBitmap(input, srcRect, dstRect, null)
            return outBitmap
        }

        // 3. Другие не-квадратные соотношения -> центрированный квадрат
        if (Math.abs(aspect - 1.0f) > 0.05f) {
            val cropSize = minOf(w, h)
            val left = (w - cropSize) / 2
            val top = (h - cropSize) / 2
            val srcRect = Rect(left, top, left + cropSize, top + cropSize)
            val outBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outBitmap)
            val dstRect = Rect(0, 0, cropSize, cropSize)
            canvas.drawBitmap(input, srcRect, dstRect, null)
            return outBitmap
        }

        return input
    }
}

/**
 * Coil-интерцептор: автоматически подключает SquareCropTransformation ко всем запросам изображений.
 */
class SquareCropInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val newRequest = request.newBuilder()
            .transformations(listOf(SquareCropTransformation()) + request.transformations)
            .build()
        return chain.proceed(newRequest)
    }
}
