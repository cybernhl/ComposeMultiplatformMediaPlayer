package chaintech.videoplayer.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

interface PlatformDrawable {
    @Composable
    fun asPainter(): Painter
}
class ComposeResourceDrawable(private val drawable: DrawableResource) : PlatformDrawable {
    @Composable
    override fun asPainter(): Painter = painterResource(drawable)
}