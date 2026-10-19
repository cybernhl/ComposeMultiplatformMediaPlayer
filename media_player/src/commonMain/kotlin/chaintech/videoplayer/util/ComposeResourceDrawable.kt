package chaintech.videoplayer.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

interface PlatformDrawable {
    @Composable
    fun asPainter(): Painter
}

class ComposeResourceDrawable(private val drawable: DrawableResource) : PlatformDrawable {
    @Composable
    override fun asPainter(): Painter {
        return painterResource(drawable)
    }
}

class VectorPlatformDrawable(private val imageVector: ImageVector) : PlatformDrawable {
    @Composable
    override fun asPainter(): Painter = rememberVectorPainter(imageVector)
}
