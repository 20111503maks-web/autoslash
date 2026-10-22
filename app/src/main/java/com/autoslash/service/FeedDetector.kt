package com.autoslash.service

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Detects whether YouTube's Shorts player is on screen by looking only at view resource IDs
 * (no text, no account data). The Shorts player views use IDs containing "reel"
 * (e.g. `reel_recycler`, `reel_player_page_container`).
 *
 * If YouTube renames these views the detector returns false; the user can then confirm
 * manually with the "I'm in Shorts" button on the floating panel.
 */
object FeedDetector {
    private const val SHORTS_ID_MARKER = "reel"
    private const val MAX_NODES = 400

    fun isShortsVisible(root: AccessibilityNodeInfo): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            val id = node.viewIdResourceName
            if (id != null && id.contains(SHORTS_ID_MARKER, ignoreCase = true) && node.isVisibleToUser) {
                return true
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let(queue::add)
            }
        }
        return false
    }
}
