package io.github.ptt98hd.totch.ui.gestures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.InstalledApps
import io.github.ptt98hd.totch.SettingsRepository
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.actionFor
import io.github.ptt98hd.totch.core.settings.isAvailable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import io.github.ptt98hd.totch.core.settings.isGestureOn
import io.github.ptt98hd.totch.core.settings.withGestureOn
import io.github.ptt98hd.totch.ui.common.Capabilities
import io.github.ptt98hd.totch.ui.common.Need
import io.github.ptt98hd.totch.ui.common.capabilities
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GestureGroup { TAP, PRESS, SWIPE, SWIPE_HOLD }

/** UI-13: gesture sections in display order. */
val GestureGroups: List<Pair<GestureGroup, List<Gesture>>> = listOf(
    GestureGroup.TAP to listOf(Gesture.TAP, Gesture.DOUBLE_TAP),
    GestureGroup.PRESS to listOf(Gesture.LONG_PRESS),
    GestureGroup.SWIPE to listOf(Gesture.SWIPE_LEFT, Gesture.SWIPE_RIGHT, Gesture.SWIPE_DOWN),
    GestureGroup.SWIPE_HOLD to listOf(Gesture.SWIPE_LEFT_HOLD, Gesture.SWIPE_RIGHT_HOLD, Gesture.SWIPE_DOWN_HOLD),
)

/**
 * One gesture row. [action] is the bound action, remembered while the gesture is switched off; [on] is the
 * state of its switch (UI-16). [appLabel] is set for LaunchApp/LaunchShortcut/LaunchActivity/ClickTile whose app is installed
 * (UI-12); [enabled] is false for gestures that cannot run (swipe down without accessibility, UI-14; no touch source,
 * UI-60); they are off. [need] says why the gesture, or its action while [enabled] (UI-61), cannot run.
 */
data class GestureRow(
    val gesture: Gesture,
    val action: Action,
    val appLabel: String? = null,
    val enabled: Boolean = true,
    val on: Boolean = false,
    val need: Need? = null,
) {
    /** UI-17, UI-61: the picker opens for a switched-on gesture, or to replace an action that cannot run. */
    val canPick: Boolean get() = enabled && (on || need != null)
}

data class GestureSection(val group: GestureGroup, val rows: List<GestureRow>)

data class GesturesUiState(
    val groups: List<GestureSection> = emptyList(),
    /** Gesture whose action picker sheet is open (UI-17). */
    val picking: Gesture? = null,
)

class GesturesViewModel(
    private val settingsRepository: SettingsRepository,
    private val installedApps: InstalledApps,
    /** Accessibility on lets its overlay block touches, so swipe-down gestures run (FR-G14); FR-U44 for the rest. */
    private val capabilities: Flow<Capabilities> = flowOf(Capabilities(accessibility = false)),
) : ViewModel() {
    private val picking = MutableStateFlow<Gesture?>(null)

    val state: StateFlow<GesturesUiState> =
        combine(settingsRepository.settings, picking, capabilities) { settings, picking, caps ->
            GesturesUiState(
                groups = GestureGroups.map { (group, gestures) ->
                    GestureSection(
                        group,
                        gestures.map { gesture ->
                            val action = settings.actionFor(gesture)
                            val pkg = when (action) {
                                is Action.LaunchApp -> action.packageName
                                is Action.LaunchShortcut -> action.packageName
                                is Action.LaunchActivity -> action.packageName
                                is Action.ClickTile -> action.packageName
                                else -> null
                            }
                            val gestureNeed = when {
                                !caps.touchSource -> Need.ACCESS
                                !gesture.isAvailable(caps.accessibility) -> Need.ACCESSIBILITY
                                else -> null
                            }
                            val need = gestureNeed ?: caps.need(action)
                            GestureRow(
                                gesture = gesture,
                                action = action,
                                appLabel = pkg?.let(installedApps::labelOf),
                                enabled = gestureNeed == null,
                                on = need == null && settings.isGestureOn(gesture, caps.accessibility),
                                need = need,
                            )
                        },
                    )
                },
                picking = picking,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GesturesUiState())

    /**
     * UI-16, UI-17: switching on a gesture without an action opens the picker; it is switched on once one is picked.
     * UI-60, UI-61: nothing happens while the gesture or its action cannot run.
     */
    fun setOn(gesture: Gesture, on: Boolean) {
        viewModelScope.launch {
            val row = state.value.groups.flatMap { it.rows }.firstOrNull { it.gesture == gesture }
            if (on && row != null && (!row.enabled || row.need != null)) return@launch
            val settings = settingsRepository.settings.first()
            if (on && settings.actionFor(gesture) == Action.None) {
                picking.value = gesture
            } else {
                settingsRepository.update { it.withGestureOn(gesture, on) }
            }
        }
    }

    /** UI-17, UI-61: actions can only be chosen for a gesture that is switched on or whose action cannot run. */
    fun openPicker(gesture: Gesture) {
        viewModelScope.launch {
            if (state.value.groups.flatMap { it.rows }.any { it.gesture == gesture && it.canPick }) picking.value = gesture
        }
    }

    fun closePicker() {
        picking.value = null
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                GesturesViewModel(
                    container.settingsRepository,
                    container.installedApps,
                    container.systemStatus.requirements.map { it.capabilities() },
                )
            }
        }
    }
}
