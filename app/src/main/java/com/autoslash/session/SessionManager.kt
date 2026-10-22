package com.autoslash.session

import com.autoslash.domain.Platform
import com.autoslash.domain.SessionSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Process-wide bridge between the UI and the accessibility service.
 *
 * The [SessionEngine] lives inside the accessibility service (the only component allowed to
 * dispatch gestures). When the service is disabled or killed the engine is detached and
 * stopped, so a session can never outlive the service.
 */
object SessionManager {
    private val engine = MutableStateFlow<SessionEngine?>(null)

    val isServiceConnected: StateFlow<Boolean> get() = _serviceConnected.asStateFlow()
    private val _serviceConnected = MutableStateFlow(false)

    /** Last state seen, kept so the UI can still show results after the service went away. */
    private val lastState = MutableStateFlow(SessionState())

    @OptIn(ExperimentalCoroutinesApi::class)
    fun state(scope: CoroutineScope): StateFlow<SessionState> =
        engine.flatMapLatest { it?.state ?: lastState }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), engine.value?.state?.value ?: lastState.value)

    fun attach(sessionEngine: SessionEngine) {
        engine.value?.stop(FinishReason.SERVICE_DISCONNECTED)
        engine.value = sessionEngine
        _serviceConnected.value = true
    }

    fun detach(sessionEngine: SessionEngine) {
        if (engine.value !== sessionEngine) return
        sessionEngine.stop(FinishReason.SERVICE_DISCONNECTED)
        lastState.value = sessionEngine.state.value
        engine.value = null
        _serviceConnected.value = false
    }

    /** Returns false when the accessibility service is not enabled or a session is already running. */
    fun start(platform: Platform, settings: SessionSettings): Boolean =
        engine.value?.start(platform, settings) ?: false

    fun stop() {
        engine.value?.stop(FinishReason.USER_STOP)
    }

    fun reset() {
        engine.value?.reset()
        lastState.value = SessionState()
    }
}
