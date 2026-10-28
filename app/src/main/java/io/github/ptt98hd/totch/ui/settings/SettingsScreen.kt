package io.github.ptt98hd.totch.ui.settings

import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.settings.ManualRegionDp
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.ui.common.Gutter
import io.github.ptt98hd.totch.ui.common.SectionHeader
import io.github.ptt98hd.totch.ui.common.SliderSetting
import io.github.ptt98hd.totch.ui.common.withGutters
import io.github.ptt98hd.totch.ui.theme.TotchTheme
import kotlin.math.max
import kotlin.math.roundToInt

/** Callbacks of the settings screen, grouped to keep the stateless signature readable. */
data class SettingsActions(
    val onDoubleTapMs: (Long) -> Unit = {},
    val onLongPressMs: (Long) -> Unit = {},
    val onSwipeDistanceDp: (Int) -> Unit = {},
    val onPaddingDp: (Int) -> Unit = {},
    val onHaptics: (Boolean) -> Unit = {},
    val onCustomArea: (Boolean) -> Unit = {},
    val onManualRegion: (ManualRegionDp) -> Unit = {},
    val onPreview: () -> Unit = {},
    val onDismissPreview: () -> Unit = {},
    val onRequestReset: () -> Unit = {},
    val onConfirmReset: () -> Unit = {},
    val onCancelReset: () -> Unit = {},
    val onAbout: () -> Unit = {},
)

@Composable
fun SettingsRoute(container: AppContainer, onAbout: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val actions = remember(vm, onAbout) {
        SettingsActions(
            onDoubleTapMs = vm::setDoubleTapMs,
            onLongPressMs = vm::setLongPressMs,
            onSwipeDistanceDp = vm::setSwipeDistanceDp,
            onPaddingDp = vm::setPaddingDp,
            onHaptics = vm::setHaptics,
            onCustomArea = vm::setCustomArea,
            onManualRegion = vm::setManualRegion,
            onPreview = vm::preview,
            onDismissPreview = vm::dismissPreview,
            onRequestReset = vm::requestReset,
            onConfirmReset = vm::confirmReset,
            onCancelReset = vm::cancelReset,
            onAbout = onAbout,
        )
    }
    SettingsScreen(state, actions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: SettingsUiState, actions: SettingsActions, modifier: Modifier = Modifier) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val s = state.settings
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                actions = {
                    IconButton(onClick = actions.onAbout) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.about_cd))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding.withGutters(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "sensitivity") {
                SettingsSection(stringResource(R.string.section_sensitivity)) {
                    val ms = stringResource(R.string.unit_ms)
                    val dp = stringResource(R.string.unit_dp)
                    SliderSetting(
                        title = stringResource(R.string.setting_double_tap),
                        value = s.doubleTapMs.toFloat(),
                        valueRange = 150f..600f,
                        step = 10f,
                        valueLabel = { ms.format(it.roundToInt()) },
                        onValueChangeFinished = { actions.onDoubleTapMs(it.roundToInt().toLong()) },
                    )
                    SliderSetting(
                        title = stringResource(R.string.setting_long_press),
                        value = s.longPressMs.toFloat(),
                        valueRange = 300f..1500f,
                        step = 50f,
                        valueLabel = { ms.format(it.roundToInt()) },
                        onValueChangeFinished = { actions.onLongPressMs(it.roundToInt().toLong()) },
                    )
                    SliderSetting(
                        title = stringResource(R.string.setting_swipe),
                        value = s.swipeMinDistanceDp.toFloat(),
                        valueRange = 16f..200f,
                        step = 2f,
                        valueLabel = { dp.format(it.roundToInt()) },
                        onValueChangeFinished = { actions.onSwipeDistanceDp(it.roundToInt()) },
                    )
                    SliderSetting(
                        title = stringResource(R.string.setting_padding),
                        value = s.paddingDp.toFloat(),
                        valueRange = 0f..48f,
                        valueLabel = { dp.format(it.roundToInt()) },
                        onValueChangeFinished = { actions.onPaddingDp(it.roundToInt()) },
                    )
                }
            }
            item(key = "feedback") {
                SettingsSection(stringResource(R.string.section_feedback)) {
                    SwitchRow(
                        title = stringResource(R.string.setting_haptics),
                        summary = stringResource(R.string.setting_haptics_desc),
                        checked = s.haptics,
                        onCheckedChange = actions.onHaptics,
                    )
                }
            }
            item(key = "touch-area") {
                SettingsSection(stringResource(R.string.section_touch_area)) {
                    SwitchRow(
                        title = stringResource(R.string.setting_custom_area),
                        summary = stringResource(R.string.setting_custom_area_desc),
                        checked = s.manualRegion != null,
                        onCheckedChange = actions.onCustomArea,
                    )
                    AnimatedVisibility(visible = s.manualRegion != null) {
                        val region = s.manualRegion ?: ManualRegionDp()
                        ManualRegionSliders(region, actions.onManualRegion)
                    }
                    HorizontalDivider(Modifier.padding(horizontal = Gutter))
                    FilledTonalButton(
                        onClick = actions.onPreview,
                        modifier = Modifier.fillMaxWidth().padding(Gutter),
                    ) {
                        Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.preview_button))
                    }
                }
            }
            item(key = "reset") {
                OutlinedButton(
                    onClick = actions.onRequestReset,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.reset_button))
                }
            }
        }
    }

    if (state.confirmReset) {
        AlertDialog(
            onDismissRequest = actions.onCancelReset,
            icon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
            title = { Text(stringResource(R.string.reset_title)) },
            text = { Text(stringResource(R.string.reset_message)) },
            confirmButton = {
                TextButton(onClick = actions.onConfirmReset) { Text(stringResource(R.string.reset_confirm)) }
            },
            dismissButton = { TextButton(onClick = actions.onCancelReset) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (state.showInAppPreview) {
        AlertDialog(
            onDismissRequest = actions.onDismissPreview,
            title = { Text(stringResource(R.string.preview_title)) },
            text = {
                val region = state.previewRegion
                if (region == null) {
                    Text(stringResource(R.string.preview_unavailable))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.preview_message))
                        TouchAreaDrawing(region)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = actions.onDismissPreview) { Text(stringResource(R.string.close)) }
            },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        SectionHeader(title)
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) { content() }
        }
    }
}

@Composable
private fun SwitchRow(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

@Composable
private fun ManualRegionSliders(region: ManualRegionDp, onChange: (ManualRegionDp) -> Unit) {
    val dp = stringResource(R.string.unit_dp)
    val percent = stringResource(R.string.unit_percent)
    Column {
        SliderSetting(
            title = stringResource(R.string.setting_area_x),
            value = region.centerXFraction * 100f,
            valueRange = 0f..100f,
            valueLabel = { percent.format(it.roundToInt()) },
            onValueChangeFinished = { onChange(region.copy(centerXFraction = it / 100f)) },
        )
        SliderSetting(
            title = stringResource(R.string.setting_area_width),
            value = region.widthDp.toFloat(),
            valueRange = 8f..400f,
            step = 2f,
            valueLabel = { dp.format(it.roundToInt()) },
            onValueChangeFinished = { onChange(region.copy(widthDp = it.roundToInt())) },
        )
        SliderSetting(
            title = stringResource(R.string.setting_area_height),
            value = region.heightDp.toFloat(),
            valueRange = 8f..100f,
            valueLabel = { dp.format(it.roundToInt()) },
            onValueChangeFinished = { onChange(region.copy(heightDp = it.roundToInt())) },
        )
    }
}

/**
 * In-app illustration of the touch area (used when no overlay can be shown, e.g. Shizuku mode):
 * the top part of the screen with [region] (screen pixels) filled in `primary` at 40% alpha.
 */
@Composable
private fun TouchAreaDrawing(region: Rect) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val (screenW, screenH) = remember(context, inPreview) {
        if (inPreview) {
            1080 to 2400
        } else {
            val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
            bounds.width() to bounds.height()
        }
    }
    // Show only the top band of the screen so a small notch area stays legible.
    val bandH = minOf(screenH.toFloat(), max(region.bottom * 3f, screenW * 0.4f))
    val colors = MaterialTheme.colorScheme
    Canvas(Modifier.fillMaxWidth().aspectRatio(screenW / bandH)) {
        val scale = size.width / screenW
        val corner = CornerRadius(24.dp.toPx())
        drawRoundRect(color = colors.surfaceContainerHighest, cornerRadius = corner)
        drawRoundRect(color = colors.outline, cornerRadius = corner, style = Stroke(width = 2.dp.toPx()))
        val topLeft = Offset(region.left * scale, region.top * scale)
        val areaSize = Size(region.width * scale, region.height * scale)
        val areaCorner = CornerRadius(areaSize.height / 2)
        drawRoundRect(color = colors.primary.copy(alpha = 0.4f), topLeft = topLeft, size = areaSize, cornerRadius = areaCorner)
        drawRoundRect(
            color = colors.primary,
            topLeft = topLeft,
            size = areaSize,
            cornerRadius = areaCorner,
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    TotchTheme {
        SettingsScreen(
            state = SettingsUiState(loading = false, settings = Settings(manualRegion = ManualRegionDp())),
            actions = SettingsActions(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TouchAreaPreviewDialogPreview() {
    TotchTheme {
        SettingsScreen(
            state = SettingsUiState(
                loading = false,
                previewRegion = Rect(470, 20, 610, 110),
                showInAppPreview = true,
            ),
            actions = SettingsActions(),
        )
    }
}
