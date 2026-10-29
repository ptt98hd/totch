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
 * (UI-12); [enabled] is false for gestures that cannot run (swipe down without accessibility, UI-14); they are off.
 */
data class GestureRow(
    val gesture: Gesture,
    val action: Action,
    val appLabel: String? = null,
    val enabled: Boolean = true,
    val on: Boolean = false,
)

data class GestureSection(val group: GestureGroup, val rows: List<GestureRow>)

data class GesturesUiState(
    val groups: List<GestureSection> = emptyList(),
    /** Gesture whose action picker sheet is open (UI-17). */
    val picking: Gesture? = null,
)

class GesturesViewModel(
    private val settingsRepository: SettingsRepository,
    private val installedApps: InstalledApps,
    /** Accessibility is on, so its overlay can block touches and swipe-down gestures run (FR-G14). */
    private val canBlockTouches: Flow<Boolean> = flowOf(false),
) : ViewModel() {
    private val picking = MutableStateFlow<Gesture?>(null)

    val state: StateFlow<GesturesUiState> =
        combine(settingsRepository.settings, picking, canBlockTouches) { settings, picking, canBlock ->
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
                            GestureRow(
                                gesture = gesture,
                                action = action,
                                appLabel = pkg?.let(installedApps::labelOf),
                                enabled = gesture.isAvailable(canBlock),
                                on = settings.isGestureOn(gesture, canBlock),
                            )
                        },
                    )
                },
                picking = picking,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GesturesUiState())

    /** UI-16, UI-17: switching on a gesture without an action opens the picker; it is switched on once one is picked. */
    fun setOn(gesture: Gesture, on: Boolean) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            if (on && settings.actionFor(gesture) == Action.None) {
                picking.value = gesture
            } else {
                settingsRepository.update { it.withGestureOn(gesture, on) }
            }
        }
    }

    /** UI-17: actions can only be chosen for a gesture that is switched on. */
    fun openPicker(gesture: Gesture) {
        viewModelScope.launch {
            if (state.value.groups.flatMap { it.rows }.any { it.gesture == gesture && it.on }) picking.value = gesture
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
                    container.systemStatus.requirements.map { it.accessibilityEnabled },
                )
            }
        }
    }
}
