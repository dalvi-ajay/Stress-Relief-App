package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BreathingPattern
import com.example.data.BreathingPhase
import com.example.data.CalmPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos

data class BreathingUiState(
    val pattern: BreathingPattern = BreathingPattern.BOX,
    val sessionMinutes: Int = 3,
    val phase: BreathingPhase = BreathingPhase.READY,
    val phaseProgress: Float = 0f,
    val circleExpansion: Float = 0.0f,
    val phaseSecondsRemaining: Int = 4,
    val totalSecondsRemaining: Int = 180,
    val completedCycles: Int = 0,
    val isRunning: Boolean = false,
    val hapticsEnabled: Boolean = true,
)

class BreathingViewModel(
    private val repository: CalmPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BreathingUiState())
    val uiState: StateFlow<BreathingUiState> = _uiState.asStateFlow()

    private val _phaseHapticEvents = MutableSharedFlow<BreathingPhase>(extraBufferCapacity = 4)
    val phaseHapticEvents: SharedFlow<BreathingPhase> = _phaseHapticEvents.asSharedFlow()

    private var timerJob: Job? = null
    private var elapsedSessionMillis: Long = 0L
    private var lastReportedSecondMark: Int = 0

    init {
        viewModelScope.launch {
            val initialStats = repository.calmStatsFlow.first()
            _uiState.update {
                it.copy(
                    pattern = initialStats.selectedPattern,
                    phaseSecondsRemaining = initialStats.selectedPattern.inhaleSec,
                    hapticsEnabled = initialStats.hapticsEnabled,
                )
            }
            repository.calmStatsFlow.collect { stats ->
                _uiState.update { state ->
                    state.copy(hapticsEnabled = stats.hapticsEnabled)
                }
            }
        }
    }

    fun selectPattern(pattern: BreathingPattern) {
        if (_uiState.value.pattern == pattern && !_uiState.value.isRunning) return
        stopAndReset(pattern = pattern, minutes = _uiState.value.sessionMinutes)
        viewModelScope.launch {
            repository.setSelectedPattern(pattern)
        }
    }

    fun selectSessionMinutes(minutes: Int) {
        stopAndReset(pattern = _uiState.value.pattern, minutes = minutes)
    }

    fun toggleHaptics() {
        val next = !_uiState.value.hapticsEnabled
        viewModelScope.launch {
            repository.setHapticsEnabled(next)
        }
    }

    fun togglePlayPause() {
        val current = _uiState.value
        if (current.isRunning) {
            pauseSession()
        } else {
            if (current.phase == BreathingPhase.COMPLETE) {
                elapsedSessionMillis = 0L
                lastReportedSecondMark = 0
                _uiState.update {
                    it.copy(
                        phase = BreathingPhase.INHALE,
                        phaseProgress = 0f,
                        circleExpansion = 0f,
                        phaseSecondsRemaining = it.pattern.inhaleSec,
                        totalSecondsRemaining = it.sessionMinutes * 60,
                        completedCycles = 0,
                    )
                }
            }
            startTicker()
        }
    }

    fun resetSession() {
        stopAndReset(pattern = _uiState.value.pattern, minutes = _uiState.value.sessionMinutes)
    }

    private fun pauseSession() {
        timerJob?.cancel()
        timerJob = null
        flushUnrecordedSeconds(completed = false)
        _uiState.update { it.copy(isRunning = false) }
    }

    private fun stopAndReset(pattern: BreathingPattern, minutes: Int) {
        timerJob?.cancel()
        timerJob = null
        flushUnrecordedSeconds(completed = false)
        elapsedSessionMillis = 0L
        lastReportedSecondMark = 0
        _uiState.update {
            it.copy(
                pattern = pattern,
                sessionMinutes = minutes,
                phase = BreathingPhase.READY,
                phaseProgress = 0f,
                circleExpansion = 0f,
                phaseSecondsRemaining = pattern.inhaleSec,
                totalSecondsRemaining = minutes * 60,
                completedCycles = 0,
                isRunning = false,
            )
        }
    }

    private fun startTicker() {
        timerJob?.cancel()
        _uiState.update { state ->
            val nextPhase = if (state.phase == BreathingPhase.READY) BreathingPhase.INHALE else state.phase
            state.copy(isRunning = true, phase = nextPhase)
        }
        var previousPhase = _uiState.value.phase
        if (_uiState.value.hapticsEnabled) {
            _phaseHapticEvents.tryEmit(previousPhase)
        }

        timerJob = viewModelScope.launch {
            var lastTickNanos = System.nanoTime()
            while (isActive) {
                delay(16L)
                val now = System.nanoTime()
                val deltaMillis = ((now - lastTickNanos) / 1_000_000L).coerceIn(1L, 100L)
                lastTickNanos = now

                elapsedSessionMillis += deltaMillis
                val state = _uiState.value
                val totalSessionMillis = state.sessionMinutes * 60_000L

                if (elapsedSessionMillis >= totalSessionMillis) {
                    flushUnrecordedSeconds(completed = true)
                    _uiState.update {
                        it.copy(
                            phase = BreathingPhase.COMPLETE,
                            phaseProgress = 1f,
                            circleExpansion = 0f,
                            phaseSecondsRemaining = 0,
                            totalSecondsRemaining = 0,
                            isRunning = false,
                        )
                    }
                    if (_uiState.value.hapticsEnabled) {
                        _phaseHapticEvents.tryEmit(BreathingPhase.COMPLETE)
                    }
                    break
                }

                val cycleMillis = state.pattern.cycleSeconds * 1000L
                val completedCycles = (elapsedSessionMillis / cycleMillis).toInt()
                val posInCycleMs = elapsedSessionMillis % cycleMillis

                val phaseResult = computePhaseState(state.pattern, posInCycleMs)
                if (phaseResult.phase != previousPhase) {
                    previousPhase = phaseResult.phase
                    if (state.hapticsEnabled) {
                        _phaseHapticEvents.tryEmit(phaseResult.phase)
                    }
                }

                val totalSecondsRemaining = ceil((totalSessionMillis - elapsedSessionMillis) / 1000.0).toInt()
                    .coerceAtLeast(1)

                _uiState.update {
                    it.copy(
                        phase = phaseResult.phase,
                        phaseProgress = phaseResult.progress,
                        circleExpansion = phaseResult.expansion,
                        phaseSecondsRemaining = phaseResult.secondsRemaining,
                        totalSecondsRemaining = totalSecondsRemaining,
                        completedCycles = completedCycles,
                    )
                }
            }
        }
    }

    private fun flushUnrecordedSeconds(completed: Boolean) {
        val elapsedWholeSeconds = (elapsedSessionMillis / 1000L).toInt()
        val deltaSeconds = (elapsedWholeSeconds - lastReportedSecondMark).coerceAtLeast(0)
        if (deltaSeconds > 0 || completed) {
            lastReportedSecondMark = elapsedWholeSeconds
            viewModelScope.launch {
                repository.recordBreathingSeconds(
                    seconds = deltaSeconds,
                    completedSession = completed,
                )
            }
        }
    }

    private data class PhaseComputation(
        val phase: BreathingPhase,
        val progress: Float,
        val expansion: Float,
        val secondsRemaining: Int,
    )

    private fun computePhaseState(pattern: BreathingPattern, posInCycleMs: Long): PhaseComputation {
        val inhaleMs = pattern.inhaleSec * 1000L
        val holdMs = pattern.holdSec * 1000L
        val exhaleMs = pattern.exhaleSec * 1000L
        val restMs = pattern.restSec * 1000L

        return when {
            posInCycleMs < inhaleMs -> {
                val p = (posInCycleMs.toFloat() / inhaleMs.toFloat()).coerceIn(0f, 1f)
                val eased = easeSineInOut(p)
                val rem = ceil((inhaleMs - posInCycleMs) / 1000.0).toInt().coerceAtLeast(1)
                PhaseComputation(BreathingPhase.INHALE, p, eased, rem)
            }
            posInCycleMs < inhaleMs + holdMs -> {
                val elapsed = posInCycleMs - inhaleMs
                val p = (elapsed.toFloat() / holdMs.toFloat()).coerceIn(0f, 1f)
                val rem = ceil((holdMs - elapsed) / 1000.0).toInt().coerceAtLeast(1)
                PhaseComputation(BreathingPhase.HOLD, p, 1f, rem)
            }
            posInCycleMs < inhaleMs + holdMs + exhaleMs -> {
                val elapsed = posInCycleMs - inhaleMs - holdMs
                val p = (elapsed.toFloat() / exhaleMs.toFloat()).coerceIn(0f, 1f)
                val eased = 1f - easeSineInOut(p)
                val rem = ceil((exhaleMs - elapsed) / 1000.0).toInt().coerceAtLeast(1)
                PhaseComputation(BreathingPhase.EXHALE, p, eased, rem)
            }
            else -> {
                val elapsed = posInCycleMs - inhaleMs - holdMs - exhaleMs
                val safeRest = restMs.coerceAtLeast(1L)
                val p = (elapsed.toFloat() / safeRest.toFloat()).coerceIn(0f, 1f)
                val rem = ceil((safeRest - elapsed) / 1000.0).toInt().coerceAtLeast(1)
                PhaseComputation(BreathingPhase.REST, p, 0f, rem)
            }
        }
    }

    private fun easeSineInOut(t: Float): Float {
        return (0.5f - 0.5f * cos(PI * t)).toFloat()
    }

    override fun onCleared() {
        flushUnrecordedSeconds(completed = false)
        super.onCleared()
    }

    class Factory(private val repository: CalmPreferencesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BreathingViewModel(repository) as T
        }
    }
}
