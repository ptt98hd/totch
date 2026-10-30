package io.github.ptt98hd.totch.ui.notch

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.SmartButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.core.overlay.IslandActivity
import io.github.ptt98hd.totch.core.settings.ManualRegionDp
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.ui.common.LocalNotchHighlight
import io.github.ptt98hd.totch.ui.common.ResetButton
import io.github.ptt98hd.totch.ui.common.RowDivider
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
    val onPaddingDp: (Int) -> Unit = {},
    val onArea: (ManualRegionDp) -> Unit = {},
    val onReset: (NotchSetting) -> Unit = {},
    /** Unsaved settings while a touch-area slider is dragged (for the live highlight); null when done. */
    val onDraft: (Settings?) -> Unit = {},
    val onIsland: (Boolean) -> Unit = {},
    val onIslandActivity: (IslandActivity, Boolean) -> Unit = { _, _ -> },
    val onTouchEffects: (Boolean) -> Unit = {},
)

@Composable
fun NotchSettingsRoute(container: AppContainer) {
    val vm: NotchSettingsViewModel = viewModel(factory = NotchSettingsViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        container.systemStatus.refresh() // UI-80: accessibility / notification access may have changed
        onPauseOrDispose { }
    }

    // FR-U8: while this screen is shown the highlight follows the draft of the slider being dragged.
    val highlight = LocalNotchHighlight.current
    var draft by remember { mutableStateOf<Settings?>(null) }
    LaunchedEffect(state.settings) { draft = null }
    val previewArea = container.touchArea.areaFor(draft ?: state.settings)
    SideEffect {
        highlight.override = previewArea
        highlight.active = !state.loading
    }
    DisposableEffect(highlight) { onDispose { highlight.active = false } }

    val actions = remember(vm) {
        NotchSettingsActions(
            onDoubleTapMs = vm::setDoubleTapMs,
            onLongPressMs = vm::setLongPressMs,
            onSwipeDistanceDp = vm::setSwipeDistanceDp,
            onPaddingDp = vm::setPaddingDp,
            onArea = vm::setArea,
            onReset = vm::reset,
            onDraft = { draft = it },
            onIsland = vm::setIsland,
            onIslandActivity = vm::setIslandActivity,
            onTouchEffects = vm::setTouchEffects,
        )
    }
    NotchSettingsScreen(state, actions)
}

/** FR-U18: both time sliders share this scale (step 50, ± 10). */
private val MS_RANGE = 100f..1000f

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
        topBar = { TotchTopBar(stringResource(R.string.nav_notch)) },
    ) { padding ->
        LazyColumn(contentPadding = padding.withGutters(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "effects") { EffectsSection(state, actions) }
            item(key = "sensitivity") {
                SettingsSection(stringResource(R.string.section_sensitivity)) {
                    SliderSetting(
                        title = stringResource(R.string.setting_double_tap),
                        value = s.doubleTapMs.toFloat(),
                        valueRange = MS_RANGE,
                        step = 50f,
                        increment = 10f,
                        valueLabel = { ms.format(it.roundToInt()) },
                        onValueChangeFinished = { actions.onDoubleTapMs(it.roundToInt().toLong()) },
                        trailing = reset(NotchSetting.DOUBLE_TAP),
                    )
                    RowDivider()
                    SliderSetting(
                        title = stringResource(R.string.setting_long_press),
                        value = s.longPressMs.toFloat(),
                        valueRange = MS_RANGE,
                        step = 50f,
                        increment = 10f,
                        valueLabel = { ms.format(it.roundToInt()) },
                        onValueChangeFinished = { actions.onLongPressMs(it.roundToInt().toLong()) },
                        trailing = reset(NotchSetting.LONG_PRESS),
                    )
                    RowDivider()
                    SliderSetting(
                        title = stringResource(R.string.setting_swipe),
                        value = s.swipeMinDistanceDp.toFloat(),
                        valueRange = 20f..200f,
                        step = 5f,
                        valueLabel = { dp.format(it.roundToInt()) },
                        onValueChangeFinished = { actions.onSwipeDistanceDp(it.roundToInt()) },
                        trailing = reset(NotchSetting.SWIPE_DISTANCE),
                    )
                }
            }
            item(key = "position") {
                SettingsSection(stringResource(R.string.section_position)) {
                    SliderSetting(
                        title = stringResource(R.string.setting_area_x),
                        value = area.centerXFraction * 100f,
                        valueRange = 0f..100f,
                        step = 5f,
                        valueLabel = { percent.format(it.roundToInt()) },
                        onValueChange = { draftArea(area.copy(centerXFraction = it / 100f)) },
                        onValueChangeFinished = { actions.onArea(area.copy(centerXFraction = it / 100f)) },
                        trailing = reset(NotchSetting.AREA_X),
                    )
                    RowDivider()
                    SliderSetting(
                        title = stringResource(R.string.setting_area_y),
                        value = area.topDp.toFloat(),
                        valueRange = 0f..100f,
                        step = 5f,
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
                        valueRange = 10f..400f,
                        step = 5f,
                        valueLabel = { dp.format(it.roundToInt()) },
                        onValueChange = { draftArea(area.copy(widthDp = it.roundToInt())) },
                        onValueChangeFinished = { actions.onArea(area.copy(widthDp = it.roundToInt())) },
                        trailing = reset(NotchSetting.AREA_WIDTH),
                    )
                    RowDivider()
                    SliderSetting(
                        title = stringResource(R.string.setting_area_height),
                        value = area.heightDp.toFloat(),
                        valueRange = 10f..100f,
                        step = 5f,
                        valueLabel = { dp.format(it.roundToInt()) },
                        onValueChange = { draftArea(area.copy(heightDp = it.roundToInt())) },
                        onValueChangeFinished = { actions.onArea(area.copy(heightDp = it.roundToInt())) },
                        trailing = reset(NotchSetting.AREA_HEIGHT),
                    )
                    RowDivider()
                    SliderSetting(
                        title = stringResource(R.string.setting_padding),
                        value = s.paddingDp.toFloat(),
                        valueRange = 1f..10f,
                        step = 5f,
                        valueLabel = { dp.format(it.roundToInt()) },
                        onValueChange = { actions.onDraft(s.copy(paddingDp = it.roundToInt())) },
                        onValueChangeFinished = { actions.onPaddingDp(it.roundToInt()) },
                        trailing = reset(NotchSetting.PADDING),
                    )
                }
            }
        }
    }
}

/** One island activity row: icon, label, optional summary and the feature whose issue it shows (UI-80). */
private data class IslandRow(
    val activity: IslandActivity,
    val feature: OverlayFeature,
    val icon: ImageVector,
    @StringRes val title: Int,
    @StringRes val summary: Int? = null,
)

private val islandRows = listOf(
    IslandRow(IslandActivity.MEDIA, OverlayFeature.ISLAND_MEDIA, Icons.Outlined.MusicNote, R.string.setting_island_media),
    IslandRow(
        IslandActivity.NOTIFICATIONS, OverlayFeature.ISLAND_NOTIFICATIONS, Icons.Outlined.Notifications,
        R.string.setting_island_notifications, R.string.setting_island_notifications_summary, // FR-O12
    ),
    IslandRow(IslandActivity.CHARGING, OverlayFeature.ISLAND_CHARGING, Icons.Outlined.Bolt, R.string.setting_island_charging),
    IslandRow(
        IslandActivity.SOUND, OverlayFeature.ISLAND_SOUND, Icons.AutoMirrored.Outlined.VolumeUp,
        R.string.setting_island_sound, R.string.setting_island_sound_summary,
    ),
)

/** notch-overlay §6: Dynamic Island (its activities in a dialog, FR-U63; the indicator is one of them, FR-O12) and touch effects. */
@Composable
private fun EffectsSection(state: NotchSettingsUiState, actions: NotchSettingsActions) {
    val s = state.settings
    @Composable
    fun summary(feature: OverlayFeature, @StringRes normal: Int?): Pair<String?, Boolean> =
        when (state.overlayIssue(feature)) {
            OverlayIssue.ACCESSIBILITY -> stringResource(R.string.overlay_needs_accessibility) to true
            OverlayIssue.NOTIFICATION_ACCESS -> stringResource(R.string.overlay_needs_notification_access) to true
            null -> normal?.let { stringResource(it) } to false
        }

    SettingsSection(stringResource(R.string.section_effects)) {
        val (islandSummary, islandError) = summary(OverlayFeature.ISLAND, R.string.setting_island_summary)
        var islandDialog by remember { mutableStateOf(false) }
        SwitchRow(
            stringResource(R.string.setting_island), s.island, actions.onIsland,
            icon = Icons.Outlined.SmartButton, summary = islandSummary, error = islandError,
            onClick = if (s.island) ({ islandDialog = true }) else null, // FR-U63
        )
        if (islandDialog && s.island) {
            AlertDialog(
                onDismissRequest = { islandDialog = false },
                title = { Text(stringResource(R.string.setting_island)) },
                text = {
                    Column {
                        islandRows.forEach { row ->
                            val (rowSummary, rowError) = summary(row.feature, row.summary)
                            SwitchRow(
                                stringResource(row.title),
                                row.activity in s.islandActivities,
                                { actions.onIslandActivity(row.activity, it) },
                                icon = row.icon,
                                summary = rowSummary,
                                error = rowError,
                                containerColor = Color.Transparent,
                            )
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { islandDialog = false }) { Text(stringResource(R.string.done)) } },
            )
        }
        RowDivider()
        val (effectsSummary, effectsError) = summary(OverlayFeature.TOUCH_EFFECTS, R.string.setting_touch_effects_summary)
        SwitchRow(
            stringResource(R.string.setting_touch_effects), s.touchEffects, actions.onTouchEffects,
            icon = Icons.Outlined.AutoAwesome, summary = effectsSummary, error = effectsError,
        )
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
