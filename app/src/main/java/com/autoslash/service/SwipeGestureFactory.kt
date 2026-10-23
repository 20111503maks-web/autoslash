package com.autoslash.service

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import kotlin.random.Random

/**
 * Builds an ordinary "swipe up to the next video" gesture: a single finger moving from the lower
 * part of the screen to the upper part along a slightly curved path, with small natural
 * variations in position and speed, like a person's thumb.
 */
object SwipeGestureFactory {

    fun nextVideo(screenWidth: Int, screenHeight: Int, random: Random = Random.Default): GestureDescription {
        val startX = screenWidth * random.between(0.44, 0.58)
        val startY = screenHeight * random.between(0.70, 0.80)
        val endX = startX + screenWidth * random.between(-0.05, 0.05)
        val endY = screenHeight * random.between(0.20, 0.30)
        // Thumbs move in a gentle arc rather than a perfectly straight line.
        val controlX = (startX + endX) / 2 + screenWidth * random.between(-0.06, 0.06)
        val controlY = (startY + endY) / 2

        val path = Path().apply {
            moveTo(startX.toFloat(), startY.toFloat())
            quadTo(controlX.toFloat(), controlY.toFloat(), endX.toFloat(), endY.toFloat())
        }
        val durationMs = random.nextLong(220, 381)
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
    }

    /**
     * Double tap in the middle of the video: the standard "like" gesture in both TikTok and
     * YouTube Shorts. It only ever adds a like (it never removes one).
     */
    fun doubleTapLike(screenWidth: Int, screenHeight: Int, random: Random = Random.Default): GestureDescription {
        val x = (screenWidth * random.between(0.42, 0.58)).toFloat()
        val y = (screenHeight * random.between(0.40, 0.50)).toFloat()
        val tap = Path().apply { moveTo(x, y) }
        val gap = random.nextLong(110, 151)
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(tap, 0, TAP_MS))
            .addStroke(GestureDescription.StrokeDescription(tap, TAP_MS + gap, TAP_MS))
            .build()
    }

    private const val TAP_MS = 40L

    private fun Random.between(from: Double, until: Double) = from + nextDouble() * (until - from)
}
