package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.calmDataStore: DataStore<Preferences> by preferencesDataStore(name = "stillness_calm_prefs")

data class CalmStats(
    val completedTaskIds: Set<String> = emptySet(),
    val customTasks: List<ReliefTaskItem> = emptyList(),
    val breathSessionsCount: Int = 0,
    val mindfulSecondsTotal: Int = 0,
    val hapticsEnabled: Boolean = true,
    val selectedPattern: BreathingPattern = BreathingPattern.BOX,
)

class CalmPreferencesRepository(private val context: Context) {

    private object Keys {
        val COMPLETED_TASK_IDS = stringSetPreferencesKey("completed_task_ids")
        val CUSTOM_TASKS_RAW = stringPreferencesKey("custom_tasks_raw")
        val BREATH_SESSIONS = intPreferencesKey("breath_sessions")
        val MINDFUL_SECONDS = intPreferencesKey("mindful_seconds")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SELECTED_PATTERN = stringPreferencesKey("selected_pattern")
    }

    val calmStatsFlow: Flow<CalmStats> = context.calmDataStore.data.map { prefs ->
        val completedIds = prefs[Keys.COMPLETED_TASK_IDS] ?: emptySet()
        val customTasks = decodeCustomTasks(prefs[Keys.CUSTOM_TASKS_RAW].orEmpty(), completedIds)
        val patternName = prefs[Keys.SELECTED_PATTERN] ?: BreathingPattern.BOX.name
        val pattern = BreathingPattern.entries.firstOrNull { it.name == patternName } ?: BreathingPattern.BOX
        CalmStats(
            completedTaskIds = completedIds,
            customTasks = customTasks,
            breathSessionsCount = prefs[Keys.BREATH_SESSIONS] ?: 0,
            mindfulSecondsTotal = prefs[Keys.MINDFUL_SECONDS] ?: 0,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            selectedPattern = pattern,
        )
    }

    suspend fun toggleTaskCompletion(taskId: String) {
        context.calmDataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_TASK_IDS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(taskId)) {
                current.remove(taskId)
            } else {
                current.add(taskId)
            }
            prefs[Keys.COMPLETED_TASK_IDS] = current
        }
    }

    suspend fun markTaskCompleted(taskId: String) {
        context.calmDataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_TASK_IDS]?.toMutableSet() ?: mutableSetOf()
            current.add(taskId)
            prefs[Keys.COMPLETED_TASK_IDS] = current
        }
    }

    suspend fun addCustomTask(title: String) {
        val clean = title.trim().replace("|", " ").replace("\n", " ")
        if (clean.isEmpty()) return
        context.calmDataStore.edit { prefs ->
            val existing = decodeCustomTasks(prefs[Keys.CUSTOM_TASKS_RAW].orEmpty(), emptySet())
            val newTask = ReliefTaskItem(
                id = "custom_${System.currentTimeMillis()}",
                title = clean,
                subtitle = "Personal calming step",
                durationLabel = "Anytime",
                isCustom = true,
            )
            prefs[Keys.CUSTOM_TASKS_RAW] = encodeCustomTasks(existing + newTask)
        }
    }

    suspend fun removeCustomTask(taskId: String) {
        context.calmDataStore.edit { prefs ->
            val existing = decodeCustomTasks(prefs[Keys.CUSTOM_TASKS_RAW].orEmpty(), emptySet())
            val updated = existing.filterNot { it.id == taskId }
            prefs[Keys.CUSTOM_TASKS_RAW] = encodeCustomTasks(updated)
            val completed = prefs[Keys.COMPLETED_TASK_IDS]?.toMutableSet() ?: mutableSetOf()
            completed.remove(taskId)
            prefs[Keys.COMPLETED_TASK_IDS] = completed
        }
    }

    suspend fun resetCompletedTasks() {
        context.calmDataStore.edit { prefs ->
            prefs[Keys.COMPLETED_TASK_IDS] = emptySet()
        }
    }

    suspend fun recordBreathingSeconds(seconds: Int, completedSession: Boolean) {
        if (seconds <= 0 && !completedSession) return
        context.calmDataStore.edit { prefs ->
            val currentSeconds = prefs[Keys.MINDFUL_SECONDS] ?: 0
            prefs[Keys.MINDFUL_SECONDS] = currentSeconds + seconds.coerceAtLeast(0)
            if (completedSession) {
                prefs[Keys.BREATH_SESSIONS] = (prefs[Keys.BREATH_SESSIONS] ?: 0) + 1
            }
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.calmDataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setSelectedPattern(pattern: BreathingPattern) {
        context.calmDataStore.edit { prefs ->
            prefs[Keys.SELECTED_PATTERN] = pattern.name
        }
    }

    private fun encodeCustomTasks(tasks: List<ReliefTaskItem>): String {
        return tasks.joinToString("\n") { "${it.id}|${it.title}" }
    }

    private fun decodeCustomTasks(raw: String, completedIds: Set<String>): List<ReliefTaskItem> {
        if (raw.isBlank()) return emptyList()
        return raw.lines().mapNotNull { line ->
            val parts = line.split("|", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                ReliefTaskItem(
                    id = parts[0],
                    title = parts[1],
                    subtitle = "Personal calming step",
                    durationLabel = "Anytime",
                    isCompleted = completedIds.contains(parts[0]),
                    isCustom = true,
                )
            } else {
                null
            }
        }
    }
}
