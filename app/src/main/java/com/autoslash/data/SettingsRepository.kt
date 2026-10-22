package com.autoslash.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autoslash.domain.Platform
import com.autoslash.domain.SessionSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "autoslash_settings")

/** Persists the session settings and the selected platform locally on the device. */
class SettingsRepository(private val context: Context) {

    val settings: Flow<SessionSettings> = context.dataStore.data.map { prefs ->
        val defaults = SessionSettings()
        SessionSettings(
            minWatchSeconds = prefs[MIN_WATCH] ?: defaults.minWatchSeconds,
            maxWatchSeconds = prefs[MAX_WATCH] ?: defaults.maxWatchSeconds,
            sessionDurationMinutes = prefs[DURATION] ?: defaults.sessionDurationMinutes,
            actionPauseMillis = prefs[PAUSE] ?: defaults.actionPauseMillis,
            maxVideos = prefs[MAX_VIDEOS] ?: defaults.maxVideos,
        ).normalized()
    }

    val platform: Flow<Platform> = context.dataStore.data.map { Platform.fromName(it[PLATFORM]) }

    suspend fun saveSettings(settings: SessionSettings) {
        val s = settings.normalized()
        context.dataStore.edit { prefs ->
            prefs[MIN_WATCH] = s.minWatchSeconds
            prefs[MAX_WATCH] = s.maxWatchSeconds
            prefs[DURATION] = s.sessionDurationMinutes
            prefs[PAUSE] = s.actionPauseMillis
            prefs[MAX_VIDEOS] = s.maxVideos
        }
    }

    suspend fun savePlatform(platform: Platform) {
        context.dataStore.edit { it[PLATFORM] = platform.name }
    }

    private companion object {
        val MIN_WATCH = intPreferencesKey("min_watch_seconds")
        val MAX_WATCH = intPreferencesKey("max_watch_seconds")
        val DURATION = intPreferencesKey("session_duration_minutes")
        val PAUSE = intPreferencesKey("action_pause_millis")
        val MAX_VIDEOS = intPreferencesKey("max_videos")
        val PLATFORM = stringPreferencesKey("platform")
    }
}
