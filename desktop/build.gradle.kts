import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

val osName = System.getProperty("os.name").lowercase()
val jfxClassifier = when {
    osName.contains("win") -> "win"
    osName.contains("mac") -> "mac"
    else -> "linux"
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    // Networking & DNS-over-HTTPS
    implementation(libs.okhttp)
    implementation(libs.okhttp.dnsoverhttps)

    // NewPipe Extractor (YouTube & SoundCloud parsing)
    implementation(libs.newpipe.extractor)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")

    // JSON support on JVM
    implementation("org.json:json:20240303")

    // JavaFX Media & WebView
    implementation("org.openjfx:javafx-media:21.0.5:$jfxClassifier")
    implementation("org.openjfx:javafx-web:21.0.5:$jfxClassifier")
    implementation("org.openjfx:javafx-swing:21.0.5:$jfxClassifier")
    implementation("org.openjfx:javafx-controls:21.0.5:$jfxClassifier")
    implementation("org.openjfx:javafx-graphics:21.0.5:$jfxClassifier")
    implementation("org.openjfx:javafx-base:21.0.5:$jfxClassifier")
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "com.melo.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "Melo"
            packageVersion = "1.0.0"
            description = "Melo Desktop Music Player"
            vendor = "Melo"

            windows {
                menuGroup = "Melo"
                upgradeUuid = "6a457476-0f72-466d-8be9-090c8a6f2360"
            }
        }
    }
}

tasks.register<JavaExec>("generateSnapshots") {
    mainClass.set("com.melo.desktop.SnapshotGenerator")
    classpath = sourceSets["main"].runtimeClasspath
}

