package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CalmPreferencesRepository
import com.example.data.DefaultReliefContent
import com.example.data.MoodState
import com.example.data.ReliefTaskItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ActiveGuidedActivity {
    NONE,
    GROUNDING_54321,
    BODY_RELEASE,
}

data class TasksUiState(
    val selectedMood: MoodState? = null,
    val tasks: List<ReliefTaskItem> = emptyList(),
    val completedTasksCount: Int = 0,
    val breathSessionsCount: Int = 0,
    val mindfulMinutes: Int = 0,
    val activeActivity: ActiveGuidedActivity = ActiveGuidedActivity.NONE,
    val groundingStepIndex: Int = 0,
    val groundingChecksInStep: Int = 0,
    val bodyReleaseStepIndex: Int = 0,
    val bodyReleaseHoldSecondsLeft: Int = 0,
    val isBodyReleaseHolding: Boolean = false,
    val showAddTaskDialog: Boolean = false,
)

private data class TransientTasksState(
    val selectedMood: MoodState? = null,
    val activeActivity: ActiveGuidedActivity = ActiveGuidedActivity.NONE,
    val groundingStepIndex: Int = 0,
    val groundingChecksInStep: Int = 0,
    val bodyReleaseStepIndex: Int = 0,
    val bodyReleaseHoldSecondsLeft: Int = 0,
    val isBodyReleaseHolding: Boolean = false,
    val showAddTaskDialog: Boolean = false,
)

class TasksViewModel(
    private val repository: CalmPreferencesRepository,
) : ViewModel() {

    private val transientState = MutableStateFlow(TransientTasksState())
    private var releaseTimerJob: Job? = null

    val uiState: StateFlow<TasksUiState> = combine(
        repository.calmStatsFlow,
        transientState,
    ) { stats, local ->
        val presetWithStatus = DefaultReliefContent.presetTasks.map { item ->
            item.copy(isCompleted = stats.completedTaskIds.contains(item.id))
        }
        val allTasks = presetWithStatus + stats.customTasks
        val completedCount = allTasks.count { it.isCompleted }
        TasksUiState(
            selectedMood = local.selectedMood,
            tasks = allTasks,
            completedTasksCount = completedCount,
            breathSessionsCount = stats.breathSessionsCount,
            mindfulMinutes = stats.mindfulSecondsTotal / 60,
            activeActivity = local.activeActivity,
            groundingStepIndex = local.groundingStepIndex,
            groundingChecksInStep = local.groundingChecksInStep,
            bodyReleaseStepIndex = local.bodyReleaseStepIndex,
            bodyReleaseHoldSecondsLeft = local.bodyReleaseHoldSecondsLeft,
            isBodyReleaseHolding = local.isBodyReleaseHolding,
            showAddTaskDialog = local.showAddTaskDialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TasksUiState(tasks = DefaultReliefContent.presetTasks),
    )

    fun selectMood(mood: MoodState) {
        transientState.update {
            it.copy(selectedMood = if (it.selectedMood == mood) null else mood)
        }
    }

    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId)
        }
    }

    fun addCustomTask(title: String) {
        viewModelScope.launch {
            repository.addCustomTask(title)
            transientState.update { it.copy(showAddTaskDialog = false) }
        }
    }

    fun removeCustomTask(taskId: String) {
        viewModelScope.launch {
            repository.removeCustomTask(taskId)
        }
    }

    fun resetDayChecklist() {
        viewModelScope.launch {
            repository.resetCompletedTasks()
        }
    }

    fun setAddTaskDialogVisible(visible: Boolean) {
        transientState.update { it.copy(showAddTaskDialog = visible) }
    }

    fun openGroundingActivity() {
        releaseTimerJob?.cancel()
        transientState.update {
            it.copy(
                activeActivity = ActiveGuidedActivity.GROUNDING_54321,
                groundingStepIndex = 0,
                groundingChecksInStep = 0,
            )
        }
    }

    fun tapGroundingSenseItem() {
        val current = transientState.value
        val step = DefaultReliefContent.groundingSteps.getOrNull(current.groundingStepIndex) ?: return
        val nextChecks = (current.groundingChecksInStep + 1).coerceAtMost(step.count)
        transientState.update { it.copy(groundingChecksInStep = nextChecks) }
    }

    fun advanceGroundingStep() {
        val current = transientState.value
        val maxIdx = DefaultReliefContent.groundingSteps.lastIndex
        if (current.groundingStepIndex < maxIdx) {
            transientState.update {
                it.copy(
                    groundingStepIndex = it.groundingStepIndex + 1,
                    groundingChecksInStep = 0,
                )
            }
        } else {
            viewModelScope.launch {
                repository.recordBreathingSeconds(seconds = 60, completedSession = false)
            }
            closeGuidedActivity()
        }
    }

    fun openBodyReleaseActivity() {
        releaseTimerJob?.cancel()
        val firstStep = DefaultReliefContent.bodyReleaseSteps.first()
        transientState.update {
            it.copy(
                activeActivity = ActiveGuidedActivity.BODY_RELEASE,
                bodyReleaseStepIndex = 0,
                bodyReleaseHoldSecondsLeft = firstStep.holdSeconds,
                isBodyReleaseHolding = false,
            )
        }
    }

    fun startBodyReleaseHold() {
        releaseTimerJob?.cancel()
        val stepIndex = transientState.value.bodyReleaseStepIndex
        val step = DefaultReliefContent.bodyReleaseSteps.getOrNull(stepIndex) ?: return
        transientState.update {
            it.copy(
                bodyReleaseHoldSecondsLeft = step.holdSeconds,
                isBodyReleaseHolding = true,
            )
        }
        releaseTimerJob = viewModelScope.launch {
            for (sec in step.holdSeconds downTo 1) {
                transientState.update { it.copy(bodyReleaseHoldSecondsLeft = sec) }
                delay(1000L)
            }
            transientState.update {
                it.copy(bodyReleaseHoldSecondsLeft = 0, isBodyReleaseHolding = false)
            }
        }
    }

    fun advanceBodyReleaseStep() {
        releaseTimerJob?.cancel()
        val current = transientState.value
        val steps = DefaultReliefContent.bodyReleaseSteps
        if (current.bodyReleaseStepIndex < steps.lastIndex) {
            val nextIdx = current.bodyReleaseStepIndex + 1
            transientState.update {
                it.copy(
                    bodyReleaseStepIndex = nextIdx,
                    bodyReleaseHoldSecondsLeft = steps[nextIdx].holdSeconds,
                    isBodyReleaseHolding = false,
                )
            }
        } else {
            viewModelScope.launch {
                repository.markTaskCompleted("task_shoulders")
                repository.recordBreathingSeconds(seconds = 40, completedSession = false)
            }
            closeGuidedActivity()
        }
    }

    fun closeGuidedActivity() {
        releaseTimerJob?.cancel()
        transientState.update {
            it.copy(
                activeActivity = ActiveGuidedActivity.NONE,
                isBodyReleaseHolding = false,
            )
        }
    }

    class Factory(private val repository: CalmPreferencesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TasksViewModel(repository) as T
        }
    }
}
