# Melo

Melo is a native Android music player and streaming application built with Jetpack Compose and AndroidX Media3. It combines on-device stream resolution, synchronized lyrics, audio enhancements, and an expressive modern interface.

---

## Overview

Melo is designed around an on-device architecture. Audio stream extraction and media resolving occur locally on the device rather than relying on central intermediary servers. This approach ensures high reliability, privacy, and low latency while remaining lightweight and modular.

---

## Key Features

### Material 3 Expressive Interface
- Fluid playback controls featuring physics-based squash-and-stretch spring animations.
- Dynamic neighbor compression: adjacent controls subtly compress when a button is held down.
- Corner radius morphing: buttons flatten into squircle capsules during touch interactions.
- Rotating Vinyl Record Mode: transforms standard square album artwork into an animated vinyl record with concentric grooves, realistic anisotropic light reflections, and center spindle cutout.
- 3D Gyroscope Parallax: real-time spatial tilt effect applied to artwork and background lighting based on device orientation.
- Shape-morphing progress indicators based on Material 3 Expressive specifications with smooth fade and scale transitions.

### In-App DPI and UI Scaling
- Granular in-app display density slider (75% to 135%) designed for automotive infotainment systems, tablets, and ultra-high DPI smartphones.
- Quick preset buttons for instant scaling without altering device-wide system settings.

### Media Extraction and Streaming
- On-device YouTube Music and SoundCloud resolution via NewPipe Extractor and yt-dlp.
- Resilient audio pipeline supporting automated format selection, stream caching, and prefetching.

### Embedded Network Proxy Engine (ByeDPI)
- Native C-based SOCKS5 proxy embedded directly into the application.
- Transparent stream routing with automatic failover to direct connections when SOCKS proxying is unnecessary or unavailable.
- Dynamic network handover detection that restarts the proxy daemon upon transitions between Wi-Fi and cellular connections.
- User-configurable bypass toggle in application settings.

### Spatial Audio and Equalizer
- Integrated 10-band audio equalizer with frequency control and dB gain adjustments.
- Melo 3D Surround spatial audio virtualizer for immersive headphone listening.

### Synchronized Lyrics and Karaoke
- Time-synchronized lyric parsing with word-level highlight transitions.
- Interactive lyric view with tap-to-seek functionality.

### Offline Storage and Downloads
- High-speed background audio downloading with notification progress tracking.
- Batch download capabilities for favorite tracks and playlists.
- Automatic offline caching of liked tracks.

---

## Architecture and Tech Stack

| Layer | Technologies |
|---|---|
| UI and Presentation | Jetpack Compose, Material 3 Expressive, Compose Navigation |
| Audio Playback | AndroidX Media3 (ExoPlayer), MediaSessionService |
| Audio Effects | Android AudioEffect API, Equalizer, Virtualizer |
| Network and HTTP | OkHttp 4, OkHttp DNS-over-HTTPS, Custom ProxySelector |
| Native Layer | C, CMake, NDK, ByeDPI daemon |
| Local Database | Room SQLite, SharedPreferences |
| Image Loading | Coil Compose |

---

## Building from Source

### Prerequisites

- Java Development Kit (JDK) 17 or higher
- Android SDK with Platform 35 and Build-Tools 35.0.0
- Android NDK and CMake 3.22+ for native C components

### Compilation

Clone the repository and build using Gradle:

```bash
# Clone the repository
git clone https://github.com/Bebrazui/Melo.git
cd Melo

# Build debug variant
./gradlew assembleDebug

# Build release variant
./gradlew assembleGoogleRelease
```

### Installation via ADB

Connect an Android device with USB debugging enabled:

```bash
adb install -r app/build/outputs/apk/google/release/app-google-release.apk
```

---

## Project Structure

```
Melo/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── cpp/            # Native C sources (ByeDPI engine and JNI bindings)
│   │   │   ├── java/com/melo/music/
│   │   │   │   ├── audio/      # Equalizer and Spatial Audio management
│   │   │   │   ├── byedpi/     # ByeDPI process controller and network listener
│   │   │   │   ├── extractor/  # On-device NewPipe and yt-dlp extraction logic
│   │   │   │   ├── lyrics/     # Synchronized lyric providers and cache
│   │   │   │   ├── net/        # Network selectors and OkHttp configurations
│   │   │   │   ├── offline/    # Multi-stream file downloader service
│   │   │   │   ├── playback/   # Foreground MediaSession service
│   │   │   │   ├── settings/   # Reactive application preferences
│   │   │   │   └── ui/         # Jetpack Compose UI screens, cards, and theme
│   │   │   └── res/            # Audio click sound, vector icons, mipmaps
├── gradle/                     # Version catalog and Gradle wrapper
├── LICENSE                     # GNU Affero General Public License v3.0
└── README.md                   # Project documentation
```

---

## Privacy and Data Handling

Melo does not collect, transmit, or monetize personal user data. All search operations, caching, and listening telemetry occur strictly on the user device.

---

## License

This project is licensed under the GNU Affero General Public License v3.0 (AGPL-3.0). See the [LICENSE](LICENSE) file for the full license text.
