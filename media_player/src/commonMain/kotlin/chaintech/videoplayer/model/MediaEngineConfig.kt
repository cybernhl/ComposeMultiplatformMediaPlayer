package chaintech.videoplayer.model

enum class RtspTransport { AUTO, TCP, UDP }
enum class HardwareDecoderMode { AUTO, FORCE_HARDWARE, FORCE_SOFTWARE }

/**
 * 跨平台媒體引擎統一配置 (Media Engine Configuration)
 *
 * 提供全平台 (Android, iOS, Desktop JVM, WasmJS) 共通的影音播放、網路連線與緩衝控制參數。
 *
 * ### 平台參數對照表 (Cross-Platform Parameter Mapping Table)
 *
 * | 跨平台抽象欄位 | 語意說明 | Android (ExoPlayer) | iOS (AVPlayer) | JVM (FFmpeg) | WasmJS (Shaka Player) |
 * | :--- | :--- | :--- | :--- | :--- | :--- |
 * | **connectTimeoutSeconds** | 連線超時秒數 | `DefaultHttpDataSource.setConnectTimeoutMs()` | `AVURLAsset` 超時 | `"timeout"` / `"stimeout"` | `manifest.retryParameters.timeout` |
 * | **readTimeoutSeconds** | 數據停滯超時秒數 | `DefaultHttpDataSource.setReadTimeoutMs()` | `NSURLSession` 超時 | `"rw_timeout"` | `streaming.retryParameters.timeout` |
 * | **enableAutoReconnect** | 斷線自動重連 | `DefaultDataSource` 重試 | `AVPlayerItem` 重試 | `"reconnect" to "1"` | `streaming.retryParameters.maxAttempts` |
 * | **userAgent** | 自訂 User-Agent | `DefaultHttpDataSource.setUserAgent()` | `AVURLAssetHTTPHeaderFieldsKey` | `"user_agent"` | `ShakaWasmHelpers.configureHeaders()` |
 * | **allowCellularAccess** | 允許行動網路 | `TrackSelectionParameters` 網路約束 | `AVURLAssetAllowsCellularAccessKey` | Socket 網路層 constraint | 瀏覽器 Fetch API 網路層控制 |
 * | **bufferDurationSeconds** | 預載緩衝秒數 | `DefaultLoadControl.setBufferDurationsMs()` | `AVPlayerItem.preferredForwardBufferDuration` | `"probesize"` / `"buffer_size"` | `streaming.bufferingGoal` |
 * | **maxBufferDurationSeconds** | 最大緩衝長度 | `DefaultLoadControl.setBufferDurationsMs()` | `AVPlayerItem` 動態上限 | `"max_delay"` | `streaming.bufferBehind` |
 * | **rebufferGoalSeconds** | 卡頓重開播緩衝 | `DefaultLoadControl.bufferForPlaybackAfterRebufferMs` | `AVPlayerItem` 恢復門檻 | FFmpeg 讀取 Buffer 門檻 | `streaming.rebufferingGoal` |
 * | **lowLatencyMode** | 低延遲即時模式 | `ExoPlayer.setLivePlaybackSpeedControl()` | `AVPlayerItem.automaticallyWaitsToMinimizeStalling(false)` | `"fflags" to "nobuffer"` | `streaming.lowLatencyMode = true` |
 * | **allowAudioMixWithOthers** | 背景音訊混合 | `ExoPlayer.setAudioAttributes(..., false)` | `AVAudioSessionCategoryOptionMixWithOthers` | `JvmAudioSink` 混音器 | Web Audio API 混合模式 |
 * | **preservePitchOnSpeedChange** | 變速維持音高 | `PlaybackParameters(speed, pitch=1f)` | `AVPlayerItem.audioTimePitchAlgorithm` | FFmpeg `"atempo"` | `HTMLMediaElement.preservesPitch = true` |
 * | **maxVideoBitrate** | 最高流量限制 | `DefaultTrackSelector.setMaxVideoBitrate()` | `AVPlayerItem.preferredPeakBitRate` | FFmpeg 流篩選器 | `shaka.Player.selectVariantTrack()` |
 * | **maxVideoWidth / Height** | 最高解析度限制 | `DefaultTrackSelector.setMaxVideoSize()` | `AVPlayerItem.preferredMaximumResolution` | FFmpeg 流篩選器 | `shaka.Player.getVariantTracks()` 過濾 |
 * | **preferredRtspTransport** | RTSP 傳輸模式 | `RtspMediaSource.Factory.setForceUseTcp()` | AVPlayer 串流處理 | `"rtsp_transport" to "tcp"/"udp"` | Web 轉接協定 |
 * | **hardwareDecoderMode** | 硬體加速解碼 | `MediaCodecSelector` | AVFoundation VideoToolbox | `"vcodec" to "h264_videotoolbox"`等 | 瀏覽器 WebGL/WebGPU/Canvas |
 */
data class MediaEngineConfig(
    /** 網路連線與握手超時時間 (秒)，預設 10 秒 */
    val connectTimeoutSeconds: Int = 10,

    /** 數據停滯與讀取超時時間 (秒)，預設 10 秒 */
    val readTimeoutSeconds: Int = 10,

    /** 網路斷線時是否自動重連與重試，預設 true */
    val enableAutoReconnect: Boolean = true,

    /** 自訂 HTTP User-Agent 標頭，設為 null 則使用系統預設值 */
    val userAgent: String? = null,

    /** 是否允許使用行動數據 (Cellular Network) 進行媒體下載，預設 true */
    val allowCellularAccess: Boolean = true,

    /** 預載前向緩衝區長度 (秒)，設為 null 則使用系統預設動態值 */
    val bufferDurationSeconds: Float? = null,

    /** 最大緩衝記憶體長度上限 (秒)，設為 null 則使用系統預設動態值 */
    val maxBufferDurationSeconds: Float? = null,

    /** 網路卡頓重緩衝後，恢復開播所需的最小緩衝秒數，設為 null 使用預設值 */
    val rebufferGoalSeconds: Float? = null,

    /** 低延遲即時模式 (如 RTSP / Live 監控串流)，開啟時儘可能關閉緩衝以達到最低延遲，預設 false */
    val lowLatencyMode: Boolean = false,

    /** 是否允許與其他背景 App (如 Podcast、音樂播放器) 的聲音混合播放，預設 false */
    val allowAudioMixWithOthers: Boolean = false,

    /** 變速播放時是否維持音高 (不變調)，預設 true */
    val preservePitchOnSpeedChange: Boolean = true,

    /** 最高位元率限制 (bps)，適用於省流量模式，設為 null 不限制 */
    val maxVideoBitrate: Long? = null,

    /** 最高影片寬度限制 (像素)，設為 null 不限制 */
    val maxVideoWidth: Int? = null,

    /** 最高影片高度限制 (像素)，設為 null 不限制 */
    val maxVideoHeight: Int? = null,

    /** RTSP 串流之偏好傳輸協定 (AUTO, TCP, UDP)，預設 AUTO */
    val preferredRtspTransport: RtspTransport = RtspTransport.AUTO,

    /** 硬體解碼器偏好模式 (AUTO, FORCE_HARDWARE, FORCE_SOFTWARE)，預設 AUTO */
    val hardwareDecoderMode: HardwareDecoderMode = HardwareDecoderMode.AUTO,

    /** 底層特定平台獨有參數逃生口，適用於 5% 極特殊微調需求 */
    val escapeHatch: PlatformEscapeHatch? = null
)

/**
 * 各平台獨有參數逃生口 (Platform Escape Hatch)
 *
 * 提供極少數 (5%) 需直接透傳給底層播放引擎 (FFmpeg, ExoPlayer, AVPlayer, Shaka) 的特有參數。
 */
data class PlatformEscapeHatch(
    /** JVM (JavaCvPlayer / FFmpeg) 專屬鍵值對字典，如 `"probesize" to "1000000"` */
    val customFfmpegOptions: Map<String, String>? = null,

    /** Android (ExoPlayer) 專屬進階配置物件/字典 */
    val customExoPlayerOptions: Map<String, Any>? = null,

    /** iOS (AVFoundation / AVPlayer) 專屬字典，如 `AVURLAsset` options */
    val customAvPlayerOptions: Map<String, Any>? = null,

    /** WasmJS (Shaka Player) 專屬配置字典，如 `shaka.configure()` */
    val customShakaConfig: Map<String, Any>? = null
)
