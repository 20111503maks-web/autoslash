package com.autoslash.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoslash.R
import com.autoslash.domain.Platform
import com.autoslash.domain.SessionSettings
import com.autoslash.platform.AccessibilityStatus
import com.autoslash.session.SessionState
import com.autoslash.session.SessionStatus
import com.autoslash.ui.components.GradientButton
import com.autoslash.ui.components.PlatformSelector
import com.autoslash.ui.components.SectionLabel
import com.autoslash.ui.components.SessionPanel
import com.autoslash.ui.components.SettingsCard
import com.autoslash.ui.theme.AppColors
import com.autoslash.ui.theme.accent

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val platform by viewModel.platform.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val accessibilityEnabled by viewModel.accessibilityEnabled.collectAsStateWithLifecycle()
    val event by viewModel.events.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The page glow follows the selected (or running) platform.
    val glowPlatform = session.platform.takeIf { session.status != SessionStatus.IDLE } ?: platform
    Box(
        Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .background(
                Brush.radialGradient(
                    colors = listOf(glowPlatform.accent.primary.copy(alpha = 0.18f), Color.Transparent),
                    radius = 1400f,
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Header()
            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(visible = !accessibilityEnabled) {
                AccessibilityBanner(onEnable = { context.startActivity(AccessibilityStatus.settingsIntent()) })
            }

            AnimatedContent(
                targetState = session.status != SessionStatus.IDLE,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen",
            ) { inSession ->
                if (inSession) {
                    SessionContent(
                        state = session,
                        onStop = viewModel::stopSession,
                        onTogglePause = viewModel::togglePause,
                        onReopen = viewModel::reopenApp,
                        onDone = viewModel::dismissResult,
                    )
                } else {
                    SetupContent(viewModel = viewModel, platform = platform, settings = settings)
                }
            }

            Spacer(Modifier.height(20.dp))
            PrivacyNote()
        }
    }

    event?.let { current ->
        when (current) {
            UiEvent.EnableAccessibility -> AlertDialog(
                onDismissRequest = viewModel::consumeEvent,
                icon = { Icon(Icons.Rounded.Accessibility, contentDescription = null) },
                title = { Text(stringResource(R.string.dialog_accessibility_title)) },
                text = { Text(stringResource(R.string.dialog_accessibility_text)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.consumeEvent()
                        context.startActivity(AccessibilityStatus.settingsIntent())
                    }) { Text(stringResource(R.string.action_open_settings)) }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::consumeEvent) { Text(stringResource(R.string.action_cancel)) }
                },
            )
            else -> LaunchedEffect(current) {
                val message = when (current) {
                    is UiEvent.NotInstalled -> context.getString(R.string.toast_not_installed, current.platform.displayName)
                    is UiEvent.Launched -> context.getString(launchMessage(current))
                    UiEvent.EnableAccessibility -> null
                }
                message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                viewModel.consumeEvent()
            }
        }
    }
}

private fun launchMessage(event: UiEvent.Launched): Int = when (event.platform) {
    Platform.TIKTOK -> R.string.toast_tiktok_opened
    Platform.YOUTUBE_SHORTS ->
        if (event.feedOpenedDirectly) R.string.toast_youtube_shorts_opened else R.string.toast_youtube_go_to_shorts
}

@Composable
private fun Header() {
    Column {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
    }
}

@Composable
private fun SetupContent(
    viewModel: MainViewModel,
    platform: Platform,
    settings: SessionSettings,
) {
    Column {
        SectionLabel(stringResource(R.string.choose_platform))
        PlatformSelector(
            selected = platform,
            enabled = true,
            isInstalled = viewModel::isInstalled,
            onSelect = viewModel::selectPlatform,
        )

        if (platform == Platform.YOUTUBE_SHORTS) {
            Spacer(Modifier.height(12.dp))
            Hint(Icons.Rounded.Info, stringResource(R.string.hint_youtube_shorts))
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.section_settings))
        SettingsCard(
            settings = settings,
            accent = platform.accent,
            enabled = true,
            onChange = viewModel::updateSettings,
        )

        Spacer(Modifier.height(24.dp))
        GradientButton(
            text = stringResource(R.string.action_start_session),
            icon = Icons.Rounded.PlayArrow,
            brush = platform.accent.brush,
            onClick = viewModel::startSession,
        )
    }
}

@Composable
private fun SessionContent(
    state: SessionState,
    onStop: () -> Unit,
    onTogglePause: () -> Unit,
    onReopen: () -> Unit,
    onDone: () -> Unit,
) {
    Column {
        SessionPanel(state)
        if (state.status == SessionStatus.WAITING_FOR_FEED) {
            Spacer(Modifier.height(12.dp))
            Hint(Icons.Rounded.Info, stringResource(R.string.hint_waiting_for_shorts))
        }
        Spacer(Modifier.height(24.dp))
        if (state.isActive) {
            val paused = state.status == SessionStatus.PAUSED_BY_USER
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onTogglePause,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(if (paused) R.string.action_resume else R.string.action_pause))
                }
                OutlinedButton(
                    onClick = onReopen,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(
                        stringResource(R.string.action_open_platform, (state.platform ?: Platform.DEFAULT).displayName),
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            GradientButton(
                text = stringResource(R.string.action_stop),
                icon = Icons.Rounded.Stop,
                brush = Brush.linearGradient(listOf(AppColors.Danger, Color(0xFFB0203A))),
                onClick = onStop,
            )
        } else {
            GradientButton(
                text = stringResource(R.string.action_new_session),
                icon = Icons.Rounded.Refresh,
                brush = (state.platform ?: Platform.DEFAULT).accent.brush,
                onClick = onDone,
            )
        }
    }
}

@Composable
private fun AccessibilityBanner(onEnable: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Warning.copy(alpha = 0.12f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Accessibility, contentDescription = null, tint = AppColors.Warning)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.banner_accessibility_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.banner_accessibility_text),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
            )
        }
        TextButton(onClick = onEnable) { Text(stringResource(R.string.action_enable), color = AppColors.Warning) }
    }
}

@Composable
private fun Hint(icon: ImageVector, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceHigh)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
    }
}

@Composable
private fun PrivacyNote() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Shield, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.size(6.dp))
        Text(stringResource(R.string.privacy_note), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
    }
}
