package com.autoslash.domain

/** User-tunable session parameters. Shared by all platforms. */
data class SessionSettings(
    val minWatchSeconds: Int = 6,
    val maxWatchSeconds: Int = 18,
    val sessionDurationMinutes: Int = 20,
    /** Pause after each swipe before the next video starts being "watched". */
    val actionPauseMillis: Int = 800,
    val maxVideos: Int = 100,
) {
    /** Clamps values into safe ranges and makes sure min <= max. */
    fun normalized(): SessionSettings {
        val min = minWatchSeconds.coerceIn(WATCH_RANGE)
        val max = maxWatchSeconds.coerceIn(WATCH_RANGE).coerceAtLeast(min)
        return copy(
            minWatchSeconds = min,
            maxWatchSeconds = max,
            sessionDurationMinutes = sessionDurationMinutes.coerceIn(DURATION_RANGE),
            actionPauseMillis = actionPauseMillis.coerceIn(PAUSE_RANGE),
            maxVideos = maxVideos.coerceIn(VIDEOS_RANGE),
        )
    }

    companion object {
        val WATCH_RANGE = 2..120
        val DURATION_RANGE = 1..240
        val PAUSE_RANGE = 200..5_000
        val VIDEOS_RANGE = 1..1_000
    }
}
