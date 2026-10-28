package io.github.ow3n.totch.ui.gestures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ow3n.totch.AppContainer
import io.github.ow3n.totch.InstalledApps
import io.github.ow3n.totch.SettingsRepository
import io.github.ow3n.totch.core.model.Action
import io.github.ow3n.totch.core.model.Gesture
import io.github.ow3n.totch.core.settings.DetectionMode
import io.github.ow3n.totch.core.settings.actionFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One gesture row. [appLabel] is set only for [Action.LaunchApp] whose app is installed (UI-12). */
data class GestureRow(val gesture: Gesture, val action: Action, val appLabel: String? = null)

data class GesturesUiState(
    val rows: List<GestureRow> = emptyList(),
    val mode: DetectionMode = DetectionMode.ACCESSIBILITY,
    /** Gesture whose action sheet is open, or null when closed. */
    val editingGesture: Gesture? = null,
)

class GesturesViewModel(
    private val settingsRepository: SettingsRepository,
    private val installedApps: InstalledApps,
) : ViewModel() {
    private val editing = MutableStateFlow<Gesture?>(null)

    val state: StateFlow<GesturesUiState> =
        combine(settingsRepository.settings, editing) { settings, editingGesture ->
            GesturesUiState(
                rows = Gesture.entries.map { gesture ->
                    val action = settings.actionFor(gesture)
                    GestureRow(
                        gesture = gesture,
                        action = action,
                        appLabel = (action as? Action.LaunchApp)?.let { installedApps.labelOf(it.packageName) },
                    )
                },
                mode = settings.mode,
                editingGesture = editingGesture,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GesturesUiState())

    fun openSheet(gesture: Gesture) {
        editing.value = gesture
    }

    fun dismissSheet() {
        editing.value = null
    }

    /** UI-10: saves the new binding for the gesture being edited and closes the sheet. */
    fun selectAction(action: Action) {
        val gesture = editing.value ?: return
        editing.value = null
        viewModelScope.launch {
            settingsRepository.update { it.copy(bindings = it.bindings + (gesture to action)) }
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { GesturesViewModel(container.settingsRepository, container.installedApps) }
        }
    }
}
