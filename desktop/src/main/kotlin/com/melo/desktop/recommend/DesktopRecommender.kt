package com.melo.desktop.recommend

import com.melo.desktop.extractor.DesktopExtractor
import com.melo.desktop.extractor.ItemKind
import com.melo.desktop.extractor.TrackItem
import com.melo.desktop.storage.DesktopStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TasteProfile(
    val topArtists: List<Pair<String, Int>>,
    val totalTracks: Int,
)

/**
 * Рекомендательный алгоритм Melo Desktop:
 * Генерация полок на основе предпочтений и режим бесконечной «Моей волны».
 */
object DesktopRecommender {

    fun buildProfile(): TasteProfile {
        val liked = DesktopStorage.favorites
        val history = DesktopStorage.history
        val all = (liked + history).distinctBy { it.url }

        val artistCounts = mutableMapOf<String, Int>()
        for (item in all) {
            val u = item.uploader?.lowercase()?.removeSuffix(" - topic")?.trim() ?: continue
            if (u.isBlank()) continue
            val weight = if (liked.any { it.url == item.url }) 3 else 1
            artistCounts[u] = (artistCounts[u] ?: 0) + weight
        }

        val top = artistCounts.toList().sortedByDescending { it.second }.take(10)
        return TasteProfile(top, all.size)
    }

    /** Генерирует персональные полки для главной страницы. */
    suspend fun generateShelves(): List<Pair<String, List<TrackItem>>> = withContext(Dispatchers.IO) {
        val profile = buildProfile()
        val shelves = mutableListOf<Pair<String, List<TrackItem>>>()

        // 1) Полка популярных новинок
        val trending = runCatching { DesktopExtractor.recommendations() }.getOrDefault(emptyList())
        if (trending.isNotEmpty()) {
            shelves.add("В тренде" to trending.take(15))
        }

        // 2) Полки по любимым артистам пользователя
        for ((artist, _) in profile.topArtists.take(3)) {
            val shelfName = "Похоже на $artist"
            val tracks = runCatching { DesktopExtractor.shelf(artist) }.getOrDefault(emptyList())
            if (tracks.isNotEmpty()) {
                shelves.add(shelfName to tracks.filter { it.kind == ItemKind.TRACK }.take(15))
            }
        }

        // 3) Жанровые / настроенческие полки
        val moods = listOf("Хиты недели", "Электронная музыка", "Спокойная музыка")
        for (mood in moods) {
            if (shelves.size >= 5) break
            val tracks = runCatching { DesktopExtractor.shelf(mood) }.getOrDefault(emptyList())
            if (tracks.isNotEmpty()) {
                shelves.add(mood to tracks.filter { it.kind == ItemKind.TRACK }.take(15))
            }
        }

        shelves
    }

    /** Возвращает следующий пакет треков для бесконечной «Моей волны». */
    suspend fun getNextWaveTracks(currentSeed: TrackItem?): List<TrackItem> = withContext(Dispatchers.IO) {
        val seed = currentSeed
            ?: DesktopStorage.favorites.firstOrNull()
            ?: DesktopStorage.history.firstOrNull()
            ?: DesktopExtractor.recommendations().firstOrNull()
            ?: return@withContext emptyList()

        DesktopExtractor.relatedTracks(seed).filter { it.kind == ItemKind.TRACK }
    }
}
