package com.autoslash.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.autoslash.R
import com.autoslash.domain.Platform
import com.autoslash.session.SessionState
import com.autoslash.session.SessionStatus
import com.autoslash.session.formatClock
import com.autoslash.ui.labelRes
import com.autoslash.ui.theme.AppColors
import com.autoslash.ui.theme.accent

fun SessionStatus.color(): Color = when (this) {
    SessionStatus.WATCHING -> AppColors.Success
    SessionStatus.SWIPING -> AppColors.TikTokCyan
    SessionStatus.WAITING_FOR_APP, SessionStatus.WAITING_FOR_FEED -> AppColors.Warning
    SessionStatus.PAUSED_OUTSIDE_APP, SessionStatus.PAUSED_BY_USER -> AppColors.Paused
    else -> AppColors.TextSecondary
}

/** Live statistics of the running (or just finished) session. */
@Composable
fun SessionPanel(state: SessionState, modifier: Modifier = Modifier) {
    val platform = state.platform ?: Platform.DEFAULT
    val accent = platform.accent

    AppCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.brush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(platform.icon, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.label_platform), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                Text(platform.displayName, style = MaterialTheme.typography.titleMedium)
            }
            StatusChip(state.status)
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.label_session_time), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        Text(formatClock(state.elapsedMillis), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.label_of_left, formatClock(state.durationMillis), formatClock(state.remainingMillis)),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        Progress(
            fraction = if (state.durationMillis > 0) state.elapsedMillis.toFloat() / state.durationMillis else 0f,
            color = accent.primary,
        )

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(
                label = stringResource(R.string.label_videos_viewed),
                value = state.videosViewed.toString(),
                sub = stringResource(R.string.label_videos_left, state.maxVideos, state.remainingVideos),
                modifier = Modifier.weight(1f),
            )
            Stat(
                label = stringResource(R.string.label_this_video),
                value = if (state.currentWatchMillis > 0) "${state.currentWatchMillis / 1000}s" else "—",
                sub = stringResource(R.string.label_random_watch),
                modifier = Modifier.weight(1f),
            )
            Stat(
                label = stringResource(R.string.label_likes),
                value = state.likes.toString(),
                sub = stringResource(R.string.label_likes_hint),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Progress(
            fraction = if (state.maxVideos > 0) state.videosViewed.toFloat() / state.maxVideos else 0f,
            color = accent.secondary,
        )

        state.finishReason?.let { reason ->
            Spacer(Modifier.height(18.dp))
            Text(stringResource(reason.labelRes()), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceHigh)
            .padding(14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.headlineLarge)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
    }
}

@Composable
private fun Progress(fraction: Float, color: Color) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape),
        color = color,
        trackColor = AppColors.Outline,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
fun StatusChip(status: SessionStatus) {
    val color by animateColorAsState(status.color(), label = "statusColor")
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    Row(
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .alpha(if (status.isActive) pulse else 1f)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.size(6.dp))
        Text(stringResource(status.labelRes()), style = MaterialTheme.typography.labelMedium, color = color)
    }
}
