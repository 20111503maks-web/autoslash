package com.autoslash.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.autoslash.R
import com.autoslash.domain.Platform
import com.autoslash.ui.theme.AppColors
import com.autoslash.ui.theme.accent

val Platform.icon: ImageVector
    get() = when (this) {
        Platform.TIKTOK -> Icons.Rounded.MusicNote
        Platform.YOUTUBE_SHORTS -> Icons.Rounded.PlayArrow
    }

@Composable
fun PlatformSelector(
    selected: Platform,
    enabled: Boolean,
    isInstalled: (Platform) -> Boolean,
    onSelect: (Platform) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Platform.entries.forEach { platform ->
            PlatformCard(
                platform = platform,
                selected = platform == selected,
                installed = isInstalled(platform),
                enabled = enabled,
                onClick = { onSelect(platform) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PlatformCard(
    platform: Platform,
    selected: Boolean,
    installed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = platform.accent
    val scale by animateFloatAsState(if (selected) 1f else 0.96f, label = "cardScale")
    val shape = RoundedCornerShape(24.dp)
    val borderBrush: Brush = if (selected) accent.brush else SolidColor(AppColors.Outline)
    val background = if (selected) {
        Brush.verticalGradient(listOf(accent.primary.copy(alpha = 0.22f), AppColors.Surface))
    } else {
        SolidColor(AppColors.Surface)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(background)
            .border(if (selected) 2.dp else 1.dp, borderBrush, shape)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(16.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) accent.brush else SolidColor(AppColors.SurfaceHigh)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(platform.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(platform.displayName, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (installed) AppColors.Success else AppColors.Warning),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    stringResource(if (installed) R.string.platform_installed else R.string.platform_not_installed),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                )
            }
        }
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = accent.primary)
        }
    }
}
