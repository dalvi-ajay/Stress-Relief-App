package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.DefaultReliefContent
import com.example.ui.UnwindMode
import com.example.ui.UnwindViewModel
import com.example.ui.WorryBubble
import kotlin.math.sin

private data class PondRipple(
    val id: Long,
    val center: Offset,
    val startTimeMs: Long,
)

@Composable
fun UnwindScreen(
    viewModel: UnwindViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.unwind_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.unwind_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(14.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                UnwindMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.mode == mode,
                        onClick = { viewModel.selectMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = UnwindMode.entries.size,
                        ),
                        modifier = Modifier.testTag("unwind_mode_${mode.name.lowercase()}"),
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (state.mode) {
                UnwindMode.POND -> {
                    StillPondCanvasSection(
                        rippleCount = state.rippleCount,
                        onSpawnRipple = viewModel::incrementRipples,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
                UnwindMode.BUBBLES -> {
                    WorryPopSection(
                        bubbles = state.bubbles,
                        poppedCount = state.poppedCount,
                        lastReleasedLabel = state.lastReleasedLabel,
                        onPopBubble = viewModel::popBubble,
                        onRefill = viewModel::refillBubbles,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
                UnwindMode.PROMPTS -> {
                    KindPromptsSection(
                        promptIndex = state.promptIndex,
                        onNextPrompt = viewModel::nextPrompt,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun StillPondCanvasSection(
    rippleCount: Int,
    onSpawnRipple: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ripples = remember { mutableStateListOf<PondRipple>() }
    var frameTimeMs by remember { mutableLongStateOf(0L) }
    var nextRippleId by remember { mutableLongStateOf(0L) }
    val haptic = LocalHapticFeedback.current
    val rippleColor = MaterialTheme.colorScheme.primary
    val secondaryRippleColor = MaterialTheme.colorScheme.secondary
    val pondTop = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val pondBottom = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)

    val hasActiveRipples = ripples.isNotEmpty()
    LaunchedEffect(hasActiveRipples) {
        if (!hasActiveRipples) return@LaunchedEffect
        while (ripples.isNotEmpty()) {
            withFrameMillis { nowMs ->
                frameTimeMs = nowMs
                for (i in ripples.indices) {
                    if (ripples[i].startTimeMs < 0L) {
                        ripples[i] = ripples[i].copy(startTimeMs = nowMs)
                    }
                }
                ripples.removeAll { it.startTimeMs >= 0L && nowMs - it.startTimeMs > 2600L }
            }
        }
    }

    Surface(
        modifier = modifier
            .padding(bottom = 10.dp)
            .testTag("still_pond_surface"),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(pondTop, pondBottom)))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        ripples.add(
                            PondRipple(
                                id = nextRippleId++,
                                center = offset,
                                startTimeMs = -1L,
                            ),
                        )
                        onSpawnRipple()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val now = frameTimeMs
                ripples.forEach { ripple ->
                    if (ripple.startTimeMs < 0L) return@forEach
                    val ageMs = (now - ripple.startTimeMs).coerceAtLeast(0L)
                    val progress = (ageMs / 2600f).coerceIn(0f, 1f)

                    // Draw 3 concentric ripples per tap
                    for (ringIndex in 0 until 3) {
                        val delayedProgress = ((progress - ringIndex * 0.14f) / 0.72f).coerceIn(0f, 1f)
                        if (delayedProgress > 0f) {
                            val radius = 14.dp.toPx() + delayedProgress * 150.dp.toPx()
                            val alpha = (1f - delayedProgress) * (0.48f - ringIndex * 0.11f)
                            val strokeColor = if (ringIndex % 2 == 0) rippleColor else secondaryRippleColor
                            drawCircle(
                                color = strokeColor.copy(alpha = alpha.coerceAtLeast(0f)),
                                radius = radius,
                                center = ripple.center,
                                style = Stroke(width = (3.dp.toPx() * (1f - delayedProgress * 0.5f))),
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (rippleCount == 0) {
                                stringResource(R.string.pond_hint)
                            } else {
                                "$rippleCount calm ripples sent"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorryPopSection(
    bubbles: List<WorryBubble>,
    poppedCount: Int,
    lastReleasedLabel: String?,
    onPopBubble: (Int) -> Unit,
    onRefill: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "bubble_float")
    val floatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bubble_phase",
    )

    val allPopped = bubbles.all { it.isPopped }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (lastReleasedLabel != null) {
                "Released \"$lastReleasedLabel\" • $poppedCount let go"
            } else {
                stringResource(R.string.bubbles_hint)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(18.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            bubbles.forEachIndexed { idx, bubble ->
                val yOffset = (sin(floatPhase + idx * 0.9f) * 5f).dp
                AnimatedVisibility(
                    visible = !bubble.isPopped,
                    enter = fadeIn(),
                    exit = fadeOut(tween(220)) + scaleOut(tween(220)),
                ) {
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPopBubble(bubble.id)
                        },
                        modifier = Modifier
                            .offset(y = yOffset)
                            .minimumInteractiveComponentSize()
                            .testTag("worry_bubble_${bubble.id}"),
                        shape = RoundedCornerShape(28.dp),
                        color = if (idx % 2 == 0) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                        },
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                        ),
                        tonalElevation = 2.dp,
                    ) {
                        Text(
                            text = bubble.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (allPopped || poppedCount > 0) {
            FilledTonalButton(
                onClick = onRefill,
                modifier = Modifier.testTag("bubbles_refill_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.bubbles_refill))
            }
        }
    }
}

@Composable
private fun KindPromptsSection(
    promptIndex: Int,
    onNextPrompt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prompts = DefaultReliefContent.kindPrompts
    val currentPrompt = prompts[promptIndex % prompts.size]

    Column(
        modifier = modifier.padding(bottom = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("kind_prompt_card"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = currentPrompt,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "${promptIndex + 1} / ${prompts.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onNextPrompt,
            modifier = Modifier
                .height(50.dp)
                .testTag("next_kind_prompt_button"),
            shape = RoundedCornerShape(25.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            Text(stringResource(R.string.affirmation_next))
        }
    }
}
