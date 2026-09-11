package com.melo.desktop.extractor

/** Источник трека. */
enum class Source {
    YOUTUBE_MUSIC,
    SOUNDCLOUD,
    BANDCAMP,
    LOCAL
}

/** Тип элемента: трек, артист или альбом. */
enum class ItemKind {
    TRACK,
    ARTIST,
    ALBUM
}

/** Элемент каталога (поиск, рекомендации, плейлист). */
data class TrackItem(
    val title: String,
    val uploader: String? = null,
    val url: String,
    val durationSeconds: Long = 0,
    val thumbnailUrl: String? = null,
    val source: Source = Source.YOUTUBE_MUSIC,
    val kind: ItemKind = ItemKind.TRACK,
    val speed: Float = 1f,
    val viewCount: Long = 0,
)

/** Результат резолва: прямой поток воспроизведения. */
data class ResolvedTrack(
    val title: String,
    val audioUrl: String,
    val thumbnailUrl: String? = null,
    val artist: String? = null,
    val videoUrl: String? = null,
    val source: Source = Source.YOUTUBE_MUSIC,
    val originalUrl: String = "",
    val durationSeconds: Long = 0L,
)

/** Статистика трека на YouTube. */
data class TrackStats(
    val viewCount: Long,
    val likeCount: Long,
    val commentsCount: Long,
)

/** Комментарий к треку. */
data class TrackComment(
    val author: String,
    val text: String,
    val likeCount: Int,
    val dateText: String?,
    val authorAvatar: String?,
)
