package chaintech.videoplayer.extension

import chaintech.videoplayer.util.formatMinSec

fun Float.formatMinSec(): String {
    return formatMinSec(this)
}
