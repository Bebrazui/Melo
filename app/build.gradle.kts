import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Данные подписи релиза держим вне репозитория (keystore.properties в .gitignore).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.melo.music"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.melo.music"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "0.3"

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

    flavorDimensions += "store"
    productFlavors {
        create("google") {
            dimension = "store"
        }
        create("ruStore") {
            dimension = "store"
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Подписываем релиз только если есть keystore.properties.
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        buildConfig = true
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

    ndkVersion = "27.0.12077973"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.compose.material3:material3:1.5.0-alpha10")
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
    implementation(libs.okhttp.dnsoverhttps)

    // Обложки треков
    implementation(libs.coil.compose)
    implementation(libs.androidx.palette)

    // Smooth corners (squircle) в стиле PixelPlayer
    implementation(libs.smooth.corner.rect)

    // Lottie анимации
    implementation(libs.lottie.compose)

    // Карта музыки
    implementation(libs.appwrite)
    implementation(libs.osmdroid.android)
    implementation(libs.play.services.location)

    // Нативный вход Google (только flavor google)
    "googleImplementation"(libs.androidx.credentials)
    "googleImplementation"(libs.androidx.credentials.play.services)
    "googleImplementation"(libs.googleid)
    "googleImplementation"(libs.play.services.auth)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
