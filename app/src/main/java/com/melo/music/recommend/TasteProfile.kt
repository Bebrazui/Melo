package com.melo.music.recommend

import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem
import com.melo.music.favorites.FavoritesManager

/**
 * Профиль вкуса: анализирует лайки и историю, извлекает ключевые слова,
 * топ-артистов и предпочтения по источникам.
 * Используется для генерации персонализированных shelf-запросов.
 */
object TasteProfile {

    data class Profile(
        val topArtists: List<Pair<String, Int>>,
        val topKeywords: List<Pair<String, Int>>,
        val sourceDistribution: Map<Source, Int>,
        val avgDurationSec: Long,
    )

    /**
     * Строит профиль вкуса из текущих лайков и истории прослушиваний.
     */
    fun build(): Profile {
        val liked = FavoritesManager.getAll()
        val history = com.melo.music.history.HistoryManager.getAll()
        val bannedUrls = SkipTracker.getBannedUrls()

        val validLiked = liked.filter { it.url !in bannedUrls }
        val validHistory = history.filter { it.url !in bannedUrls }

        if (validLiked.isEmpty() && validHistory.isEmpty()) return emptyProfile()

        // --- Артисты с весами (лайк = 3 балла, история = 1 балл) ---
        val artistFreq = mutableMapOf<String, Int>()
        fun addArtist(raw: String?, weight: Int) {
            val artist = raw
                ?.removeSuffix(" - Topic")
                ?.removeSuffix(" - topic")
                ?.trim()
                ?.lowercase()
                ?: return
            if (artist.length < 2) return
            artistFreq[artist] = (artistFreq[artist] ?: 0) + weight
        }

        for (t in validLiked) addArtist(t.uploader, 3)
        for (t in validHistory) addArtist(t.uploader, 1)

        val topArtists = artistFreq.entries
            .sortedByDescending { it.value }
            .take(12)
            .map { it.key to it.value }

        // --- Ключевые слова из названий треков ---
        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "is", "it", "that", "this", "was", "are",
            "be", "has", "had", "have", "not", "no", "you", "i", "my", "me",
            "we", "us", "our", "his", "her", "she", "he", "they", "them",
            "official", "video", "audio", "remix", "feat", "ft", "live",
            "clip", "hd", "4k", "mono", "stereo", "music", "музыка", "трек", "песня",
        )
        val keywordFreq = mutableMapOf<String, Int>()
        fun addKeywords(title: String, weight: Int) {
            val words = title.lowercase()
                .replace(Regex("[^a-zA-Zа-яёА-ЯЁ0-9\\s]"), " ")
                .split("\\s+".toRegex())
                .filter { it.length >= 3 && it !in stopWords }
            for (w in words) {
                keywordFreq[w] = (keywordFreq[w] ?: 0) + weight
            }
        }

        for (t in validLiked) addKeywords(t.title, 3)
        for (t in validHistory) addKeywords(t.title, 1)

        val topKeywords = keywordFreq.entries
            .sortedByDescending { it.value }
            .take(15)
            .map { it.key to it.value }

        // --- Источники ---
        val allTracks = (validLiked + validHistory).distinctBy { it.url }
        val sourceDist = allTracks.groupBy { it.source }.mapValues { it.value.size }

        // --- Средняя длительность ---
        val avgDur = allTracks.filter { it.durationSeconds > 0 }
            .map { it.durationSeconds }
            .average()
            .let { if (it.isNaN()) 0L else it.toLong() }

        return Profile(topArtists, topKeywords, sourceDist, avgDur)
    }

    /**
     * Генерирует shelf-запросы на основе профиля.
     * Возвращает список seed-строк для NewPipeResolver.shelf().
     */
    fun generateShelfSeeds(profile: Profile = build()): List<String> {
        val seeds = mutableListOf<String>()

        // 1) По топ-артистам пользователя: "$artist треки"
        for ((artist, _) in profile.topArtists.take(4)) {
            seeds.add("$artist лучшие песни")
        }

        // 2) По ключевым словам и сочетаниям пользователя
        val kw = profile.topKeywords.take(4).map { it.first }
        if (kw.isNotEmpty()) {
            seeds.add("${kw.take(2).joinToString(" ")} music")
        }

        return seeds.distinct().take(6)
    }

    /**
     * Ранжирует треки по близости к профилю вкуса.
     * Возвращает треки, отсортированные по релевантности.
     */
    fun rankByTaste(
        tracks: List<TrackItem>,
        profile: Profile = build(),
    ): List<TrackItem> {
        if (profile.topArtists.isEmpty() && profile.topKeywords.isEmpty()) return tracks
        return tracks.sortedByDescending { scoreTasteAffinity(it, profile) }
    }

    /**
     * Фильтрует треки, исключая уже прослушанные и забаненные артистов.
     */
    fun filterExcluded(
        tracks: List<TrackItem>,
        listenedUrls: Set<String>,
        bannedArtists: Set<String>,
    ): List<TrackItem> {
        val bannedLower = bannedArtists.map { it.lowercase() }.toSet()
        return tracks.filter { t ->
            t.url !in listenedUrls &&
                bannedLower.none { banned ->
                    t.uploader?.lowercase()?.contains(banned) == true
                }
        }
    }

    private fun scoreTasteAffinity(track: TrackItem, profile: Profile): Double {
        var score = 0.0

        // Совпадение артиста — сильный сигнал.
        val trackArtist = track.uploader?.lowercase() ?: ""
        for ((artist, freq) in profile.topArtists) {
            if (trackArtist.contains(artist) || artist.contains(trackArtist)) {
                score += 10.0 * freq
                break
            }
        }

        // Совпадение ключевых слов в названии.
        val titleWords = track.title.lowercase().split("\\s+".toRegex()).toSet()
        for ((kw, freq) in profile.topKeywords) {
            if (kw in titleWords) {
                score += 2.0 * freq
            }
        }

        // Бонус за предпочтительный источник.
        val sourceCount = profile.sourceDistribution[track.source] ?: 0
        val maxSource = profile.sourceDistribution.values.maxOrNull() ?: 1
        score += (sourceCount.toDouble() / maxSource) * 3.0

        return score
    }

    private fun emptyProfile() = Profile(
        topArtists = emptyList(),
        topKeywords = emptyList(),
        sourceDistribution = emptyMap(),
        avgDurationSec = 0,
    )
}
