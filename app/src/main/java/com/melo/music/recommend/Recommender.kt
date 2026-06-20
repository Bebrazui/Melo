package com.melo.music.recommend

import android.content.Context
import com.melo.music.extractor.TrackItem
import com.melo.music.favorites.FavoritesManager
import com.melo.music.history.HistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Рекомендательный движок: генерирует персонализированные shelf-запросы
 * на основе Taste Profile + Skip-aware фильтрации.
 */
object Recommender {

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        SkipTracker.init(context)
        FavoritesManager.init(context)
        HistoryManager.init(context)
        initialized = true
    }

    /**
     * Генерирует список персонализированных полок для главного экрана.
     * Каждая полка — это пара (заголовок, seed-запрос для shelf()).
     */
    suspend fun generatePersonalizedShelves(
        onLoadShelf: suspend (String) -> List<TrackItem>,
    ): List<Pair<String, List<TrackItem>>> = withContext(Dispatchers.IO) {
        val profile = TasteProfile.build()
        val seeds = TasteProfile.generateShelfSeeds(profile)

        if (seeds.isEmpty()) return@withContext emptyList()

        val heardUrls = HistoryManager.getAll().map { it.url }.toSet()
        val likedUrls = FavoritesManager.getAll().map { it.url }.toSet()
        val bannedUrls = SkipTracker.getBannedUrls()

        val shelves = mutableListOf<Pair<String, List<TrackItem>>>()

        for (seed in seeds.take(4)) {
            val raw = runCatching { onLoadShelf(seed) }.getOrDefault(emptyList())
            val filtered = raw
                .filter { it.url !in heardUrls && it.url !in likedUrls && it.url !in bannedUrls }
                .let { TasteProfile.rankByTaste(it, profile) }
                .take(10)

            if (filtered.isNotEmpty()) {
                val title = seedToTitle(seed)
                shelves.add(title to filtered)
            }
        }

        shelves
    }

    /**
     * Фильтрует результаты поиска: исключает забаненные треки,
     * сортирует по Taste Profile.
     */
    fun filterSearchResults(
        tracks: List<TrackItem>,
        profile: com.melo.music.recommend.TasteProfile.Profile = TasteProfile.build(),
    ): List<TrackItem> {
        val bannedUrls = SkipTracker.getBannedUrls()
        val likedUrls = FavoritesManager.getAll().map { it.url }.toSet()

        return tracks
            .filter { it.url !in bannedUrls }
            .let { TasteProfile.rankByTaste(it, profile) }
    }

    /**
     * Получить статистику skip-aware для UI.
     */
    fun getStats(): SkipStats {
        val profile = TasteProfile.build()
        return SkipStats(
            totalLikes = FavoritesManager.getAll().size,
            totalListens = HistoryManager.getAll().size,
            topArtists = profile.topArtists.take(5).map { it.first },
            topKeywords = profile.topKeywords.take(5).map { it.first },
        )
    }

    data class SkipStats(
        val totalLikes: Int,
        val totalListens: Int,
        val topArtists: List<String>,
        val topKeywords: List<String>,
    )

    private fun seedToTitle(seed: String): String {
        return seed
            .replace("похожие хиты", "Похоже на")
            .replace("музыка", "Музыка")
            .replace("хиты", "Хиты")
            .trim()
            .replaceFirstChar { it.uppercase() }
    }
}
