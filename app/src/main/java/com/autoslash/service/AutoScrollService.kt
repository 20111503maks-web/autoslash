package com.autoslash.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.Intent
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.autoslash.domain.Platform
import com.autoslash.session.ScreenCheck
import com.autoslash.session.SessionEngine
import com.autoslash.session.SessionEnvironment
import com.autoslash.session.SessionManager
import com.autoslash.session.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Hosts the [SessionEngine] and gives it eyes (which app/feed is in front) and hands (swipes).
 *
 * Safety rules enforced here:
 *  - gestures are dispatched only while a session is running;
 *  - only when the session's platform app is the active window, the screen is on and unlocked;
 *  - only view IDs and the package name are read; no text, passwords, cookies or tokens.
 */
class AutoScrollService : AccessibilityService(), SessionEnvironment {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var engine: SessionEngine? = null
    private var overlay: FloatingStopOverlay? = null

    /** Set by the "I'm in Shorts" overlay button when automatic detection fails. Per session. */
    private var feedConfirmedManually = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        val sessionEngine = SessionEngine(scope, this, SystemClock::elapsedRealtime)
        engine = sessionEngine
        overlay = FloatingStopOverlay(
            context = this,
            onStop = { SessionManager.stop() },
            onConfirmFeed = { feedConfirmedManually = true },
        )
        SessionManager.attach(sessionEngine)
        scope.launch {
            var wasActive = false
            sessionEngine.state.collect { state ->
                if (state.isActive && !wasActive) feedConfirmedManually = false
                wasActive = state.isActive
                renderOverlay(state)
            }
        }
    }

    private fun renderOverlay(state: SessionState) {
        val panel = overlay ?: return
        if (state.isActive) panel.show(state) else panel.hide()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        engine?.stop()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        tearDown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        tearDown()
        scope.cancel()
        super.onDestroy()
    }

    private fun tearDown() {
        engine?.let(SessionManager::detach)
        engine = null
        overlay?.hide()
        overlay = null
    }

    // --- SessionEnvironment ---

    override fun checkScreen(platform: Platform): ScreenCheck {
        val power = getSystemService(PowerManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (power?.isInteractive != true || keyguard?.isKeyguardLocked == true) return ScreenCheck.OUTSIDE_APP

        val root = rootInActiveWindow ?: return ScreenCheck.OUTSIDE_APP
        if (!platform.owns(root.packageName)) return ScreenCheck.OUTSIDE_APP
        if (!platform.requiresFeedConfirmation) return ScreenCheck.IN_FEED
        return if (feedConfirmedManually || FeedDetector.isShortsVisible(root)) ScreenCheck.IN_FEED else ScreenCheck.IN_APP
    }

    override suspend fun swipeToNext(): Boolean {
        if (engine?.isRunning != true) return false
        val metrics = resources.displayMetrics
        val gesture = SwipeGestureFactory.nextVideo(metrics.widthPixels, metrics.heightPixels)
        return suspendCancellableCoroutine { cont ->
            val callback = object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(false)
                }
            }
            if (!dispatchGesture(gesture, callback, null) && cont.isActive) cont.resume(false)
        }
    }
}
