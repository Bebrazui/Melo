package com.melo.music.ui

import android.os.Bundle
import androidx.compose.runtime.saveable.Saver
import com.melo.music.extractor.ItemKind
import com.melo.music.extractor.Source
import com.melo.music.extractor.TrackItem

object TrackSaver {

    fun listOfSaver(): Saver<List<TrackItem>, Bundle> {
        return Saver(
            save = { list ->
                Bundle().apply {
                    putStringArrayList("urls", ArrayList(list.map { it.url }))
                    putStringArrayList("titles", ArrayList(list.map { it.title }))
                    putStringArrayList("uploaders", ArrayList(list.map { it.uploader ?: "" }))
                    putLongArray("durations", list.map { it.durationSeconds }.toLongArray())
                    putStringArrayList("thumbnails", ArrayList(list.map { it.thumbnailUrl ?: "" }))
                    putStringArrayList("sources", ArrayList(list.map { it.source.name }))
                }
            },
            restore = { bundle ->
                val urls = bundle.getStringArrayList("urls") ?: arrayListOf()
                val titles = bundle.getStringArrayList("titles") ?: arrayListOf()
                val uploaders = bundle.getStringArrayList("uploaders") ?: arrayListOf()
                val durations = bundle.getLongArray("durations") ?: longArrayOf()
                val thumbnails = bundle.getStringArrayList("thumbnails") ?: arrayListOf()
                val sources = bundle.getStringArrayList("sources") ?: arrayListOf()
                urls.mapIndexed { i, url ->
                    TrackItem(
                        title = titles.getOrElse(i) { "" },
                        uploader = uploaders.getOrElse(i) { "" }.ifBlank { null },
                        url = url,
                        durationSeconds = durations.getOrElse(i) { 0L },
                        thumbnailUrl = thumbnails.getOrElse(i) { "" }.ifBlank { null },
                        source = runCatching { Source.valueOf(sources.getOrElse(i) { "YOUTUBE_MUSIC" }) }
                            .getOrDefault(Source.YOUTUBE_MUSIC),
                        kind = ItemKind.TRACK,
                    )
                }
            },
        )
    }

    fun singleSaver(): Saver<TrackItem?, Bundle> {
        return Saver(
            save = { item ->
                Bundle().apply {
                    item?.let {
                        putString("title", it.title)
                        putString("uploader", it.uploader ?: "")
                        putString("url", it.url)
                        putLong("duration", it.durationSeconds)
                        putString("thumbnail", it.thumbnailUrl ?: "")
                        putString("source", it.source.name)
                    }
                }
            },
            restore = { bundle ->
                val url = bundle.getString("url") ?: return@Saver null
                TrackItem(
                    title = bundle.getString("title") ?: "",
                    uploader = bundle.getString("uploader")?.ifBlank { null },
                    url = url,
                    durationSeconds = bundle.getLong("duration", 0L),
                    thumbnailUrl = bundle.getString("thumbnail")?.ifBlank { null },
                    source = runCatching { Source.valueOf(bundle.getString("source") ?: "YOUTUBE_MUSIC") }
                        .getOrDefault(Source.YOUTUBE_MUSIC),
                    kind = ItemKind.TRACK,
                )
            },
        )
    }
}
