package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ai.CalmVoiceController
import com.example.data.BreathingPattern
import com.example.ui.VoiceAssistantViewModel
import com.example.ui.VoiceMessage

private val QuickVoicePrompts = listOf(
    "I feel overwhelmed right now",
    "Guide me through a 1-minute calm reset",
    "Help me relax my shoulders",
    "Suggest a breathing pace for sleep",
)

@Composable
fun VoiceAssistantScreen(
    viewModel: VoiceAssistantViewModel,
    onOpenBreathingPattern: (BreathingPattern) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var micPermissionDeniedOnce by remember { mutableStateOf(false) }

    val voiceController = remember(context) {
        CalmVoiceController(
            context = context,
            onPartialResult = viewModel::onPartialTranscript,
            onFinalResult = viewModel::sendVoiceTranscript,
            onListeningStateChanged = viewModel::onListeningChanged,
            onSpeechError = viewModel::onVoiceError,
            onSpeakingStateChanged = viewModel::onSpeakingChanged,
        )
    }

    DisposableEffect(voiceController) {
        onDispose {
            voiceController.release()
        }
    }

    LaunchedEffect(viewModel, voiceController) {
        viewModel.speakEvents.collect { reply ->
            voiceController.speak(reply)
        }
    }

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.text?.length) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            micPermissionDeniedOnce = false
            voiceController.startListening()
        } else {
            micPermissionDeniedOnce = true
        }
    }

    fun handleMicTap() {
        when {
            state.isSpeaking -> voiceController.stopSpeaking()
            state.isListening -> voiceController.stopListening()
            hasMicPermission -> voiceController.startListening()
            else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

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
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.voice_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.voice_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = state.autoSpeakEnabled,
                        onClick = {
                            if (state.isSpeaking) voiceController.stopSpeaking()
                            viewModel.toggleAutoSpeak()
                        },
                        label = {
                            Text(
                                text = stringResource(
                                    if (state.autoSpeakEnabled) {
                                        R.string.voice_auto_speak_on
                                    } else {
                                        R.string.voice_auto_speak_off
                                    },
                                ),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (state.autoSpeakEnabled) {
                                    Icons.Rounded.VolumeUp
                                } else {
                                    Icons.Rounded.VolumeOff
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("voice_autospeak_chip"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                    IconButton(
                        onClick = {
                            voiceController.stopListening()
                            voiceController.stopSpeaking()
                            viewModel.clearConversation()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("voice_clear_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.voice_clear_chat),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Voice Orb Card
            VoiceOrbPanel(
                isListening = state.isListening,
                isThinking = state.isThinking,
                isSpeaking = state.isSpeaking,
                partialTranscript = state.partialTranscript,
                onTapOrb = ::handleMicTap,
            )

            AnimatedVisibility(visible = micPermissionDeniedOnce && !hasMicPermission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.voice_permission_needed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                        ) {
                            Text(stringResource(R.string.voice_grant_mic))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state.errorMessage != null) {
                state.errorMessage?.let { error ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                            if (state.lastFailedPrompt != null) {
                                TextButton(onClick = viewModel::retryLastPrompt) {
                                    Text(stringResource(R.string.voice_retry))
                                }
                            }
                            IconButton(
                                onClick = viewModel::dismissError,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Dismiss error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Conversation list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    VoiceMessageBubble(
                        message = message,
                        isSpeakingNow = state.isSpeaking,
                        onSpeakMessage = {
                            if (state.isSpeaking) {
                                voiceController.stopSpeaking()
                            } else {
                                voiceController.speak(message.text)
                            }
                        },
                        onOpenBreathing = onOpenBreathingPattern,
                    )
                }
            }

            // Quick prompts row
            if (state.messages.size <= 2) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(QuickVoicePrompts) { prompt ->
                        AssistChip(
                            onClick = { viewModel.sendQuickPrompt(prompt) },
                            label = {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                    }
                }
            }

            // Text fallback / hybrid input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = viewModel::updateInputText,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.voice_input_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(26.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.sendCurrentInput() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voice_text_input"),
                )

                Surface(
                    onClick = viewModel::sendCurrentInput,
                    enabled = state.inputText.isNotBlank() && !state.isThinking,
                    shape = CircleShape,
                    color = if (state.inputText.isNotBlank() && !state.isThinking) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("voice_send_button"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = stringResource(R.string.voice_send),
                            tint = if (state.inputText.isNotBlank() && !state.isThinking) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceOrbPanel(
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    partialTranscript: String,
    onTapOrb: () -> Unit,
) {
    val isActive = isListening || isThinking || isSpeaking
    val pulseFactor = if (isActive) {
        val transition = rememberInfiniteTransition(label = "voice_orb_pulse")
        val value by transition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (isListening) 900 else 1350),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "orb_scale",
        )
        value
    } else {
        1.0f
    }

    val statusText = when {
        isListening && partialTranscript.isNotBlank() -> "\"$partialTranscript\""
        isListening -> stringResource(R.string.voice_listening)
        isThinking -> stringResource(R.string.voice_thinking)
        isSpeaking -> stringResource(R.string.voice_speaking)
        else -> stringResource(R.string.voice_tap_to_speak)
    }

    val orbPrimary = when {
        isListening -> MaterialTheme.colorScheme.tertiary
        isSpeaking -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTapOrb)
                    .testTag("voice_mic_orb_button"),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = size.minDimension * 0.36f
                    val haloRadius = (baseRadius * 1.32f * pulseFactor).coerceAtMost(size.minDimension * 0.49f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                orbPrimary.copy(alpha = 0.32f),
                                orbPrimary.copy(alpha = 0.04f),
                            ),
                            center = center,
                            radius = haloRadius.coerceAtLeast(1f),
                        ),
                        radius = haloRadius,
                        center = center,
                    )
                    drawCircle(
                        color = orbPrimary,
                        radius = baseRadius,
                        center = center,
                    )
                }

                if (isThinking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = when {
                            isSpeaking -> Icons.Rounded.Stop
                            isListening -> Icons.Rounded.GraphicEq
                            else -> Icons.Rounded.Mic
                        },
                        contentDescription = statusText,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isListening -> "Listening"
                        isThinking -> "Calm Guide"
                        isSpeaking -> "Speaking"
                        else -> "Voice Ready"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

@Composable
private fun VoiceMessageBubble(
    message: VoiceMessage,
    isSpeakingNow: Boolean,
    onSpeakMessage: () -> Unit,
    onOpenBreathing: (BreathingPattern) -> Unit,
) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (message.isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 500.dp),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isUser) 20.dp else 6.dp,
                bottomEnd = if (message.isUser) 6.dp else 20.dp,
            ),
            color = containerColor,
            border = if (!message.isUser) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            } else {
                null
            },
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (!message.isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Stillness Guide",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (!message.isStreaming && message.text.isNotBlank()) {
                            IconButton(
                                onClick = onSpeakMessage,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = if (isSpeakingNow) {
                                        Icons.Rounded.Stop
                                    } else {
                                        Icons.Rounded.VolumeUp
                                    },
                                    contentDescription = "Read aloud",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = if (message.text.isBlank() && message.isStreaming) {
                        "Gathering a calm response…"
                    } else {
                        message.text
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )

                if (!message.isUser && !message.isStreaming && message.suggestedBreathingPattern != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AssistChip(
                        onClick = { onOpenBreathing(message.suggestedBreathingPattern) },
                        label = {
                            Text(
                                text = "Try ${message.suggestedBreathingPattern.title}",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Air,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}
