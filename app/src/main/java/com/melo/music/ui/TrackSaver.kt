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
                    putFloatArray("speeds", list.map { it.speed }.toFloatArray())
                    putBooleanArray("bassBoosts", list.map { it.bassBoost }.toBooleanArray())
                    putFloatArray("crushes", list.map { it.crush }.toFloatArray())
                    putFloatArray("pitchSemis", list.map { it.pitchSemi }.toFloatArray())
                    putLongArray("noteShiftSeeds", list.map { it.noteShiftSeed }.toLongArray())
                }
            },
            restore = { bundle ->
                val urls = bundle.getStringArrayList("urls") ?: arrayListOf()
                val titles = bundle.getStringArrayList("titles") ?: arrayListOf()
                val uploaders = bundle.getStringArrayList("uploaders") ?: arrayListOf()
                val durations = bundle.getLongArray("durations") ?: longArrayOf()
                val thumbnails = bundle.getStringArrayList("thumbnails") ?: arrayListOf()
                val sources = bundle.getStringArrayList("sources") ?: arrayListOf()
                val speeds = bundle.getFloatArray("speeds") ?: floatArrayOf()
                val bassBoosts = bundle.getBooleanArray("bassBoosts") ?: booleanArrayOf()
                val crushes = bundle.getFloatArray("crushes") ?: floatArrayOf()
                val pitchSemis = bundle.getFloatArray("pitchSemis") ?: floatArrayOf()
                val noteShiftSeeds = bundle.getLongArray("noteShiftSeeds") ?: longArrayOf()
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
                        speed = speeds.getOrElse(i) { 1f },
                        bassBoost = bassBoosts.getOrElse(i) { false },
                        crush = crushes.getOrElse(i) { 0f },
                        pitchSemi = pitchSemis.getOrElse(i) { 0f },
                        noteShiftSeed = noteShiftSeeds.getOrElse(i) { 0L },
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
                        putFloat("speed", it.speed)
                        putBoolean("bassBoost", it.bassBoost)
                        putFloat("crush", it.crush)
                        putFloat("pitchSemi", it.pitchSemi)
                        putLong("noteShiftSeed", it.noteShiftSeed)
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
                    speed = bundle.getFloat("speed", 1f),
                    bassBoost = bundle.getBoolean("bassBoost", false),
                    crush = bundle.getFloat("crush", 0f),
                    pitchSemi = bundle.getFloat("pitchSemi", 0f),
                    noteShiftSeed = bundle.getLong("noteShiftSeed", 0L),
                )
            },
        )
    }
}
