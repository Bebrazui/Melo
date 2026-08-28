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
     * Каждая полка — это пара (заголовок, список треков).
     */
    suspend fun generatePersonalizedShelves(
        onLoadShelf: suspend (String) -> List<TrackItem>,
        related: suspend (TrackItem) -> List<TrackItem> = { emptyList() },
    ): List<Pair<String, List<TrackItem>>> = withContext(Dispatchers.IO) {
        val profile = TasteProfile.build()
        val liked = FavoritesManager.getAll()
        val history = HistoryManager.getAll()
        val allUserTracks = (liked + history).distinctBy { it.url }
        val heardUrls = history.map { it.url }.toSet()
        val likedUrls = liked.map { it.url }.toSet()
        val bannedUrls = SkipTracker.getBannedUrls()

        val shelves = mutableListOf<Pair<String, List<TrackItem>>>()
        val usedSeeds = mutableSetOf<String>()

        // 1) «Похоже на <артист>» — НАСТОЯЩИЕ рекомендации (радио YouTube Music)
        //    от любимого/прослушанного трека этого артиста.
        for ((artist, _) in profile.topArtists) {
            if (shelves.size >= 4) break
            val seed = allUserTracks.firstOrNull {
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
            if (filtered.size >= 3) {
                val artistTitle = seed.uploader?.removeSuffix(" - Topic")?.removeSuffix(" - topic") ?: artist
                shelves.add("Похоже на $artistTitle" to filtered)
            }
        }

        // 2) Полки по ключевым словам и сочетаниям пользователя
        if (shelves.size < 4 && profile.topKeywords.isNotEmpty()) {
            val seeds = TasteProfile.generateShelfSeeds(profile)
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

        // 3) Если данных о вкусе ещё мало — отдаём нейтральные качественные подборки
        if (shelves.isEmpty()) {
            val neutralSeeds = listOf("популярные новинки музыки", "electronic synthwave chill", "rock indie alternative")
            for (seed in neutralSeeds) {
                val raw = runCatching { onLoadShelf(seed) }.getOrDefault(emptyList())
                val filtered = raw.filter { it.url !in bannedUrls }.take(10)
                if (filtered.isNotEmpty()) shelves.add(seedToTitle(seed) to filtered)
            }
        }

        shelves
    }

    /**
     * Формирует умный персональный микс для «Быстрого выбора» (Quick Picks):
     * сочетание треков из истории/лайков + связанные с ними свежие находки.
     */
    suspend fun generateQuickPicks(
        fallbackTracks: List<TrackItem> = emptyList(),
        related: suspend (TrackItem) -> List<TrackItem> = { emptyList() },
    ): List<TrackItem> = withContext(Dispatchers.IO) {
        val liked = FavoritesManager.getAll()
        val history = HistoryManager.getAll()
        val bannedUrls = SkipTracker.getBannedUrls()
        val profile = TasteProfile.build()

        val validUserTracks = (liked + history)
            .filter { it.url !in bannedUrls }
            .distinctBy { it.url }

        if (validUserTracks.isEmpty()) {
            return@withContext fallbackTracks.take(12)
        }

        val pool = mutableListOf<TrackItem>()
        // Добавляем любимые треки пользователя
        pool.addAll(validUserTracks.take(8))

        // Подмешиваем 4-6 похожих треков из радио
        val seed = validUserTracks.shuffled().firstOrNull()
        if (seed != null) {
            val rel = runCatching { related(seed) }.getOrDefault(emptyList())
            val fresh = rel.filter { it.kind == ItemKind.TRACK && it.url !in bannedUrls && it.url !in validUserTracks.map { u -> u.url } }
            pool.addAll(fresh.take(4))
        }

        TasteProfile.rankByTaste(pool.distinctBy { it.url }, profile).take(12)
    }

    /**
     * Формирует персональную ленту «Рекомендуем» на основе радио похожих треков к вкусу пользователя.
     */
    suspend fun generatePersonalizedRecommendations(
        fallbackProvider: suspend () -> List<TrackItem>,
        related: suspend (TrackItem) -> List<TrackItem>,
    ): List<TrackItem> = withContext(Dispatchers.IO) {
        val liked = FavoritesManager.getAll()
        val history = HistoryManager.getAll()
        val bannedUrls = SkipTracker.getBannedUrls()
        val profile = TasteProfile.build()

        val validSeeds = (liked + history).filter { it.url !in bannedUrls }.distinctBy { it.url }
        if (validSeeds.isEmpty()) {
            return@withContext fallbackProvider()
        }

        val results = mutableListOf<TrackItem>()
        val excludeUrls = (bannedUrls + validSeeds.map { it.url }).toMutableSet()

        // Берём до 3 разных треков пользователя и строим от них радио-поток
        for (seed in validSeeds.shuffled().take(3)) {
            val rel = runCatching { related(seed) }.getOrDefault(emptyList())
            val fresh = rel.filter { it.kind == ItemKind.TRACK && it.url !in excludeUrls }
            results.addAll(fresh)
            excludeUrls.addAll(fresh.map { it.url })
        }

        if (results.size < 6) {
            val fallback = runCatching { fallbackProvider() }.getOrDefault(emptyList())
            results.addAll(fallback.filter { it.url !in excludeUrls })
        }

        TasteProfile.rankByTaste(results.distinctBy { it.url }, profile)
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
            .replace("лучшие песни", "Лучшее")
            .replace("музыка", "Музыка")
            .replace("хиты", "Хиты")
            .trim()
            .replaceFirstChar { it.uppercase() }
    }
}
