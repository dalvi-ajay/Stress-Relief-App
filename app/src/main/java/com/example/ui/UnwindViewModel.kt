package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.data.DefaultReliefContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class UnwindMode(val label: String) {
    POND("Still Pond"),
    BUBBLES("Worry Pop"),
    PROMPTS("Kind Prompts"),
}

data class WorryBubble(
    val id: Int,
    val label: String,
    val isPopped: Boolean = false,
)

data class UnwindUiState(
    val mode: UnwindMode = UnwindMode.POND,
    val rippleCount: Int = 0,
    val bubbles: List<WorryBubble> = DefaultReliefContent.worryBubbleLabels.mapIndexed { idx, label ->
        WorryBubble(id = idx, label = label, isPopped = false)
    },
    val poppedCount: Int = 0,
    val lastReleasedLabel: String? = null,
    val promptIndex: Int = 0,
)

class UnwindViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UnwindUiState())
    val uiState: StateFlow<UnwindUiState> = _uiState.asStateFlow()

    fun selectMode(mode: UnwindMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun incrementRipples() {
        _uiState.update { it.copy(rippleCount = it.rippleCount + 1) }
    }

    fun popBubble(id: Int) {
        _uiState.update { state ->
            var poppedText: String? = state.lastReleasedLabel
            val updated = state.bubbles.map { bubble ->
                if (bubble.id == id && !bubble.isPopped) {
                    poppedText = bubble.label
                    bubble.copy(isPopped = true)
                } else {
                    bubble
                }
            }
            state.copy(
                bubbles = updated,
                poppedCount = state.poppedCount + 1,
                lastReleasedLabel = poppedText,
            )
        }
    }

    fun refillBubbles() {
        _uiState.update { state ->
            state.copy(
                bubbles = DefaultReliefContent.worryBubbleLabels.mapIndexed { idx, label ->
                    WorryBubble(id = idx, label = label, isPopped = false)
                },
                lastReleasedLabel = null,
            )
        }
    }

    fun nextPrompt() {
        _uiState.update { state ->
            val next = (state.promptIndex + 1) % DefaultReliefContent.kindPrompts.size
            state.copy(promptIndex = next)
        }
    }
}
