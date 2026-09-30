# Compose Multiplatform Media Player (CMplayer)

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue.svg?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-purple.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg?logo=android)](https://developer.android.com/)
[![iOS](https://img.shields.io/badge/Platform-iOS-black.svg?logo=apple)](https://developer.apple.com/ios/)
[![Desktop](https://img.shields.io/badge/Platform-Desktop-orange.svg)](https://www.jetbrains.com/desktop/)
[![WasmJS](https://img.shields.io/badge/Platform-WasmJS-yellow.svg)](https://webassembly.org/)

**Compose Multiplatform Media Player** is a cross-platform media playback library for Compose Multiplatform projects. It provides unified UI components and host controllers for playing online/local videos, HLS (`.m3u8`) live and VOD streams, audio tracks, YouTube videos, reels/TikTok-style short video pagers, and video previews across **Android, iOS, Desktop (JVM), and WasmJS**.

> **Note on Project Origin:**  
> This library codebase (`:media_player`) was reconstructed and restored from source JAR artifacts (`ComposeMultiplatformMediaPlayer-*-sources.jar`) and POM metadata published for version **1.0.53** (`network.chaintech:compose-multiplatform-media-player:1.0.53`).

---

## ✨ Features

- 📱 **Cross-Platform Support:** Single Kotlin Multiplatform codebase targeting Android, iOS, Desktop (JVM), and WasmJS.
- 📹 **Video Player (`VideoPlayerComposable` / `VideoPlayerWithControl`):**
  - Full-screen, PIP (Picture-in-Picture) floating window on Android & iOS.
  - HLS (`.m3u8`) multi-bitrate quality selection, audio track selection, and subtitle overlay.
  - Interactive gesture controls: right-side vertical drag for volume, long-press fast-forward (2x), and pinch-to-zoom.
  - Custom controls overlay with auto-hide timer, lock screen, and customizable playback speed.
  - Dynamic watermark (`MovingWatermark`) and seek bar chapter markers.
  - Resume playback from last saved position via DataStore Preferences (`PlaybackPreference`).
- 🎵 **Audio Player (`AudioPlayerComposable` / `AudioPlayer`):**
  - Full UI mode with album art, title, seek bar, time display, play/pause, next/previous, shuffle, and repeat controls.
  - Headless/independent audio playback host (`CMPAudioPlayer`).
- ▶️ **YouTube Player (`YouTubePlayerComposable` / `YoutubePlayerWithControl`):**
  - Embedded YouTube player with state management.
- 🎥 **Reels Player (`ReelsPlayerComposable`):**
  - Vertical or horizontal pager layout (TikTok/Reels style) with auto-play / preloading control.
- 🎞️ **Video Preview (`VideoPreviewComposable`):**
  - Animated video preview thumbnail generator.

---

## 🏗️ Architecture & Platform Engines

| Platform | Underlying Engine / Framework | Key Features |
| :--- | :--- | :--- |
| **Android** | AndroidX Media3 (ExoPlayer) | Native `PlayerView`, PIP mode via `PictureInPictureParams`, `SimpleCache` media caching. |
| **iOS** | Apple AVFoundation | `AVQueuePlayer`, `AVPlayerViewController`, `AVPictureInPictureController`, `AVAudioSession`. |
| **Desktop (JVM)** | JavaFX (`JFXPanel` / `WebView`) & VLCJ | Native desktop video rendering, VLC playback engine. |
| **WasmJS** | HTML5 Video & Shaka Player Interop | Web-native HLS and streaming playback. |

---

## 📦 Project Module Setup

The library module is located at `:media_player`.

### `build.gradle.kts` Configuration (`:media_player`)

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.android.library)
}

group = "network.chaintech"
version = "1.0.53"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvm {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }

    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
                    freeCompilerArgs.add("-Xjdk-release=8")
                }
            }
        }
    }

    val xcfName = "compose-multiplatform-media-player"
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = xcfName
            isStatic = true
            binaryOption("bundleId", "chaintech.videoplayer")
        }
    }

    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material)
            implementation(libs.jetbrains.compose.material.icons.extended)
            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.components.resources)

            implementation(libs.ktor.client.core)
            implementation(libs.image.loader)
            implementation("io.github.kevinnzou:compose-webview-multiplatform:2.0.3")
            implementation("net.engawapg.lib:zoomable:2.8.1")
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.vlcj)
            val javafxVersion = "17.0.10"
            val os = when {
                org.gradle.internal.os.OperatingSystem.current().isMacOsX -> if (System.getProperty("os.arch") == "aarch64") "mac-aarch64" else "mac"
                org.gradle.internal.os.OperatingSystem.current().isWindows -> "win"
                else -> "linux"
            }
            implementation("org.openjfx:javafx-controls:$javafxVersion:$os")
            implementation("org.openjfx:javafx-graphics:$javafxVersion:$os")
            implementation("org.openjfx:javafx-swing:$javafxVersion:$os")
            implementation("org.openjfx:javafx-web:$javafxVersion:$os")
            implementation("org.openjfx:javafx-base:$javafxVersion:$os")
            implementation("org.openjfx:javafx-media:$javafxVersion:$os")
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.process)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.datastore.preferences)

            implementation(libs.media3.exoplayer)
            implementation(libs.media3.common)
            implementation(libs.media3.datasource)
            implementation(libs.media3.datasource.okhttp)
            implementation("androidx.media3:media3-ui:${libs.versions.media3.get()}")
            implementation("androidx.media3:media3-exoplayer-hls:${libs.versions.media3.get()}")
        }
    }
}

android {
    namespace = "chaintech.videoplayer"
    compileSdk = 37

    androidResources {
        enable = true
    }

    buildFeatures {
        compose = true
    }

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "reelsdemo.composemultiplatformmediaplayer.generated.resources"
    generateResClass = always
}
```

---

## 🚀 Usage Guide

### 1. `MediaPlayerHost` Controller

`MediaPlayerHost` is the central state holder that controls media loading, playback, volume, speed, fullscreen, looping, and events:

```kotlin
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.host.MediaPlayerEvent
import chaintech.videoplayer.host.MediaPlayerError
import chaintech.videoplayer.model.PlayerSpeed
import chaintech.videoplayer.model.ScreenResize

val playerHost = remember {
    MediaPlayerHost(
        mediaUrl = "https://example.com/video.mp4",
        autoPlay = true,
        isMuted = false,
        initialSpeed = PlayerSpeed.X1,
        initialVideoFitMode = ScreenResize.FIT,
        isLooping = false
    )
}

// Control Methods
playerHost.play()
playerHost.pause()
playerHost.seekTo(15f) // Seek to 15 seconds
playerHost.setSpeed(PlayerSpeed.X1_5)
playerHost.toggleMuteUnmute()
playerHost.setFullScreen(true)

// Listen to Events
playerHost.onEvent = { event ->
    when (event) {
        is MediaPlayerEvent.CurrentTimeChange -> println("Current: ${event.currentTime}s")
        is MediaPlayerEvent.TotalTimeChange -> println("Total: ${event.totalTime}s")
        is MediaPlayerEvent.BufferChange -> println("Buffering: ${event.isBuffering}")
        is MediaPlayerEvent.PauseChange -> println("Is Paused: ${event.isPaused}")
        is MediaPlayerEvent.FullScreenChange -> println("Fullscreen: ${event.isFullScreen}")
        MediaPlayerEvent.MediaEnd -> println("Media Finished")
        is MediaPlayerEvent.PIPChange -> println("PiP Mode: ${event.isPip}")
    }
}

// Error Handling
playerHost.onError = { error ->
    when (error) {
        is MediaPlayerError.PlaybackError -> println("Playback error: ${error.details}")
        is MediaPlayerError.ResourceError -> println("Resource error: ${error.details}")
        else -> println("Error: $error")
    }
}
```

---

### 2. Video Player Composable

```kotlin
import chaintech.videoplayer.ui.video.VideoPlayerComposable
import chaintech.videoplayer.model.VideoPlayerConfig

val playerHost = remember { MediaPlayerHost(mediaUrl = "https://example.com/video.mp4") }

VideoPlayerComposable(
    modifier = Modifier.fillMaxSize(),
    playerHost = playerHost,
    playerConfig = VideoPlayerConfig(
        isSeekBarVisible = true,
        isDurationVisible = true,
        isAutoHideControlEnabled = true,
        controlHideIntervalSeconds = 5,
        isFastForwardBackwardEnabled = true,
        fastForwardBackwardIntervalSeconds = 10,
        enableResumePlayback = true,
        isZoomEnabled = true,
        isGestureVolumeControlEnabled = true,
        enablePIPControl = true
    )
)
```

---

### 3. Audio Player Composable

```kotlin
import chaintech.videoplayer.ui.audio.AudioPlayerComposable
import chaintech.videoplayer.model.AudioFile

val audioList = listOf(
    AudioFile(
        audioUrl = "https://example.com/audio1.mp3",
        audioTitle = "Track Title 1",
        thumbnailUrl = "https://example.com/cover1.jpg"
    )
)

val playerHost = remember { MediaPlayerHost(mediaUrl = audioList.first().audioUrl) }

AudioPlayerComposable(
    modifier = Modifier.fillMaxSize(),
    audios = audioList,
    playerHost = playerHost
)
```

---

### 4. YouTube Player Composable

```kotlin
import chaintech.videoplayer.ui.youtube.YouTubePlayerComposable

val youtubeHost = remember { MediaPlayerHost(mediaUrl = "dQw4w9WgXcQ") }

YouTubePlayerComposable(
    modifier = Modifier.fillMaxSize(),
    playerHost = youtubeHost
)
```

---

### 5. Reels Short Video Pager

```kotlin
import chaintech.videoplayer.ui.reel.ReelsPlayerComposable

val videoUrls = listOf(
    "https://example.com/reel1.mp4",
    "https://example.com/reel2.mp4"
)

ReelsPlayerComposable(
    modifier = Modifier.fillMaxSize(),
    urls = videoUrls
)
```

---

## 📊 Format Support Matrix

| Format | Android | iOS | Desktop (JVM) | WasmJS |
| :--- | :---: | :---: | :---: | :---: |
| **MP4** | ✅ | ✅ | ✅ | ✅ |
| **MOV** | ✅ | ✅ | ✅ | 🟡 |
| **3GP** | ✅ | ✅ | ✅ | ✅ |
| **AVI** | ✅ | ❌ | ✅ | ❌ |
| **MKV** | ✅ | ❌ | ✅ | ❌ |
| **HLS (.m3u8)** | ✅ | ✅ | ✅ | ✅ |
| **MP3** | ✅ | ✅ | ✅ | ✅ |
| **FLAC** | ✅ | ✅ | ✅ | 🟡 |
| **WAV** | ✅ | ✅ | ✅ | 🟡 |
| **YouTube** | ✅ | ✅ | ✅ | ✅ |

*Legend: `🟡` Browser-dependent support on WasmJS.*

---

## 📄 License

This project is licensed under the MIT License.
