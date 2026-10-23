package com.autoslash.session

import com.autoslash.domain.Platform

enum class SessionStatus {
    IDLE,
    /** Session started, waiting for the target app to come to the foreground. */
    WAITING_FOR_APP,
    /** Target app is open but the short-video feed (e.g. Shorts tab) is not visible yet. */
    WAITING_FOR_FEED,
    WATCHING,
    SWIPING,
    /** User left the target app; no gestures are dispatched until they return. */
    PAUSED_OUTSIDE_APP,
    /** User pressed Pause: stays on the current video, session clock stopped. */
    PAUSED_BY_USER,
    FINISHED,
    STOPPED;

    val isActive: Boolean get() = this != IDLE && this != FINISHED && this != STOPPED
}

enum class FinishReason { TIME_LIMIT, VIDEO_LIMIT, USER_STOP, SERVICE_DISCONNECTED }

data class SessionState(
    val status: SessionStatus = SessionStatus.IDLE,
    val platform: Platform? = null,
    val elapsedMillis: Long = 0,
    val durationMillis: Long = 0,
    val videosViewed: Int = 0,
    /** Likes the user gave with the ❤ button on the floating panel. */
    val likes: Int = 0,
    val maxVideos: Int = 0,
    /** How long the current video will be watched before swiping, for UI display. */
    val currentWatchMillis: Long = 0,
    val finishReason: FinishReason? = null,
) {
    val isActive: Boolean get() = status.isActive
    val remainingMillis: Long get() = (durationMillis - elapsedMillis).coerceAtLeast(0)
    val remainingVideos: Int get() = (maxVideos - videosViewed).coerceAtLeast(0)
}
