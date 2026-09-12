# Proguard / R8 rules for Melo Music Player

# ── 1. Нативные методы C++ (Melo Crystal Audio DSP / JNI) ──
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.melo.music.audio.MeloDspAudioProcessor { *; }

# ── 2. NewPipe Extractor (YouTube, парсеры и рефлексия) ──
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn org.mozilla.javascript.**

# ── 3. youtubedl-android & FFmpeg ──
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }
-dontwarn com.yausername.youtubedl_android.**
-dontwarn com.yausername.ffmpeg.**

# ── 4. OkHttp & DNS over HTTPS ──
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepclassmembers class * extends okhttp3.OkHttpClient { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ── 5. Media3 / ExoPlayer ──
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── 6. Модели данных (JSON сериализация / SharedPreferences) ──
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class * implements java.lang.reflect.Type
-keep class com.melo.music.extractor.TrackItem { *; }
-keep class com.melo.music.extractor.ResolvedTrack { *; }
-keep class com.melo.music.extractor.Source { *; }
-keep class com.melo.music.extractor.ItemKind { *; }
-keep class com.melo.music.playlists.Playlist { *; }
-keep class com.melo.music.profile.MeloProfile { *; }
-keep class com.melo.music.sync.YouTubeSyncManager$* { *; }

# ── 7. Compose, Coil & Appwrite ──
-dontwarn coil.**
-keep class coil.** { *; }
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }
-dontwarn org.osmdroid.**
-keep class org.osmdroid.** { *; }
-dontwarn io.appwrite.**
-keep class io.appwrite.** { *; }
-keepclassmembers class io.appwrite.** { *; }
