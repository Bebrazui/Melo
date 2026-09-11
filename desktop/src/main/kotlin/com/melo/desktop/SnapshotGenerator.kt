package com.melo.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.melo.desktop.audio.DesktopAudioPlayer
import com.melo.desktop.extractor.ResolvedTrack
import com.melo.desktop.extractor.Source
import com.melo.desktop.extractor.TrackItem
import com.melo.desktop.storage.DesktopStorage
import com.melo.desktop.ui.components.NowPlayingContent
import com.melo.desktop.ui.theme.MeloDesktopTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

object SnapshotGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        val outDir = File("C:/Users/ttt79/.gemini/antigravity/brain/96a7dc7a-5156-458c-b4bc-8dabdc3b8046")
        outDir.mkdirs()

        // 1. Установка данных для воспроизведения
        val mockTrack = ResolvedTrack(
            title = "Endless Waves & Echoes",
            artist = "Melo Chill Collective",
            audioUrl = "http://localhost",
            originalUrl = "https://youtube.com/watch?v=sample",
            thumbnailUrl = null,
            source = Source.YOUTUBE_MUSIC,
        )
        DesktopAudioPlayer.setPreviewState(
            track = mockTrack,
            playing = true,
            posMs = 74000L,
            durMs = 215000L,
            vol = 0.78f,
        )

        DesktopStorage.toggleFavorite(
            TrackItem(
                title = "Endless Waves & Echoes",
                uploader = "Melo Chill Collective",
                url = "https://youtube.com/watch?v=sample",
                durationSeconds = 215,
                thumbnailUrl = null,
                source = Source.YOUTUBE_MUSIC,
            )
        )

        // 2. Рендеринг главного окна (HomeScreen + SeaCard + Чистый темный фон)
        println("Rendering Home Screen...")
        val homeScene = ImageComposeScene(width = 1180, height = 760) {
            MeloDesktopTheme {
                MeloAppContent()
            }
        }
        val homeImg = homeScene.render()
        val homeBytes = homeImg.encodeToData(EncodedImageFormat.PNG)?.bytes
        if (homeBytes != null) {
            val file = File(outDir, "melo_desktop_home.png")
            file.writeBytes(homeBytes)
            println("Saved: ${file.absolutePath} (${file.length()} bytes)")
        }

        // 3. Рендеринг полноэкранного плеера NowPlaying (WavySlider + 96x74 капсула)
        println("Rendering NowPlaying Player...")
        val playerScene = ImageComposeScene(width = 1180, height = 760) {
            MeloDesktopTheme {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F1412))) {
                    NowPlayingContent(
                        onDismiss = {},
                        onPrevious = {},
                        onNext = {},
                    )
                }
            }
        }
        val playerImg = playerScene.render()
        val playerBytes = playerImg.encodeToData(EncodedImageFormat.PNG)?.bytes
        if (playerBytes != null) {
            val file = File(outDir, "melo_desktop_player.png")
            file.writeBytes(playerBytes)
            println("Saved: ${file.absolutePath} (${file.length()} bytes)")
        }

        // 3.5 Рендеринг плеера в режиме виниловой пластинки (Vinyl Record Mode)
        println("Rendering Vinyl Record NowPlaying Player...")
        val vinylScene = ImageComposeScene(width = 1180, height = 760) {
            MeloDesktopTheme {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F1412))) {
                    NowPlayingContent(
                        onDismiss = {},
                        onPrevious = {},
                        onNext = {},
                        initialVinylMode = true,
                    )
                }
            }
        }
        val vinylImg = vinylScene.render()
        val vinylBytes = vinylImg.encodeToData(EncodedImageFormat.PNG)?.bytes
        if (vinylBytes != null) {
            val file = File(outDir, "melo_desktop_vinyl_player.png")
            file.writeBytes(vinylBytes)
            println("Saved: ${file.absolutePath} (${file.length()} bytes)")
        }

        // 4. Рендеринг пустого состояния NowPlaying (когда ничего не играет)
        println("Rendering Empty NowPlaying Player...")
        DesktopAudioPlayer.setPreviewState(track = null)
        val emptyPlayerScene = ImageComposeScene(width = 1180, height = 760) {
            MeloDesktopTheme {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F1412))) {
                    NowPlayingContent(
                        onDismiss = {},
                        onPrevious = {},
                        onNext = {},
                    )
                }
            }
        }
        val emptyImg = emptyPlayerScene.render()
        val emptyBytes = emptyImg.encodeToData(EncodedImageFormat.PNG)?.bytes
        if (emptyBytes != null) {
            val file = File(outDir, "melo_desktop_empty_player.png")
            file.writeBytes(emptyBytes)
            println("Saved: ${file.absolutePath} (${file.length()} bytes)")
        }

        println("ALL SNAPSHOTS GENERATED SUCCESSFULLY!")
        kotlin.system.exitProcess(0)
    }
}
