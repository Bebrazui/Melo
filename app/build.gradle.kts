plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.melo.music"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.melo.music"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        vectorDrawables {
            useSupportLibrary = true
        }

        // youtubedl-android поставляет нативный Python/yt-dlp по ABI.
        // Для тестового билда ограничиваемся arm64-v8a (телефон V2405A).
        // Для релиза — ABI splits со всеми архитектурами.
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // NewPipe Extractor использует свежие java.* API
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // youtubedl-android требует распаковки нативных библиотек.
        jniLibs {
            useLegacyPackaging = true
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Media3 — фоновое воспроизведение через MediaSessionService
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    implementation(libs.kotlinx.coroutines.android)

    // yt-dlp на устройстве (SoundCloud, Яндекс и прочее)
    implementation(libs.youtubedl.library)
    implementation(libs.youtubedl.ffmpeg)

    // NewPipe Extractor — YouTube
    implementation(libs.newpipe.extractor)
    implementation(libs.okhttp)

    // Обложки треков
    implementation(libs.coil.compose)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
