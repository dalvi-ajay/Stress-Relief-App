package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.BreathingPattern
import com.example.data.BreathingPhase
import com.example.ui.BreathingUiState
import com.example.ui.BreathingViewModel
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun BreathingScreen(
    viewModel: BreathingViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(viewModel) {
        viewModel.phaseHapticEvents.collect { phase ->
            when (phase) {
                BreathingPhase.INHALE,
                BreathingPhase.COMPLETE -> hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                BreathingPhase.HOLD,
                BreathingPhase.EXHALE,
                BreathingPhase.REST -> hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                BreathingPhase.READY -> Unit
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        val isWide = maxWidth >= 700.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    BreathingCircleStage(
                        state = state,
                        onTogglePlayPause = viewModel::togglePlayPause,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    BreathingControlsRow(
                        state = state,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onReset = viewModel::resetSession,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    BreathingHeader(
                        hapticsEnabled = state.hapticsEnabled,
                        onToggleHaptics = viewModel::toggleHaptics,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SessionCompletionBanner(
                        visible = state.phase == BreathingPhase.COMPLETE,
                        cycles = state.completedCycles,
                    )
                    BreathingSettingsSection(
                        state = state,
                        onSelectPattern = viewModel::selectPattern,
                        onSelectMinutes = viewModel::selectSessionMinutes,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 560.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BreathingHeader(
                        hapticsEnabled = state.hapticsEnabled,
                        onToggleHaptics = viewModel::toggleHaptics,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SessionCompletionBanner(
                        visible = state.phase == BreathingPhase.COMPLETE,
                        cycles = state.completedCycles,
                    )

                    BreathingCircleStage(
                        state = state,
                        onTogglePlayPause = viewModel::togglePlayPause,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    BreathingControlsRow(
                        state = state,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onReset = viewModel::resetSession,
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    BreathingSettingsSection(
                        state = state,
                        onSelectPattern = viewModel::selectPattern,
                        onSelectMinutes = viewModel::selectSessionMinutes,
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun BreathingHeader(
    hapticsEnabled: Boolean,
    onToggleHaptics: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.breathe_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.breathe_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        FilterChip(
            selected = hapticsEnabled,
            onClick = onToggleHaptics,
            label = {
                Text(
                    text = stringResource(if (hapticsEnabled) R.string.haptics_on else R.string.haptics_off),
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Vibration,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("breathe_haptics_chip"),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
    }
}

@Composable
private fun BreathingCircleStage(
    state: BreathingUiState,
    onTogglePlayPause: () -> Unit,
) {
    val phaseColorTarget = when (state.phase) {
        BreathingPhase.INHALE -> MaterialTheme.colorScheme.primary
        BreathingPhase.HOLD -> MaterialTheme.colorScheme.tertiary
        BreathingPhase.EXHALE -> MaterialTheme.colorScheme.secondary
        BreathingPhase.REST -> MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
        BreathingPhase.COMPLETE -> MaterialTheme.colorScheme.primary
        BreathingPhase.READY -> MaterialTheme.colorScheme.primary
    }
    val activeColor by animateColorAsState(
        targetValue = phaseColorTarget,
        animationSpec = tween(durationMillis = 450),
        label = "breath_phase_color",
    )

    val phaseText = when (state.phase) {
        BreathingPhase.READY -> stringResource(R.string.breathe_ready)
        BreathingPhase.INHALE -> stringResource(R.string.breathe_phase_inhale)
        BreathingPhase.HOLD -> stringResource(R.string.breathe_phase_hold)
        BreathingPhase.EXHALE -> stringResource(R.string.breathe_phase_exhale)
        BreathingPhase.REST -> stringResource(R.string.breathe_phase_rest)
        BreathingPhase.COMPLETE -> stringResource(R.string.breathe_session_complete_title)
    }

    val holdRipple = if (state.phase == BreathingPhase.HOLD) {
        0.025f * sin(state.phaseProgress * 2f * PI.toFloat())
    } else {
        0f
    }
    val expansion = (state.circleExpansion + holdRipple).coerceIn(0f, 1.05f)

    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val centerTextColor = MaterialTheme.colorScheme.onPrimary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .sizeIn(maxWidth = 260.dp, maxHeight = 260.dp)
                .fillMaxWidth(0.78f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .clickable(onClick = onTogglePlayPause)
                .testTag("breathing_circle_canvas"),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension * 0.45f
                val minRadius = maxRadius * 0.42f

                // Outer guide track circle
                drawCircle(
                    color = trackColor,
                    radius = maxRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )

                // Phase progress arc on the outer guide ring
                if (state.isRunning || state.phase == BreathingPhase.COMPLETE) {
                    val arcTopLeft = Offset(center.x - maxRadius, center.y - maxRadius)
                    drawArc(
                        color = activeColor.copy(alpha = 0.85f),
                        startAngle = -90f,
                        sweepAngle = 360f * state.phaseProgress.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = Size(maxRadius * 2f, maxRadius * 2f),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                // Expanding / contracting aura ring 1 (outermost soft halo)
                val haloRadius = minRadius + (maxRadius * 1.04f - minRadius) * expansion
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.24f),
                            activeColor.copy(alpha = 0.04f),
                        ),
                        center = center,
                        radius = haloRadius.coerceAtLeast(1f),
                    ),
                    radius = haloRadius,
                    center = center,
                )

                // Expanding / contracting mid circle
                val midRadius = minRadius * 0.96f + (maxRadius * 0.88f - minRadius * 0.96f) * expansion
                drawCircle(
                    color = activeColor.copy(alpha = 0.26f),
                    radius = midRadius,
                    center = center,
                )

                // Expanding / contracting inner ring stroke
                val ringRadius = minRadius * 0.92f + (maxRadius * 0.78f - minRadius * 0.92f) * expansion
                drawCircle(
                    color = surfaceColor.copy(alpha = 0.35f),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )

                // Expanding / contracting solid core circle
                val coreRadius = minRadius * 0.86f + (maxRadius * 0.68f - minRadius * 0.86f) * expansion
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            activeColor,
                            activeColor.copy(alpha = 0.86f),
                        ),
                        center = center,
                        radius = coreRadius.coerceAtLeast(1f),
                    ),
                    radius = coreRadius,
                    center = center,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = phaseText,
                    style = MaterialTheme.typography.titleMedium,
                    color = centerTextColor,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (state.isRunning || (state.phase != BreathingPhase.READY && state.phase != BreathingPhase.COMPLETE)) {
                    Text(
                        text = "${state.phaseSecondsRemaining}",
                        style = MaterialTheme.typography.displayMedium,
                        color = centerTextColor,
                    )
                } else {
                    Text(
                        text = state.pattern.cadenceLabel,
                        style = MaterialTheme.typography.titleLarge,
                        color = centerTextColor.copy(alpha = 0.95f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live metrics row
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(50),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${stringResource(R.string.breathe_cycles_label)}: ${state.completedCycles}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = formatSeconds(state.totalSecondsRemaining),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun BreathingControlsRow(
    state: BreathingUiState,
    onTogglePlayPause: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val primaryLabel = when {
            state.isRunning -> stringResource(R.string.breathe_pause)
            state.phase == BreathingPhase.READY || state.phase == BreathingPhase.COMPLETE ->
                stringResource(R.string.breathe_start)
            else -> stringResource(R.string.breathe_resume)
        }
        Button(
            onClick = onTogglePlayPause,
            modifier = Modifier
                .height(52.dp)
                .testTag("breathe_play_pause_button"),
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(horizontal = 28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                imageVector = if (state.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.labelLarge,
            )
        }

        OutlinedIconButton(
            onClick = onReset,
            modifier = Modifier
                .size(52.dp)
                .testTag("breathe_reset_button"),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.breathe_reset),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SessionCompletionBanner(
    visible: Boolean,
    cycles: Int,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${stringResource(R.string.breathe_session_complete_title)} ($cycles cycles)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.breathe_session_complete_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BreathingSettingsSection(
    state: BreathingUiState,
    onSelectPattern: (BreathingPattern) -> Unit,
    onSelectMinutes: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.breathe_pattern_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(BreathingPattern.entries) { pattern ->
                val selected = pattern == state.pattern
                Surface(
                    onClick = { onSelectPattern(pattern) },
                    modifier = Modifier
                        .width(172.dp)
                        .testTag("pattern_${pattern.name.lowercase()}"),
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Air,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Text(
                                text = pattern.cadenceLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pattern.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pattern.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.breathe_duration_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(1, 3, 5).forEach { minutes ->
                val selected = state.sessionMinutes == minutes
                FilterChip(
                    selected = selected,
                    onClick = { onSelectMinutes(minutes) },
                    label = {
                        Text(
                            text = "$minutes min",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("duration_${minutes}m"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                )
            }
        }
    }
}

private fun formatSeconds(totalSeconds: Int): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val mins = clamped / 60
    val secs = clamped % 60
    return "%d:%02d".format(mins, secs)
}
