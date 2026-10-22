package com.autoslash.session

import com.autoslash.domain.Platform
import com.autoslash.domain.SessionSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Runs one auto-scroll session at a time, for any [Platform].
 *
 * The whole session is a single coroutine [Job] in [scope]: cancelling it (STOP, limits reached,
 * service disconnected) is the only thing needed to guarantee no further gestures are dispatched.
 * Before every swipe the engine re-checks that the target app's feed is in the foreground.
 */
class SessionEngine(
    private val scope: CoroutineScope,
    private val environment: SessionEnvironment,
    private val clock: () -> Long,
    private val random: Random = Random.Default,
    private val tickMillis: Long = 250,
) {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var job: Job? = null

    /** Incremented on every start/stop so a stale coroutine can never overwrite newer state. */
    private var generation = 0

    val isRunning: Boolean get() = job?.isActive == true

    fun start(platform: Platform, settings: SessionSettings): Boolean {
        if (isRunning) return false
        val config = settings.normalized()
        val myGeneration = ++generation
        val startedAt = clock()
        _state.value = SessionState(
            status = SessionStatus.WAITING_FOR_APP,
            platform = platform,
            durationMillis = config.sessionDurationMinutes * 60_000L,
            maxVideos = config.maxVideos,
        )
        job = scope.launch {
            Run(platform, config, startedAt, myGeneration).execute()
        }
        return true
    }

    fun stop(reason: FinishReason = FinishReason.USER_STOP) {
        val wasActive = _state.value.isActive
        generation++
        job?.cancel()
        job = null
        if (wasActive) {
            _state.value = _state.value.copy(status = SessionStatus.STOPPED, finishReason = reason)
        }
    }

    /** Clears a finished/stopped session so the UI returns to the setup screen. */
    fun reset() {
        if (!isRunning) _state.value = SessionState()
    }

    private inner class Run(
        private val platform: Platform,
        private val config: SessionSettings,
        private val startedAt: Long,
        private val myGeneration: Int,
    ) {
        private val deadline = startedAt + config.sessionDurationMinutes * 60_000L

        private fun publish(transform: (SessionState) -> SessionState) {
            if (myGeneration != generation) return
            _state.value = transform(_state.value).copy(elapsedMillis = (clock() - startedAt).coerceAtLeast(0))
        }

        private fun timeIsUp() = clock() >= deadline

        private fun screenReady(check: ScreenCheck) =
            check == ScreenCheck.IN_FEED || (check == ScreenCheck.IN_APP && !platform.requiresFeedConfirmation)

        suspend fun execute() {
            var hasBeenInApp = false
            while (true) {
                val viewed = _state.value.videosViewed
                if (viewed >= config.maxVideos) return finish(FinishReason.VIDEO_LIMIT)
                if (timeIsUp()) return finish(FinishReason.TIME_LIMIT)

                // 1. Make sure the target feed is on screen before doing anything.
                val check = environment.checkScreen(platform)
                if (check != ScreenCheck.OUTSIDE_APP) hasBeenInApp = true
                if (!screenReady(check)) {
                    val waiting = when {
                        check == ScreenCheck.IN_APP -> SessionStatus.WAITING_FOR_FEED
                        hasBeenInApp -> SessionStatus.PAUSED_OUTSIDE_APP
                        else -> SessionStatus.WAITING_FOR_APP
                    }
                    publish { it.copy(status = waiting, currentWatchMillis = 0) }
                    delay(tickMillis)
                    continue
                }

                // 2. Watch the current video for a random time within the user's range.
                val watchMillis = random.nextLong(config.minWatchSeconds * 1_000L, config.maxWatchSeconds * 1_000L + 1)
                publish { it.copy(status = SessionStatus.WATCHING, currentWatchMillis = watchMillis) }
                if (!watch(watchMillis)) continue

                // 3. Swipe, but only if the feed is still in front right now.
                if (!screenReady(environment.checkScreen(platform))) continue
                publish { it.copy(status = SessionStatus.SWIPING) }
                val swiped = environment.swipeToNext()
                publish { if (swiped) it.copy(videosViewed = it.videosViewed + 1) else it }

                // 4. Short, slightly randomized pause so the next video can load.
                val pause = config.actionPauseMillis
                val jitter = pause / 4
                waitTicking(pause + random.nextLong(-jitter.toLong(), jitter.toLong() + 1))
            }
        }

        /** Returns true when the full watch time passed while staying in the feed. */
        private suspend fun watch(watchMillis: Long): Boolean {
            val end = clock() + watchMillis
            while (clock() < end) {
                if (timeIsUp()) return false
                if (!screenReady(environment.checkScreen(platform))) return false
                delay(minOf(tickMillis, end - clock()).coerceAtLeast(1))
                publish { it }
            }
            return true
        }

        private suspend fun waitTicking(millis: Long) {
            val end = clock() + millis
            while (clock() < end && !timeIsUp()) {
                delay(minOf(tickMillis, end - clock()).coerceAtLeast(1))
                publish { it }
            }
        }

        private fun finish(reason: FinishReason) {
            publish { it.copy(status = SessionStatus.FINISHED, finishReason = reason, currentWatchMillis = 0) }
        }
    }
}
