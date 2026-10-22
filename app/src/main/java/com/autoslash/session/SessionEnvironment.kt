package com.autoslash.session

import com.autoslash.domain.Platform

/** What is on screen right now, relative to the platform the session runs on. */
enum class ScreenCheck {
    /** Another app (or the lock screen, or nothing) is in the foreground. */
    OUTSIDE_APP,
    /** The target app is in the foreground but its short-video feed is not confirmed. */
    IN_APP,
    /** The target app's short-video feed is visible. */
    IN_FEED,
}

/**
 * The device-facing side of a session. Implemented by the accessibility service; kept as an
 * interface so the engine stays free of Android classes and can be unit-tested.
 */
interface SessionEnvironment {
    fun checkScreen(platform: Platform): ScreenCheck

    /** Performs one "next video" swipe. Returns true when the gesture completed. */
    suspend fun swipeToNext(): Boolean
}
