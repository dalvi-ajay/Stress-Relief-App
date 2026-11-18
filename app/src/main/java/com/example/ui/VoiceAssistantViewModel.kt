package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BreathingPattern
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "VoiceAssistantViewModel"

data class VoiceMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
    val suggestedBreathingPattern: BreathingPattern? = null,
)

data class VoiceAssistantUiState(
    val messages: List<VoiceMessage> = listOf(
        VoiceMessage(
            id = 1L,
            isUser = false,
            text = "Hello, I am your calm guide in Stillness. Tell me how you are feeling, or ask for a quick breathing or grounding exercise.",
            suggestedBreathingPattern = BreathingPattern.BOX,
        ),
    ),
    val inputText: String = "",
    val partialTranscript: String = "",
    val isListening: Boolean = false,
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val autoSpeakEnabled: Boolean = true,
    val errorMessage: String? = null,
    val lastFailedPrompt: String? = null,
)

class VoiceAssistantViewModel : ViewModel() {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are Stillness, a warm, calm, concise voice assistant for everyday stress relief. " +
                        "Respond in 2 to 4 short, soothing, conversational sentences suitable for reading aloud. " +
                        "Offer practical grounding, gentle breathing cues (such as Box Breathing 4-4-4-4, 4-7-8 Calm, " +
                        "Coherent 5-5, or Long Exhale Sigh), shoulder/jaw release tips, or compassionate perspective. " +
                        "Do not use markdown formatting, bullet lists, or medical diagnoses.",
                )
            },
        )
    }

    private var chatSession: Chat? = null
    private var generationJob: Job? = null
    private var nextMessageId = 2L

    private val _uiState = MutableStateFlow(VoiceAssistantUiState())
    val uiState: StateFlow<VoiceAssistantUiState> = _uiState.asStateFlow()

    private val _speakEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val speakEvents: SharedFlow<String> = _speakEvents.asSharedFlow()

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onListeningChanged(listening: Boolean) {
        _uiState.update {
            it.copy(
                isListening = listening,
                partialTranscript = if (listening) it.partialTranscript else "",
            )
        }
    }

    fun onPartialTranscript(partial: String) {
        _uiState.update { it.copy(partialTranscript = partial) }
    }

    fun onSpeakingChanged(speaking: Boolean) {
        _uiState.update { it.copy(isSpeaking = speaking) }
    }

    fun onVoiceError(message: String) {
        _uiState.update { it.copy(errorMessage = message, isListening = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun toggleAutoSpeak() {
        _uiState.update { it.copy(autoSpeakEnabled = !it.autoSpeakEnabled) }
    }

    fun sendCurrentInput() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isThinking) return
        _uiState.update { it.copy(inputText = "") }
        sendPrompt(text)
    }

    fun sendVoiceTranscript(transcript: String) {
        val clean = transcript.trim()
        if (clean.isEmpty()) return
        _uiState.update { it.copy(partialTranscript = "") }
        sendPrompt(clean)
    }

    fun sendQuickPrompt(prompt: String) {
        if (_uiState.value.isThinking) return
        sendPrompt(prompt)
    }

    fun retryLastPrompt() {
        val last = _uiState.value.lastFailedPrompt ?: return
        sendPrompt(last, appendUserBubble = false)
    }

    fun clearConversation() {
        generationJob?.cancel()
        chatSession = null
        nextMessageId = 2L
        _uiState.update {
            VoiceAssistantUiState(
                autoSpeakEnabled = it.autoSpeakEnabled,
            )
        }
    }

    private fun sendPrompt(prompt: String, appendUserBubble: Boolean = true) {
        generationJob?.cancel()
        val assistantMsgId = nextMessageId++

        _uiState.update { state ->
            val updatedMessages = buildList {
                addAll(state.messages)
                if (appendUserBubble) {
                    add(
                        VoiceMessage(
                            id = nextMessageId++,
                            isUser = true,
                            text = prompt,
                        ),
                    )
                }
                add(
                    VoiceMessage(
                        id = assistantMsgId,
                        isUser = false,
                        text = "",
                        isStreaming = true,
                    ),
                )
            }
            state.copy(
                messages = updatedMessages,
                isThinking = true,
                errorMessage = null,
                lastFailedPrompt = null,
            )
        }

        generationJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                val activeChat = chatSession ?: model.startChat().also { chatSession = it }
                withTimeout(60_000L) {
                    activeChat.sendMessageStream(prompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        if (chunkText.isNotEmpty()) {
                            accumulated.append(chunkText)
                            val partialReply = accumulated.toString()
                            updateAssistantMessage(
                                id = assistantMsgId,
                                text = partialReply,
                                isStreaming = true,
                                suggestedPattern = null,
                            )
                        }
                    }
                }

                val finalReply = accumulated.toString().trim()
                if (finalReply.isNotEmpty()) {
                    val suggested = detectSuggestedPattern("$prompt $finalReply")
                    updateAssistantMessage(
                        id = assistantMsgId,
                        text = finalReply,
                        isStreaming = false,
                        suggestedPattern = suggested,
                    )
                    _uiState.update { it.copy(isThinking = false) }
                    if (_uiState.value.autoSpeakEnabled) {
                        _speakEvents.tryEmit(finalReply)
                    }
                } else {
                    removeMessage(assistantMsgId)
                    _uiState.update {
                        it.copy(
                            isThinking = false,
                            errorMessage = "The calm guide returned an empty response. Please try again.",
                            lastFailedPrompt = prompt,
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini voice guide request timed out", e)
                removeMessage(assistantMsgId)
                _uiState.update {
                    it.copy(
                        isThinking = false,
                        errorMessage = "Request timed out after 60 seconds. Please try again.",
                        lastFailedPrompt = prompt,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI voice guide failed", e)
                removeMessage(assistantMsgId)
                _uiState.update {
                    it.copy(
                        isThinking = false,
                        errorMessage = e.localizedMessage ?: "Unable to reach the calm guide right now.",
                        lastFailedPrompt = prompt,
                    )
                }
            }
        }
    }

    private fun updateAssistantMessage(
        id: Long,
        text: String,
        isStreaming: Boolean,
        suggestedPattern: BreathingPattern?,
    ) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == id) {
                        msg.copy(
                            text = text,
                            isStreaming = isStreaming,
                            suggestedBreathingPattern = suggestedPattern,
                        )
                    } else {
                        msg
                    }
                },
            )
        }
    }

    private fun removeMessage(id: Long) {
        _uiState.update { state ->
            state.copy(messages = state.messages.filterNot { it.id == id })
        }
    }

    private fun detectSuggestedPattern(combinedText: String): BreathingPattern? {
        val lower = combinedText.lowercase()
        return when {
            "4-7-8" in lower || "4 7 8" in lower || "sleep" in lower -> BreathingPattern.CALM_478
            "box" in lower || "4-4-4-4" in lower -> BreathingPattern.BOX
            "sigh" in lower || "exhale" in lower -> BreathingPattern.PHYSIOLOGICAL_SIGH
            "coherent" in lower || "5-5" in lower -> BreathingPattern.COHERENT
            "breath" in lower || "inhale" in lower -> BreathingPattern.BOX
            else -> null
        }
    }
}
