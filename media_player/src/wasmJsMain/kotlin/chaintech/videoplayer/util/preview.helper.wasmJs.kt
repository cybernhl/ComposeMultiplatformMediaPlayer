@file:OptIn(ExperimentalWasmJsInterop::class)

package chaintech.videoplayer.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.Image
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement
import kotlin.coroutines.resume

actual suspend fun extractFrames(
    videoPath: String,
    frameCount: Int
): List<ImageBitmap> = suspendCancellableCoroutine { cont ->
    val video = (document.createElement("video") as HTMLVideoElement).apply {
        crossOrigin = "anonymous"
        src = videoPath
        preload = "auto"
    }
    val canvas = (document.createElement("canvas") as HTMLCanvasElement)
    val ctx = canvas.getContext("2d") as org.w3c.dom.CanvasRenderingContext2D

    val frames = mutableListOf<ImageBitmap>()
    var completed = false

    fun safeResume(result: List<ImageBitmap>) {
        if (!completed) {
            completed = true
            // cleanup
            try {
                video.pause()
                video.removeAttribute("src")
                video.load()
                video.remove()
                canvas.remove()
            } catch (_: Throwable) {
            }
            cont.resume(result)
        }
    }

    video.onloadedmetadata = {
        val duration = video.duration
        if (duration.isNaN() || duration <= 0 || frameCount <= 0) {
            safeResume(emptyList())
        } else {
            val times = if (frameCount == 1) {
                val safeOffset = (duration * 0.1).coerceAtMost(duration - 0.5)
                listOf(safeOffset)
            } else {
                val interval = duration / frameCount
                List(frameCount) { i -> i * interval }
            }

            var index = 0

            fun captureFrame() {
                if (index >= times.size) {
                    safeResume(frames)
                    return
                }

                video.currentTime = times[index]

                video.onseeked = {
                    try {
                        canvas.width = video.videoWidth
                        canvas.height = video.videoHeight
                        ctx.drawImage(video, 0.0, 0.0)

                        val dataUrl = canvas.toDataURL("image/png")
                        val base64 = dataUrl.substringAfter("base64,")
                        val binary = window.atob(base64)

                        // Convert binary string to ByteArray
                        val bytes = ByteArray(binary.length) { i -> binary[i].code.toByte() }

                        val skiaImage = Image.makeFromEncoded(bytes)
                        frames.add(skiaImage.toComposeImageBitmap())
                    } catch (e: Throwable) {
//                        console.error("Frame extraction failed", e)
                    }

                    index++
                    captureFrame()
                }
            }

            captureFrame()
        }
    }
    video.addEventListener("error") {
        safeResume(emptyList())
    }

    cont.invokeOnCancellation {
        safeResume(emptyList())
    }
}
