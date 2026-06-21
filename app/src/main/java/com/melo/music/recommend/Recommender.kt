package com.melo.music.recommend

import android.content.Context
import com.melo.music.extractor.ItemKind
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
        related: suspend (TrackItem) -> List<TrackItem> = { emptyList() },
    ): List<Pair<String, List<TrackItem>>> = withContext(Dispatchers.IO) {
        val profile = TasteProfile.build()
        val liked = FavoritesManager.getAll()
        val heardUrls = HistoryManager.getAll().map { it.url }.toSet()
        val likedUrls = liked.map { it.url }.toSet()
        val bannedUrls = SkipTracker.getBannedUrls()

        val shelves = mutableListOf<Pair<String, List<TrackItem>>>()
        val usedSeeds = mutableSetOf<String>()

        // 1) «Похоже на <артист>» — НАСТОЯЩИЕ рекомендации (радио YouTube Music)
        //    от лайкнутого трека этого артиста, а не текстовый поиск по имени.
        for ((artist, _) in profile.topArtists) {
            if (shelves.size >= 3) break
            val seed = liked.firstOrNull {
                val a = it.uploader?.lowercase()?.removeSuffix(" - topic")?.trim()
                a != null && (a == artist || a.contains(artist)) && it.url !in usedSeeds
            } ?: continue
            usedSeeds.add(seed.url)
            val raw = runCatching { related(seed) }.getOrDefault(emptyList())
            val filtered = raw
                .filter {
                    it.kind == ItemKind.TRACK &&
                        it.url !in heardUrls && it.url !in likedUrls && it.url !in bannedUrls
                }
                .distinctBy { it.url }
                .let { TasteProfile.rankByTaste(it, profile) }
                .take(12)
            if (filtered.size >= 4) {
                shelves.add("Похоже на ${seed.uploader ?: artist}" to filtered)
            }
        }

        // 2) Жанровые полки через поиск (без сломанных «похожих хитов») — для разнообразия.
        if (shelves.size < 4) {
            val seeds = TasteProfile.generateShelfSeeds(profile)
                .filterNot { it.contains("похож") }
            for (seed in seeds) {
                if (shelves.size >= 4) break
                val raw = runCatching { onLoadShelf(seed) }.getOrDefault(emptyList())
                val filtered = raw
                    .filter { it.url !in heardUrls && it.url !in likedUrls && it.url !in bannedUrls }
                    .let { TasteProfile.rankByTaste(it, profile) }
                    .take(10)
                if (filtered.isNotEmpty()) shelves.add(seedToTitle(seed) to filtered)
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
