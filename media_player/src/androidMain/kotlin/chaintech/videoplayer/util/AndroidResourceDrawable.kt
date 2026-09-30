package chaintech.videoplayer.util

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

class AndroidResourceDrawable(@DrawableRes val resId: Int) : PlatformDrawable {
    @Composable
    override fun asPainter(): Painter = painterResource(id = resId)
}