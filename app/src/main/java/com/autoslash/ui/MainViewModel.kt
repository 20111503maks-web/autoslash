package com.autoslash.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoslash.data.SettingsRepository
import com.autoslash.domain.Platform
import com.autoslash.domain.SessionSettings
import com.autoslash.platform.AccessibilityStatus
import com.autoslash.platform.LaunchResult
import com.autoslash.platform.PlatformLauncher
import com.autoslash.session.SessionManager
import com.autoslash.session.SessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** One-off messages for the UI (snackbar / toast). */
sealed interface UiEvent {
    data object EnableAccessibility : UiEvent
    data class NotInstalled(val platform: Platform) : UiEvent
    data class Launched(val platform: Platform, val feedOpenedDirectly: Boolean) : UiEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val launcher = PlatformLauncher(application)

    /** Edited in memory for smooth sliders; persisted shortly after the user stops dragging. */
    private val _settings = MutableStateFlow(SessionSettings())
    val settings: StateFlow<SessionSettings> = _settings.asStateFlow()
    private var saveJob: Job? = null

    private val _platform = MutableStateFlow(Platform.DEFAULT)
    val platform: StateFlow<Platform> = _platform.asStateFlow()

    val session: StateFlow<SessionState> = SessionManager.state(viewModelScope)

    private val _accessibilityEnabled = MutableStateFlow(false)
    val accessibilityEnabled: StateFlow<Boolean> = _accessibilityEnabled.asStateFlow()

    private val _events = MutableStateFlow<UiEvent?>(null)
    val events: StateFlow<UiEvent?> = _events.asStateFlow()

    init {
        viewModelScope.launch {
            _settings.value = repository.settings.first()
            _platform.value = repository.platform.first()
        }
    }

    fun refreshAccessibility() {
        _accessibilityEnabled.value = AccessibilityStatus.isEnabled(getApplication())
    }

    fun selectPlatform(platform: Platform) {
        if (session.value.isActive) return
        _platform.value = platform
        viewModelScope.launch { repository.savePlatform(platform) }
    }

    fun updateSettings(transform: (SessionSettings) -> SessionSettings) {
        val updated = transform(_settings.value).normalized()
        _settings.value = updated
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(300)
            repository.saveSettings(updated)
        }
    }

    fun isInstalled(platform: Platform) = launcher.installedPackage(platform) != null

    fun startSession() {
        refreshAccessibility()
        if (!_accessibilityEnabled.value || !SessionManager.isServiceConnected.value) {
            _events.value = UiEvent.EnableAccessibility
            return
        }
        viewModelScope.launch {
            val target = _platform.value
            val config = _settings.value
            when (val result = launcher.launch(target)) {
                LaunchResult.NotInstalled -> _events.value = UiEvent.NotInstalled(target)
                is LaunchResult.Opened -> {
                    if (SessionManager.start(target, config)) {
                        _events.value = UiEvent.Launched(target, result.feedOpenedDirectly)
                    }
                }
            }
        }
    }

    fun stopSession() = SessionManager.stop()

    fun dismissResult() = SessionManager.reset()

    fun reopenApp() {
        session.value.platform?.let(launcher::launch)
    }

    fun consumeEvent() {
        _events.value = null
    }
}
