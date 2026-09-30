package chaintech.videoplayer.youtube

internal object PlayerEventParser {

    private val eventPattern = "ytplayer://([A-z]+)(\\?data=([A-z\\d._-]+))*".toRegex()

    fun parse(url: String?): PlayerEvent? {
        val match = eventPattern.matchEntire(url.orEmpty()) ?: return null
        val eventType = PlayerAction.fromAction(match.groupValues[1])
        val data = match.groupValues[3]
        return PlayerEvent.create(eventType, data)
    }
}