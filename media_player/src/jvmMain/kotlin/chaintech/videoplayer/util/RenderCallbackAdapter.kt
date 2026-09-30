package chaintech.videoplayer.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.nio.ByteBuffer

// VLC Rendering Adapter
internal class RenderCallbackAdapter : BufferFormatCallback, RenderCallback {
    var imageBitmap by mutableStateOf<ImageBitmap?>(null)
        private set

    override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
        return RV32BufferFormat(sourceWidth, sourceHeight)
    }

    override fun newFormatSize(
        bufferWidth: Int,
        bufferHeight: Int,
        displayWidth: Int,
        displayHeight: Int
    ) { }

    override fun allocatedBuffers(buffers: Array<out ByteBuffer>) { }

    override fun lock(mediaPlayer: MediaPlayer?) { }

    override fun unlock(mediaPlayer: MediaPlayer?) {}

    override fun display(
        mediaPlayer: MediaPlayer?,
        nativeBuffers: Array<ByteBuffer>,
        bufferFormat: BufferFormat,
        displayWidth: Int,
        displayHeight: Int
    ) {
        val buffer = nativeBuffers[0]
        buffer.rewind()

        val size = bufferFormat.width * bufferFormat.height * 4
        val byteArray = ByteArray(size)
        buffer.get(byteArray)

        updateImageBitmap(byteArray, bufferFormat.width, bufferFormat.height)
    }

    private fun updateImageBitmap(pixelData: ByteArray, width: Int, height: Int) {
        val bitmap = Bitmap().apply {
            allocPixels(ImageInfo.makeS32(width, height, ColorAlphaType.PREMUL))
            installPixels(imageInfo, pixelData, width * 4)
        }
        imageBitmap = bitmap.asComposeImageBitmap()
    }
}