package chaintech.videoplayer.util

import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.Component
import java.util.Locale

@Suppress("UnsafeDynamicallyLoadedCode")
internal fun initializeMediaPlayerComponent(): Component {
    NativeDiscovery().discover()

    return if (isMacOS()) {
        CallbackMediaPlayerComponent()
    } else {
        EmbeddedMediaPlayerComponent()
    }
}

/**
 * Returns [MediaPlayer] from player components.
 * The method names are the same, but they don't share the same parent/interface.
 * That's why we need this method.
 */
internal fun Component.mediaPlayer() = when (this) {
    is CallbackMediaPlayerComponent -> mediaPlayer()
    is EmbeddedMediaPlayerComponent -> mediaPlayer()
    else -> error("mediaPlayer() can only be called on vlcj player components")
}

internal fun isMacOS(): Boolean {
    val os = System
        .getProperty("os.name", "generic")
        .lowercase(Locale.ENGLISH)
    return "mac" in os || "darwin" in os
}