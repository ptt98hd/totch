package io.github.ptt98hd.totch.ui.notch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.core.settings.ManualRegionDp
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.ui.common.LocalNotchHighlight
import io.github.ptt98hd.totch.ui.common.ResetButton
import io.github.ptt98hd.totch.ui.common.SettingsSection
import io.github.ptt98hd.totch.ui.common.SliderSetting
import io.github.ptt98hd.totch.ui.common.SwitchRow
import io.github.ptt98hd.totch.ui.common.TotchTopBar
import io.github.ptt98hd.totch.ui.common.withGutters
import io.github.ptt98hd.totch.ui.theme.TotchTheme
import kotlin.math.roundToInt

/** Callbacks of the notch screen, grouped to keep the stateless signature readable. */
data class NotchSettingsActions(
    val onDoubleTapMs: (Long) -> Unit = {},
    val onLongPressMs: (Long) -> Unit = {},
    val onSwipeDistanceDp: (Int) -> Unit = {},
    val onRepeatIntervalMs: (Long) -> Unit = {},
    val onPaddingDp: (Int) -> Unit = {},
    val onHaptics: (Boolean) -> Unit = {},
    val onArea: (ManualRegionDp) -> Unit = {},
    val onReset: (NotchSetting) -> Unit = {},
    /** Unsaved settings while a touch-area slider is dragged (for the live highlight); null when done. */
    val onDraft: (Settings?) -> Unit = {},
    val onOpenSettings: () -> Unit = {},
)

@Composable
fun NotchSettingsRoute(container: AppContainer, onOpenSettings: () -> Unit) {
    val vm: NotchSettingsViewModel = viewModel(factory = NotchSettingsViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()

    // FR-U8: while this screen is shown the highlight follows the draft of the slider being dragged.
    val highlight = LocalNotchHighlight.current
    var draft by remember { mutableStateOf<Settings?>(null) }
    LaunchedEffect(state.settings) { draft = null }
    val previewRegion = container.touchArea.regionFor(draft ?: state.settings)
    SideEffect {
        highlight.override = previewRegion
        highlight.active = !state.loading
    }
    DisposableEffect(highlight) { onDispose { highlight.active = false } }

    val actions = remember(vm, onOpenSettings) {
        NotchSettingsActions(
            onDoubleTapMs = vm::setDoubleTapMs,
            onLongPressMs = vm::setLongPressMs,
            onSwipeDistanceDp = vm::setSwipeDistanceDp,
            onRepeatIntervalMs = vm::setRepeatIntervalMs,
            onPaddingDp = vm::setPaddingDp,
            onHaptics = vm::setHaptics,
            onArea = vm::setArea,
            onReset = vm::reset,
            onDraft = { draft = it },
            onOpenSettings = onOpenSettings,
        )
    }
    NotchSettingsScreen(state, actions)
}

@Composable
fun NotchSettingsScreen(state: NotchSettingsUiState, actions: NotchSettingsActions, modifier: Modifier = Modifier) {
    val s = state.settings
    val area = state.area
    val ms = stringResource(R.string.unit_ms)
    val dp = stringResource(R.string.unit_dp)
    val percent = stringResource(R.string.unit_percent)
    fun reset(item: NotchSetting): @Composable () -> Unit = {
        ResetButton(enabled = !state.isDefault(item), onClick = { actions.onReset(item) })
    }
    fun draftArea(r: ManualRegionDp) = actions.onDraft(s.copy(manualRegion = r))

    Scaffold(
        modifier = modifier,
        topBar = { TotchTopBar(stringResource(R.string.nav_notch), onOpenSettings = actions.onOpenSettings) },
    ) { padding ->
        LazyColumn(contentPadding = padding.withGutters(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "sensitivity") {
                SettingsSection(stringResource(R.string.section_sensitivity)) {
                    SliderSetting(
                        title = stringResource(R.string.setting_double_tap),
                        value = s.doubleTapMs.toFloat(),
                        valueRange = 150f..600f,
                        step = 10f,
                        valueLabel = { ms.format(it.roundToInt()) },
                        onValueChangeFinished = { actions.onDoubleTapMs(it.roundToInt().toLong()) },
                        trailing = reset(NotchSetting.DOUBLE_TAP),
                    )
                    SliderSetting(
                        title = stringResource(R.string.setting_long_press),
                        value = s.longPressMs.toFloat(),
                        valueRange = 300f..1500f,
                        step = 50f,
                        valueLabel = { ms.format(it.roundToInt()) },
                        onValueChangeFinished = { actions.onLongPressMs(it.roundToInt().toLong()) },
                        trailing = reset(NotchSetting.LONG_PRESS),
                    )
                    SliderSetting(
                        title = stringResource(R.string.setting_swipe),
                        value = s.swipeMinDistanceDp.toFloat(),
                        valueRange = 16f..200f,
                        step = 2f,
                        valueLabel = { dp.format(it.roundToInt()) },
                        onValueChangeFinished = { actions.onSwipeDistanceDp(it.roundToInt()) },
                        trailing = reset(NotchSetting.SWIPE_DISTANCE),
                    )
                    SliderSetting(
                        title = stringResource(R.string.setting_repeat_interval),
                        value = s.repeatIntervalMs.toFloat(),
                        valueRange = 100f..1000f,
                        step = 50f,
                        valueLabel = { ms.format(it.roundToInt()) },
                        onValueChangeFinished = { actions.onRepeatIntervalMs(it.roundToInt().toLong()) },
                        trailing = reset(NotchSetting.REPEAT_INTERVAL),
                    )
                }
            }
            item(key = "position") {
                SettingsSection(stringResource(R.string.section_position)) {
                    SliderSetting(
                        title = stringResource(R.string.setting_area_x),
                        value = area.centerXFraction * 100f,
                        valueRange = 0f..100f,
                        valueLabel = { percent.format(it.roundToInt()) },
                        onValueChange = { draftArea(area.copy(centerXFraction = it / 100f)) },
                        onValueChangeFinished = { actions.onArea(area.copy(centerXFraction = it / 100f)) },
                        trailing = reset(NotchSetting.AREA_X),
                    )
                    SliderSetting(
                        title = stringResource(R.string.setting_area_y),
                        value = area.topDp.toFloat(),
                        valueRange = 0f..100f,
                        valueLabel = { dp.format(it.roundToInt()) },
                        onValueChange = { draftArea(area.copy(topDp = it.roundToInt())) },
                        onValueChangeFinished = { actions.onArea(area.copy(topDp = it.roundToInt())) },
                        trailing = reset(NotchSetting.AREA_Y),
                    )
                }
            }
            item(key = "size") {
                SettingsSection(stringResource(R.string.section_size)) {
                    SliderSetting(
                        title = stringResource(R.string.setting_area_width),
                        value = area.widthDp.toFloat(),
                        valueRange = 8f..400f,
                        step = 2f,
                        valueLabel = { dp.format(it.roundToInt()) },
                        onValueChange = { draftArea(area.copy(widthDp = it.roundToInt())) },
                        onValueChangeFinished = { actions.onArea(area.copy(widthDp = it.roundToInt())) },
                        trailing = reset(NotchSetting.AREA_WIDTH),
                    )
                    SliderSetting(
                        title = stringResource(R.string.setting_area_height),
                        value = area.heightDp.toFloat(),
                        valueRange = 8f..100f,
                        valueLabel = { dp.format(it.roundToInt()) },
                        onValueChange = { draftArea(area.copy(heightDp = it.roundToInt())) },
                        onValueChangeFinished = { actions.onArea(area.copy(heightDp = it.roundToInt())) },
                        trailing = reset(NotchSetting.AREA_HEIGHT),
                    )
                    SliderSetting(
                        title = stringResource(R.string.setting_padding),
                        value = s.paddingDp.toFloat(),
                        valueRange = 0f..48f,
                        valueLabel = { dp.format(it.roundToInt()) },
                        onValueChange = { actions.onDraft(s.copy(paddingDp = it.roundToInt())) },
                        onValueChangeFinished = { actions.onPaddingDp(it.roundToInt()) },
                        trailing = reset(NotchSetting.PADDING),
                    )
                }
            }
            item(key = "feedback") {
                SettingsSection(stringResource(R.string.section_feedback)) {
                    SwitchRow(stringResource(R.string.setting_haptics), s.haptics, actions.onHaptics)
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun NotchSettingsScreenPreview() {
    TotchTheme {
        NotchSettingsScreen(
            state = NotchSettingsUiState(loading = false, settings = Settings(doubleTapMs = 250)),
            actions = NotchSettingsActions(),
        )
    }
}
