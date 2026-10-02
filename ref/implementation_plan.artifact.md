# Implementation Plan - MediaEngineConfig & MediaPlayerHost Reload Mechanism

Implement a unified cross-platform `MediaEngineConfig` with `PlatformEscapeHatch` and add `reload()` + `retryToken` reconnect mechanisms across all target platforms (Android, iOS, JVM, WasmJS) while maintaining 100% backward compatibility.

## Proposed Changes

### 1. Data Models (`chaintech.videoplayer.model`)

#### [NEW] [MediaEngineConfig.kt](file:///Users/neo.chang/Documents/AndroidStudioProjects/CMplayer/media_player/src/commonMain/kotlin/chaintech/videoplayer/model/MediaEngineConfig.kt)
- Define `RtspTransport` enum (`AUTO`, `TCP`, `UDP`).
- Define `HardwareDecoderMode` enum (`AUTO`, `FORCE_HARDWARE`, `FORCE_SOFTWARE`).
- Define `MediaEngineConfig` data class with 15 cross-platform default-initialized fields.
- Define `PlatformEscapeHatch` data class for platform-specific escape hatch maps (`customFfmpegOptions`, `customExoPlayerOptions`, `customAvPlayerOptions`, `customShakaConfig`).

#### [MODIFY] [VideoPlayerConfig.kt](file:///Users/neo.chang/Documents/AndroidStudioProjects/CMplayer/media_player/src/commonMain/kotlin/chaintech/videoplayer/model/VideoPlayerConfig.kt)
- Add `var engineConfig: MediaEngineConfig = MediaEngineConfig()` to `VideoPlayerConfig` with default value for 100% backward compatibility.

---

### 2. Host Controller (`chaintech.videoplayer.host`)

#### [MODIFY] [MediaPlayerHost.kt](file:///Users/neo.chang/Documents/AndroidStudioProjects/CMplayer/media_player/src/commonMain/kotlin/chaintech/videoplayer/host/MediaPlayerHost.kt)
- Add `engineConfig: MediaEngineConfig = MediaEngineConfig()` to constructor parameters with default value.
- Add `internal var retryToken by mutableStateOf(0L)`.
- Add public `fun reload()` method to reset metadata, refresh media info, unpause, set buffering, and increment `retryToken`.

---

### 3. Core Player Composable (`chaintech.videoplayer.util`)

#### [MODIFY] [CMPlayer.kt](file:///Users/neo.chang/Documents/AndroidStudioProjects/CMplayer/media_player/src/commonMain/kotlin/chaintech/videoplayer/util/CMPlayer.kt)
- Add `engineConfig: MediaEngineConfig = MediaEngineConfig()` and `retryToken: Long = 0L` to `CMPPlayer` `expect` signature with default values.

#### [MODIFY] [CMPlayer.jvm.kt](file:///Users/neo.chang/Documents/AndroidStudioProjects/CMplayer/media_player/src/jvmMain/kotlin/chaintech/videoplayer/util/CMPlayer.jvm.kt)
- Update `CMPPlayer` `actual` signature.
- Map `engineConfig` to FFmpeg options in `JavaCvPlayer` (RTSP transport, connect/read timeouts, auto-reconnect, User-Agent, low-latency nobuffer, hardware decoder mode, and `escapeHatch.customFfmpegOptions` override).
- Include `retryToken` in `LaunchedEffect(effectiveUrl, retryToken)`.

#### [MODIFY] [CMPlayer.android.kt](file:///Users/neo.chang/Documents/AndroidStudioProjects/CMplayer/media_player/src/androidMain/kotlin/chaintech/videoplayer/util/CMPlayer.android.kt)
- Update `CMPPlayer` `actual` signature.
- Translate `engineConfig` to ExoPlayer / Media3 settings (`DefaultHttpDataSource` connect/read timeout, User-Agent, AudioAttributes focus/mixing, bitrate constraints).
- Include `retryToken` in `LaunchedEffect(url, retryToken)`.

#### [MODIFY] [CMPlayer.ios.kt](file:///Users/neo.chang/Documents/AndroidStudioProjects/CMplayer/media_player/src/iosMain/kotlin/chaintech/videoplayer/util/CMPlayer.ios.kt)
- Update `CMPPlayer` `actual` signature.
- Translate `engineConfig` to AVFoundation settings (`preferredForwardBufferDuration`, `automaticallyWaitsToMinimizeStalling`, `AVAudioSession` audio mixing, `AVURLAsset` headers/cellular access).
- Include `retryToken` in `LaunchedEffect(url, retryToken)`.

#### [MODIFY] [CMPlayer.wasmJs.kt](file:///Users/neo.chang/Documents/AndroidStudioProjects/CMplayer/media_player/src/wasmJsMain/kotlin/chaintech/videoplayer/util/CMPlayer.wasmJs.kt)
- Update `CMPPlayer` `actual` signature.
- Translate `engineConfig` to Shaka Player settings (`bufferingGoal`, `lowLatencyMode`, timeout retry parameters).
- Include `retryToken` in `LaunchedEffect(url, retryToken)`.

#### [MODIFY] [VideoPlayerWithControl.kt](file:///Users/neo.chang/Documents/AndroidStudioProjects/CMplayer/media_player/src/commonMain/kotlin/chaintech/videoplayer/ui/video/VideoPlayerWithControl.kt)
- Pass `engineConfig = playerConfig.engineConfig ?: playerHost.engineConfig` and `retryToken = playerHost.retryToken` to `CMPPlayer`.

---

## Verification Plan

### Automated Tests
- Run `:media_player:compileKotlinJvm` and `:media_player:assembleDebug` to verify compilation across all platforms.
- Run `:media_player:publishToMavenLocal` to confirm Maven publication succeeds without breaking backward compatibility.

### Manual Verification
- Test `playerHost.reload()` in demo/test module to confirm player re-initializes and reconnects.
- Verify `engineConfig` parameter overrides default FFmpeg/ExoPlayer/AVPlayer options as expected.
