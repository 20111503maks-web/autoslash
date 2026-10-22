package com.autoslash.session

import com.autoslash.domain.Platform
import com.autoslash.domain.SessionSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionEngineTest {

    private class FakeEnvironment(private val now: () -> Long) : SessionEnvironment {
        var screen = ScreenCheck.IN_FEED
        val swipeTimes = mutableListOf<Long>()
        val checkedPlatforms = mutableSetOf<Platform>()

        override fun checkScreen(platform: Platform): ScreenCheck {
            checkedPlatforms += platform
            return screen
        }

        override suspend fun swipeToNext(): Boolean {
            swipeTimes += now()
            return true
        }
    }

    private val settings = SessionSettings(
        minWatchSeconds = 3,
        maxWatchSeconds = 6,
        sessionDurationMinutes = 10,
        actionPauseMillis = 400,
        maxVideos = 50,
    )

    private fun TestScope.newEngine(): Pair<SessionEngine, FakeEnvironment> {
        val clock = { testScheduler.currentTime }
        val env = FakeEnvironment(clock)
        return SessionEngine(backgroundScope, env, clock) to env
    }

    @Test
    fun tiktokSwipesWithRandomWatchTimeWithinRange() = runTest {
        val (engine, env) = newEngine()
        assertTrue(engine.start(Platform.TIKTOK, settings))
        advanceTimeBy(60_000)

        assertTrue(env.swipeTimes.size >= 8)
        assertEquals(env.swipeTimes.size, engine.state.value.videosViewed)
        val gaps = env.swipeTimes.zipWithNext { a, b -> b - a }
        // watch 3..6 s + pause 400 ms ± 25 %
        gaps.forEach { assertTrue("gap $it", it in 3_300..6_600) }
        assertTrue("watch times must vary", gaps.toSet().size > 1)
        engine.stop()
    }

    @Test
    fun stopsAtVideoLimitAndNoMoreGestures() = runTest {
        val (engine, env) = newEngine()
        engine.start(Platform.TIKTOK, settings.copy(maxVideos = 3))
        advanceTimeBy(120_000)

        assertEquals(3, env.swipeTimes.size)
        assertEquals(SessionStatus.FINISHED, engine.state.value.status)
        assertEquals(FinishReason.VIDEO_LIMIT, engine.state.value.finishReason)
        assertFalse(engine.isRunning)
    }

    @Test
    fun stopsAtTimeLimit() = runTest {
        val (engine, env) = newEngine()
        engine.start(Platform.TIKTOK, settings.copy(sessionDurationMinutes = 1))
        advanceTimeBy(61_000)
        runCurrent()

        assertEquals(SessionStatus.FINISHED, engine.state.value.status)
        assertEquals(FinishReason.TIME_LIMIT, engine.state.value.finishReason)
        assertTrue(env.swipeTimes.all { it <= 60_000 })
        val count = env.swipeTimes.size
        advanceTimeBy(60_000)
        assertEquals(count, env.swipeTimes.size)
        assertFalse(engine.isRunning)
    }

    @Test
    fun userStopCancelsLoopImmediately() = runTest {
        val (engine, env) = newEngine()
        engine.start(Platform.YOUTUBE_SHORTS, settings)
        advanceTimeBy(20_000)
        engine.stop()
        val count = env.swipeTimes.size

        advanceTimeBy(300_000)
        assertEquals(count, env.swipeTimes.size)
        assertEquals(SessionStatus.STOPPED, engine.state.value.status)
        assertEquals(FinishReason.USER_STOP, engine.state.value.finishReason)
        assertFalse(engine.isRunning)
    }

    @Test
    fun noGesturesOutsideTargetApp() = runTest {
        val (engine, env) = newEngine()
        env.screen = ScreenCheck.OUTSIDE_APP
        engine.start(Platform.TIKTOK, settings)
        advanceTimeBy(30_000)
        assertTrue(env.swipeTimes.isEmpty())
        assertEquals(SessionStatus.WAITING_FOR_APP, engine.state.value.status)

        env.screen = ScreenCheck.IN_FEED
        advanceTimeBy(10_000)
        assertTrue(env.swipeTimes.isNotEmpty())

        env.screen = ScreenCheck.OUTSIDE_APP
        val count = env.swipeTimes.size
        advanceTimeBy(30_000)
        assertEquals(count, env.swipeTimes.size)
        assertEquals(SessionStatus.PAUSED_OUTSIDE_APP, engine.state.value.status)
        engine.stop()
    }

    @Test
    fun youtubeWaitsForShortsFeedButTikTokDoesNot() = runTest {
        val (engine, env) = newEngine()
        env.screen = ScreenCheck.IN_APP

        engine.start(Platform.YOUTUBE_SHORTS, settings)
        advanceTimeBy(30_000)
        assertTrue(env.swipeTimes.isEmpty())
        assertEquals(SessionStatus.WAITING_FOR_FEED, engine.state.value.status)

        env.screen = ScreenCheck.IN_FEED
        advanceTimeBy(10_000)
        assertTrue(env.swipeTimes.isNotEmpty())
        engine.stop()

        // TikTok has no separate feed screen to confirm: being in the app is enough.
        env.swipeTimes.clear()
        env.screen = ScreenCheck.IN_APP
        engine.start(Platform.TIKTOK, settings)
        advanceTimeBy(10_000)
        assertTrue(env.swipeTimes.isNotEmpty())
        engine.stop()
    }

    @Test
    fun switchingPlatformsTikTokYouTubeTikTok() = runTest {
        val (engine, env) = newEngine()
        for (platform in listOf(Platform.TIKTOK, Platform.YOUTUBE_SHORTS, Platform.TIKTOK)) {
            env.checkedPlatforms.clear()
            env.swipeTimes.clear()
            assertTrue(engine.start(platform, settings))
            assertEquals(platform, engine.state.value.platform)
            assertEquals(0, engine.state.value.videosViewed)
            advanceTimeBy(15_000)
            assertEquals(setOf(platform), env.checkedPlatforms)
            assertTrue(env.swipeTimes.isNotEmpty())
            engine.stop()
            assertFalse(engine.isRunning)
        }
    }

    @Test
    fun cannotStartTwice() = runTest {
        val (engine, _) = newEngine()
        assertTrue(engine.start(Platform.TIKTOK, settings))
        assertFalse(engine.start(Platform.YOUTUBE_SHORTS, settings))
        assertEquals(Platform.TIKTOK, engine.state.value.platform)
        engine.stop()
    }

    @Test
    fun settingsAreNormalized() {
        val s = SessionSettings(minWatchSeconds = 50, maxWatchSeconds = 10, maxVideos = 0).normalized()
        assertEquals(50, s.minWatchSeconds)
        assertEquals(50, s.maxWatchSeconds)
        assertEquals(1, s.maxVideos)
    }
}
