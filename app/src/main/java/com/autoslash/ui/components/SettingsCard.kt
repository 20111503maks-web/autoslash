package com.autoslash.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.SwipeUp
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.autoslash.R
import com.autoslash.domain.SessionSettings
import com.autoslash.ui.theme.AppColors
import com.autoslash.ui.theme.PlatformAccent
import kotlin.math.roundToInt

private const val MAX_WATCH_SLIDER = 60
private const val MAX_DURATION_SLIDER = 120
private const val MAX_VIDEOS_SLIDER = 500

@Composable
fun SettingsCard(
    settings: SessionSettings,
    accent: PlatformAccent,
    enabled: Boolean,
    onChange: ((SessionSettings) -> SessionSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderColors = SliderDefaults.colors(
        thumbColor = accent.primary,
        activeTrackColor = accent.primary,
        inactiveTrackColor = AppColors.Outline,
    )
    var advanced by rememberSaveable { mutableStateOf(false) }

    AppCard(modifier) {
        SettingHeader(
            icon = Icons.Rounded.Timer,
            title = stringResource(R.string.setting_session_duration),
            value = stringResource(R.string.value_minutes, settings.sessionDurationMinutes),
        )
        Slider(
            value = settings.sessionDurationMinutes.toFloat(),
            onValueChange = { v -> onChange { it.copy(sessionDurationMinutes = v.roundToInt()) } },
            valueRange = 1f..MAX_DURATION_SLIDER.toFloat(),
            enabled = enabled,
            colors = sliderColors,
        )

        Spacer(Modifier.height(8.dp))
        SettingHeader(
            icon = Icons.Rounded.SwipeUp,
            title = stringResource(R.string.setting_swipe_interval),
            value = stringResource(R.string.value_seconds_range, settings.minWatchSeconds, settings.maxWatchSeconds),
        )
        Text(
            stringResource(R.string.setting_swipe_interval_hint),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary,
        )
        RangeSlider(
            value = settings.minWatchSeconds.coerceAtMost(MAX_WATCH_SLIDER).toFloat()..
                settings.maxWatchSeconds.coerceAtMost(MAX_WATCH_SLIDER).toFloat(),
            onValueChange = { range ->
                onChange { it.copy(minWatchSeconds = range.start.roundToInt(), maxWatchSeconds = range.endInclusive.roundToInt()) }
            },
            valueRange = 2f..MAX_WATCH_SLIDER.toFloat(),
            enabled = enabled,
            colors = sliderColors,
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = AppColors.Outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { advanced = !advanced }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Tune, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
            Text(
                stringResource(R.string.setting_advanced),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (advanced) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = AppColors.TextSecondary,
            )
        }
        AnimatedVisibility(visible = advanced) {
            Column {
                Spacer(Modifier.height(8.dp))
                SettingHeader(
                    icon = Icons.Rounded.PauseCircle,
                    title = stringResource(R.string.setting_action_pause),
                    value = stringResource(R.string.value_seconds_decimal, settings.actionPauseMillis / 1000f),
                )
                Slider(
                    value = settings.actionPauseMillis.toFloat(),
                    onValueChange = { v -> onChange { it.copy(actionPauseMillis = (v / 100).roundToInt() * 100) } },
                    valueRange = SessionSettings.PAUSE_RANGE.first.toFloat()..3_000f,
                    enabled = enabled,
                    colors = sliderColors,
                )
                SettingHeader(
                    icon = Icons.Rounded.Movie,
                    title = stringResource(R.string.setting_max_videos),
                    value = settings.maxVideos.toString(),
                )
                Slider(
                    value = settings.maxVideos.coerceAtMost(MAX_VIDEOS_SLIDER).toFloat(),
                    onValueChange = { v -> onChange { it.copy(maxVideos = (v / 5).roundToInt().coerceAtLeast(1) * 5) } },
                    valueRange = 5f..MAX_VIDEOS_SLIDER.toFloat(),
                    enabled = enabled,
                    colors = sliderColors,
                )
            }
        }
    }
}

@Composable
private fun SettingHeader(icon: ImageVector, title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = AppColors.TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = AppColors.TextPrimary)
    }
}
