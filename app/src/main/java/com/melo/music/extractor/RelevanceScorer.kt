package com.melo.music.extractor

/**
 * Релевантность поискового результата.
 * Совпадение слов (30%) + точное/префикс-совпадение (20%) + Левенштейн (20%) +
 * триграммы (20%) + популярность/длительность (10%).
 */
object RelevanceScorer {

    fun score(query: String, item: TrackItem): Double {
        val titleLower = item.title.lowercase()
        val uploaderLower = item.uploader?.lowercase() ?: ""
        val target = if (uploaderLower.isNotBlank()) "$titleLower $uploaderLower" else titleLower
        val q = query.lowercase().trim()
        if (q.isBlank() || target.isBlank()) return 0.0

        val qWords = q.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val tWords = target.split("\\s+".toRegex()).filter { it.isNotEmpty() }

        val wordScore = wordOverlap(qWords, tWords)
        val exactBonus = exactPrefixBonus(qWords, tWords)
        val levScore = levenshteinNorm(q, target)
        val triScore = trigramSim(q, target)
        val popScore = popularityProxy(item)

        return wordScore * 0.30 + exactBonus * 0.20 + levScore * 0.20 + triScore * 0.20 + popScore * 0.10
    }

    /** Доля слов запроса, которые есть в целевой строке (с учётом подстроки). */
    private fun wordOverlap(queryWords: List<String>, targetWords: List<String>): Double {
        if (queryWords.isEmpty()) return 0.0
        val targetJoined = targetWords.joinToString(" ")
        val matched = queryWords.count { qw ->
            targetWords.any { tw -> tw.contains(qw) || qw.contains(tw) } ||
                targetJoined.contains(qw)
        }
        return matched.toDouble() / queryWords.size
    }

    /**
     * Бонус за точное совпадение или начало слова.
     * "wet" в "wet leg" → высокий бонус (wet = начало слова).
     * "wet" в "wetten"  → высокий бонус.
     * "wet" в "flower" → 0.
     */
    private fun exactPrefixBonus(queryWords: List<String>, targetWords: List<String>): Double {
        if (queryWords.isEmpty()) return 0.0
        var total = 0.0
        for (qw in queryWords) {
            val exactMatch = targetWords.any { it == qw }
            val prefixMatch = targetWords.any { it.startsWith(qw) }
            val contained = targetWords.any { it.contains(qw) }
            total += when {
                exactMatch -> 1.0
                prefixMatch -> 0.8
                contained -> 0.4
                else -> 0.0
            }
        }
        return total / queryWords.size
    }

    /** Нормализованное расстояние Левенштейна (1.0 = идентичны, 0.0 =完全不同). */
    private fun levenshteinNorm(a: String, b: String): Double {
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1.0
        val dist = levenshtein(a, b)
        return 1.0 - dist.toDouble() / maxLen
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = IntArray(n + 1) { it }
        for (i in 1..m) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..n) {
                val temp = dp[j]
                dp[j] = if (a[i - 1] == b[j - 1]) prev
                else minOf(prev, dp[j], dp[j - 1]) + 1
                prev = temp
            }
        }
        return dp[n]
    }

    /** Jaccard-сходство триграмм. */
    private fun trigramSim(a: String, b: String): Double {
        val aTris = trigrams(a)
        val bTris = trigrams(b)
        if (aTris.isEmpty() && bTris.isEmpty()) return 1.0
        if (aTris.isEmpty() || bTris.isEmpty()) return 0.0
        val intersection = aTris.intersect(bTris).size
        val union = aTris.union(bTris).size
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }

    private fun trigrams(s: String): Set<String> {
        if (s.length < 3) return setOf("  $s")
        val padded = "  $s "
        return (0..padded.length - 3).map { padded.substring(it, it + 3) }.toSet()
    }

    /**
     * Прокси популярности: длительность трека.
     * 2–6 минут (типичный трек) → 1.0.
     * Короткие (<30с) и длинные (>15мин) → ниже.
     */
    private fun popularityProxy(item: TrackItem): Double {
        val dur = item.durationSeconds
        if (dur <= 0) return 0.5
        return when {
            dur in 30..180 -> 0.7
            dur in 180..360 -> 1.0
            dur in 360..600 -> 0.8
            dur in 600..900 -> 0.6
            else -> 0.3
        }
    }

    /** Сортирует треки по релевантности относительно запроса. */
    fun rank(query: String, items: List<TrackItem>): List<TrackItem> =
        items.sortedByDescending { score(query, it) }
}
