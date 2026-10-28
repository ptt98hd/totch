package io.github.ptt98hd.totch.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.BuildConfig
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.core.settings.AppLanguage
import io.github.ptt98hd.totch.core.settings.ManualRegionDp
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.core.settings.ThemeMode
import io.github.ptt98hd.totch.ui.common.Gutter
import io.github.ptt98hd.totch.ui.common.LocalNotchHighlight
import io.github.ptt98hd.totch.ui.common.ResetButton
import io.github.ptt98hd.totch.ui.common.SectionHeader
import io.github.ptt98hd.totch.ui.common.SliderSetting
import io.github.ptt98hd.totch.ui.common.TonalIcon
import io.github.ptt98hd.totch.ui.common.TopBarNav
import io.github.ptt98hd.totch.ui.common.TotchTopBar
import io.github.ptt98hd.totch.ui.common.withGutters
import io.github.ptt98hd.totch.ui.theme.TotchTheme
import kotlin.math.roundToInt

/** Callbacks of the settings screen, grouped to keep the stateless signature readable. */
data class SettingsActions(
    val onDoubleTapMs: (Long) -> Unit = {},
    val onLongPressMs: (Long) -> Unit = {},
    val onSwipeDistanceDp: (Int) -> Unit = {},
    val onRepeatIntervalMs: (Long) -> Unit = {},
    val onPaddingDp: (Int) -> Unit = {},
    val onHaptics: (Boolean) -> Unit = {},
    val onCustomArea: (Boolean) -> Unit = {},
    val onManualRegion: (ManualRegionDp) -> Unit = {},
    val onTheme: (ThemeMode) -> Unit = {},
    val onAmoledBlack: (Boolean) -> Unit = {},
    val onLanguage: (AppLanguage) -> Unit = {},
    val onStartOnBoot: (Boolean) -> Unit = {},
    val onReset: (SettingItem) -> Unit = {},
    /** Unsaved settings while a touch-area slider is dragged (for the live highlight); null when done. */
    val onDraft: (Settings?) -> Unit = {},
    val onRequestReset: () -> Unit = {},
    val onConfirmReset: () -> Unit = {},
    val onCancelReset: () -> Unit = {},
    val onOpenUrl: (String) -> Unit = {},
    val onBack: () -> Unit = {},
)

@Composable
fun SettingsRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    // FR-U8: while Settings is shown the highlight follows the draft of the slider being dragged.
    val highlight = LocalNotchHighlight.current
    var draft by remember { mutableStateOf<Settings?>(null) }
    LaunchedEffect(state.settings) { draft = null }
    val previewRegion = container.touchArea.regionFor(draft ?: state.settings)
    SideEffect {
        highlight.override = previewRegion
        highlight.active = !state.loading
    }
    DisposableEffect(highlight) { onDispose { highlight.active = false } }

    val actions = remember(vm, onBack) {
        SettingsActions(
            onDoubleTapMs = vm::setDoubleTapMs,
            onLongPressMs = vm::setLongPressMs,
            onSwipeDistanceDp = vm::setSwipeDistanceDp,
            onRepeatIntervalMs = vm::setRepeatIntervalMs,
            onPaddingDp = vm::setPaddingDp,
            onHaptics = vm::setHaptics,
            onCustomArea = vm::setCustomArea,
            onManualRegion = vm::setManualRegion,
            onTheme = vm::setTheme,
            onAmoledBlack = vm::setAmoledBlack,
            onLanguage = vm::setLanguage,
            onStartOnBoot = vm::setStartOnBoot,
            onReset = vm::reset,
            onDraft = { draft = it },
            onRequestReset = vm::requestReset,
            onConfirmReset = vm::confirmReset,
            onCancelReset = vm::cancelReset,
            onOpenUrl = { uriHandler.openUri(it) },
            onBack = onBack,
        )
    }
    SettingsScreen(state, actions, versionName = BuildConfig.VERSION_NAME)
}

@Composable
fun SettingsScreen(state: SettingsUiState, actions: SettingsActions, versionName: String, modifier: Modifier = Modifier) {
    val s = state.settings
    var showLanguage by rememberSaveable { mutableStateOf(false) }
    val ms = stringResource(R.string.unit_ms)
    val dp = stringResource(R.string.unit_dp)
    fun reset(item: SettingItem): @Composable () -> Unit = {
        ResetButton(enabled = !item.isDefault(s), onClick = { actions.onReset(item) })
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TotchTopBar(stringResource(R.string.settings_title), TopBarNav.Back(actions.onBack)) {
                IconButton(onClick = actions.onRequestReset) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = stringResource(R.string.reset_settings_cd))
                }
            }
        },
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
                        trailing = reset(SettingItem.DOUBLE_TAP),
                    )
                    SliderSetting(
                        title = stringResource(R.string.setting_long_press),
                        value = s.longPressMs.toFloat(),
                        valueRange = 300f..1500f,
                        step = 50f,
                        valueLabel = { ms.format(it.roundToInt()) },
                        onValueChangeFinished = { actions.onLongPressMs(it.roundToInt().toLong()) },
                        trailing = reset(SettingItem.LONG_PRESS),
                    )
                    SliderSetting(
                        title = stringResource(R.string.setting_swipe),
                        value = s.swipeMinDistanceDp.toFloat(),
                        valueRange = 16f..200f,
                        step = 2f,
                        valueLabel = { dp.format(it.roundToInt()) },
                        onValueChangeFinished = { actions.onSwipeDistanceDp(it.roundToInt()) },
                        trailing = reset(SettingItem.SWIPE_DISTANCE),
                    )
                    SliderSetting(
                        title = stringResource(R.string.setting_repeat_interval),
                        value = s.repeatIntervalMs.toFloat(),
                        valueRange = 100f..1000f,
                        step = 50f,
                        valueLabel = { ms.format(it.roundToInt()) },
                        onValueChangeFinished = { actions.onRepeatIntervalMs(it.roundToInt().toLong()) },
                        trailing = reset(SettingItem.REPEAT_INTERVAL),
                    )
                }
            }
            item(key = "touch-area") {
                SettingsSection(stringResource(R.string.section_touch_area)) {
                    SliderSetting(
                        title = stringResource(R.string.setting_padding),
                        value = s.paddingDp.toFloat(),
                        valueRange = 0f..48f,
                        valueLabel = { dp.format(it.roundToInt()) },
                        onValueChange = { actions.onDraft(s.copy(paddingDp = it.roundToInt())) },
                        onValueChangeFinished = { actions.onPaddingDp(it.roundToInt()) },
                        trailing = reset(SettingItem.PADDING),
                    )
                    SwitchRow(
                        title = stringResource(R.string.setting_custom_area),
                        checked = s.manualRegion != null,
                        onCheckedChange = actions.onCustomArea,
                        reset = reset(SettingItem.CUSTOM_AREA),
                    )
                    AnimatedVisibility(visible = s.manualRegion != null) {
                        ManualRegionSliders(s, s.manualRegion ?: ManualRegionDp(), actions, ::reset)
                    }
                }
            }
            item(key = "feedback") {
                SettingsSection(stringResource(R.string.section_feedback)) {
                    SwitchRow(
                        title = stringResource(R.string.setting_haptics),
                        checked = s.haptics,
                        onCheckedChange = actions.onHaptics,
                        reset = reset(SettingItem.HAPTICS),
                    )
                }
            }
            item(key = "appearance") {
                SettingsSection(stringResource(R.string.section_appearance)) {
                    ThemeSelector(s.theme, actions.onTheme, reset(SettingItem.THEME))
                    SwitchRow(
                        title = stringResource(R.string.setting_amoled),
                        checked = s.amoledBlack,
                        onCheckedChange = actions.onAmoledBlack,
                        reset = reset(SettingItem.AMOLED),
                    )
                    ListItem(
                        modifier = Modifier.clickable { showLanguage = true },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        headlineContent = { Text(stringResource(R.string.setting_language)) },
                        supportingContent = { Text(languageName(s.language)) },
                        trailingContent = reset(SettingItem.LANGUAGE),
                    )
                }
            }
            item(key = "system") {
                SettingsSection(stringResource(R.string.section_system)) {
                    SwitchRow(
                        title = stringResource(R.string.setting_start_on_boot),
                        checked = s.startOnBoot,
                        onCheckedChange = actions.onStartOnBoot,
                        reset = reset(SettingItem.START_ON_BOOT),
                    )
                }
            }
            item(key = "about") { AboutSection(versionName, actions.onOpenUrl) }
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

    if (showLanguage) {
        LanguageDialog(
            current = s.language,
            onSelect = {
                showLanguage = false
                actions.onLanguage(it)
            },
            onDismiss = { showLanguage = false },
        )
    }
}

@Composable
private fun languageName(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    AppLanguage.ENGLISH -> stringResource(R.string.language_english)
    AppLanguage.VIETNAMESE -> stringResource(R.string.language_vietnamese)
}

@Composable
private fun LanguageDialog(current: AppLanguage, onSelect: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.setting_language)) },
        text = {
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = language == current, role = Role.RadioButton, onClick = { onSelect(language) })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        RadioButton(selected = language == current, onClick = null)
                        Text(languageName(language), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ThemeSelector(theme: ThemeMode, onTheme: (ThemeMode) -> Unit, reset: @Composable () -> Unit) {
    Column(Modifier.padding(start = Gutter, end = 4.dp, top = 4.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.setting_theme), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            reset()
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(end = 12.dp)) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = theme == mode,
                    onClick = { onTheme(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    label = {
                        Text(
                            stringResource(
                                when (mode) {
                                    ThemeMode.SYSTEM -> R.string.theme_system
                                    ThemeMode.LIGHT -> R.string.theme_light
                                    ThemeMode.DARK -> R.string.theme_dark
                                },
                            ),
                        )
                    },
                )
            }
        }
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
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, reset: @Composable () -> Unit) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        headlineContent = { Text(title) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                reset()
                Switch(checked = checked, onCheckedChange = null)
            }
        },
    )
}

@Composable
private fun ManualRegionSliders(
    s: Settings,
    region: ManualRegionDp,
    actions: SettingsActions,
    reset: (SettingItem) -> (@Composable () -> Unit),
) {
    val dp = stringResource(R.string.unit_dp)
    val percent = stringResource(R.string.unit_percent)
    fun draft(r: ManualRegionDp) = actions.onDraft(s.copy(manualRegion = r))
    Column {
        SliderSetting(
            title = stringResource(R.string.setting_area_x),
            value = region.centerXFraction * 100f,
            valueRange = 0f..100f,
            valueLabel = { percent.format(it.roundToInt()) },
            onValueChange = { draft(region.copy(centerXFraction = it / 100f)) },
            onValueChangeFinished = { actions.onManualRegion(region.copy(centerXFraction = it / 100f)) },
            trailing = reset(SettingItem.AREA_X),
        )
        SliderSetting(
            title = stringResource(R.string.setting_area_width),
            value = region.widthDp.toFloat(),
            valueRange = 8f..400f,
            step = 2f,
            valueLabel = { dp.format(it.roundToInt()) },
            onValueChange = { draft(region.copy(widthDp = it.roundToInt())) },
            onValueChangeFinished = { actions.onManualRegion(region.copy(widthDp = it.roundToInt())) },
            trailing = reset(SettingItem.AREA_WIDTH),
        )
        SliderSetting(
            title = stringResource(R.string.setting_area_height),
            value = region.heightDp.toFloat(),
            valueRange = 8f..100f,
            valueLabel = { dp.format(it.roundToInt()) },
            onValueChange = { draft(region.copy(heightDp = it.roundToInt())) },
            onValueChangeFinished = { actions.onManualRegion(region.copy(heightDp = it.roundToInt())) },
            trailing = reset(SettingItem.AREA_HEIGHT),
        )
    }
}

/** FR-U5, FR-U10: About is the last section of Settings. */
@Composable
private fun AboutSection(versionName: String, onOpenUrl: (String) -> Unit) {
    val sourceUrl = stringResource(R.string.about_source_url)
    val shizukuUrl = stringResource(R.string.about_shizuku_url)
    SettingsSection(stringResource(R.string.section_about)) {
        AboutRow(Icons.Outlined.Info, stringResource(R.string.app_name), stringResource(R.string.about_version, versionName))
        HorizontalDivider(Modifier.padding(horizontal = Gutter))
        AboutRow(Icons.Outlined.Gavel, stringResource(R.string.about_license_title), stringResource(R.string.about_license))
        HorizontalDivider(Modifier.padding(horizontal = Gutter))
        AboutRow(Icons.Outlined.Code, stringResource(R.string.about_source_title), sourceUrl, onClick = { onOpenUrl(sourceUrl) })
        HorizontalDivider(Modifier.padding(horizontal = Gutter))
        AboutRow(
            Icons.Outlined.Favorite,
            stringResource(R.string.about_credits_title),
            stringResource(R.string.about_shizuku_credit),
            onClick = { onOpenUrl(shizukuUrl) },
        )
    }
}

@Composable
private fun AboutRow(icon: ImageVector, title: String, summary: String, onClick: (() -> Unit)? = null) {
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = { TonalIcon(icon) },
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = onClick?.let { { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) } },
    )
}

@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun SettingsScreenPreview() {
    TotchTheme {
        SettingsScreen(
            state = SettingsUiState(
                loading = false,
                settings = Settings(manualRegion = ManualRegionDp(), theme = ThemeMode.DARK, doubleTapMs = 250),
            ),
            actions = SettingsActions(),
            versionName = "1.1.0",
        )
    }
}
