package io.github.ptt98hd.totch.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.BuildConfig
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.core.settings.AppLanguage
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.core.settings.ThemeMode
import io.github.ptt98hd.totch.ui.common.ChoiceDialog
import io.github.ptt98hd.totch.ui.common.ChoiceRow
import io.github.ptt98hd.totch.ui.common.RowDivider
import io.github.ptt98hd.totch.ui.common.SettingsSection
import io.github.ptt98hd.totch.ui.common.SwitchRow
import io.github.ptt98hd.totch.ui.common.TonalIcon
import io.github.ptt98hd.totch.ui.common.TotchTopBar
import io.github.ptt98hd.totch.ui.common.withGutters
import io.github.ptt98hd.totch.ui.theme.TotchTheme

/** Callbacks of the app settings screen, grouped to keep the stateless signature readable. */
data class AppSettingsActions(
    val onTheme: (ThemeMode) -> Unit = {},
    val onAmoledBlack: (Boolean) -> Unit = {},
    val onLanguage: (AppLanguage) -> Unit = {},
    val onStartOnBoot: (Boolean) -> Unit = {},
    val onHaptics: (Boolean) -> Unit = {},
    val onLandscapeGestures: (Boolean) -> Unit = {},
    val onOpenUrl: (String) -> Unit = {},
    val onUpdateRow: () -> Unit = {},
    val onStartUpdate: () -> Unit = {},
    val onDismissUpdate: () -> Unit = {},
)

@Composable
fun AppSettingsRoute(container: AppContainer) {
    val vm: AppSettingsViewModel = viewModel(factory = AppSettingsViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val actions = remember(vm) {
        AppSettingsActions(
            onTheme = vm::setTheme,
            onAmoledBlack = vm::setAmoledBlack,
            onLanguage = vm::setLanguage,
            onStartOnBoot = vm::setStartOnBoot,
            onHaptics = vm::setHaptics,
            onLandscapeGestures = vm::setLandscapeGestures,
            onOpenUrl = { uriHandler.openUri(it) },
            onUpdateRow = vm::onUpdateRowClick,
            onStartUpdate = vm::startUpdate,
            onDismissUpdate = vm::dismissUpdateDialog,
        )
    }
    AppSettingsScreen(state, actions, versionName = BuildConfig.VERSION_NAME)
}

private enum class Picker { THEME, LANGUAGE }

@Composable
fun AppSettingsScreen(state: AppSettingsUiState, actions: AppSettingsActions, versionName: String, modifier: Modifier = Modifier) {
    val s = state.settings
    var picker by rememberSaveable { mutableStateOf<Picker?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { TotchTopBar(stringResource(R.string.settings_title)) },
    ) { padding ->
        LazyColumn(contentPadding = padding.withGutters(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "system") {
                SettingsSection(stringResource(R.string.section_system)) {
                    // FR-U26: each system row has a one-line summary.
                    SwitchRow(
                        stringResource(R.string.setting_start_on_boot),
                        s.startOnBoot,
                        actions.onStartOnBoot,
                        Icons.Outlined.PowerSettingsNew,
                        summary = stringResource(R.string.setting_start_on_boot_summary),
                    )
                    RowDivider()
                    SwitchRow(
                        stringResource(R.string.setting_haptics),
                        s.haptics,
                        actions.onHaptics,
                        Icons.Outlined.Vibration,
                        summary = stringResource(R.string.setting_haptics_summary),
                    )
                    RowDivider()
                    SwitchRow(
                        stringResource(R.string.setting_landscape),
                        s.landscapeGestures,
                        actions.onLandscapeGestures,
                        Icons.Outlined.ScreenRotation,
                        summary = stringResource(R.string.setting_landscape_summary),
                    )
                }
            }
            item(key = "appearance") {
                SettingsSection(stringResource(R.string.section_appearance)) {
                    ChoiceRow(stringResource(R.string.setting_theme), themeName(s.theme), Icons.Outlined.Palette) { picker = Picker.THEME }
                    RowDivider()
                    SwitchRow(
                        stringResource(R.string.setting_amoled),
                        s.amoledBlack,
                        actions.onAmoledBlack,
                        Icons.Outlined.Contrast,
                        summary = stringResource(R.string.setting_amoled_summary),
                    )
                    RowDivider()
                    ChoiceRow(stringResource(R.string.setting_language), languageName(s.language), Icons.Outlined.Translate) {
                        picker = Picker.LANGUAGE
                    }
                }
            }
            item(key = "about") { AboutSection(versionName, state.update, actions) }
        }
    }

    when (picker) {
        Picker.THEME -> ChoiceDialog(
            title = stringResource(R.string.setting_theme),
            options = ThemeMode.entries,
            current = s.theme,
            label = { themeName(it) },
            onSelect = {
                picker = null
                actions.onTheme(it)
            },
            onDismiss = { picker = null },
        )
        Picker.LANGUAGE -> ChoiceDialog(
            title = stringResource(R.string.setting_language),
            options = AppLanguage.entries,
            current = s.language,
            label = { languageName(it) },
            onSelect = {
                picker = null
                actions.onLanguage(it)
            },
            onDismiss = { picker = null },
        )
        null -> Unit
    }

    val available = state.update as? UpdateState.Available
    if (state.updateDialog && available != null) {
        UpdateDialog(available, onUpdate = actions.onStartUpdate, onDismiss = actions.onDismissUpdate)
    }
}

/** UI-37: release notes of the new version, Update / Later. */
@Composable
private fun UpdateDialog(available: UpdateState.Available, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
        title = { Text(stringResource(R.string.update_dialog_title, available.release.version.toString())) },
        text = {
            Text(
                available.release.notes.trim(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = { TextButton(onClick = onUpdate) { Text(stringResource(R.string.update_action)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) } },
    )
}

/** UP-5…UP-7: one-line summary of the update row. */
@Composable
private fun updateSummary(update: UpdateState): String = when (update) {
    UpdateState.Idle -> stringResource(R.string.update_idle)
    UpdateState.Checking -> stringResource(R.string.update_checking)
    UpdateState.UpToDate -> stringResource(R.string.update_up_to_date)
    is UpdateState.Available -> stringResource(R.string.update_available, update.release.version.toString())
    is UpdateState.Downloading -> stringResource(R.string.update_downloading, (update.progress * 100).toInt())
    UpdateState.Installing -> stringResource(R.string.update_installing)
    UpdateState.Failed -> stringResource(R.string.update_failed)
}

@Composable
private fun themeName(theme: ThemeMode): String = when (theme) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}

@Composable
private fun languageName(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    AppLanguage.ENGLISH -> stringResource(R.string.language_english)
    AppLanguage.VIETNAMESE -> stringResource(R.string.language_vietnamese)
}

/** FR-U5, FR-U10, FR-U27: About is the last section of the app settings. */
@Composable
private fun AboutSection(versionName: String, update: UpdateState, actions: AppSettingsActions) {
    val onOpenUrl = actions.onOpenUrl
    val sourceUrl = stringResource(R.string.about_source_url)
    val shizukuUrl = stringResource(R.string.about_shizuku_url)
    SettingsSection(stringResource(R.string.section_about)) {
        AboutRow(Icons.Outlined.Info, stringResource(R.string.app_name), stringResource(R.string.about_version, versionName))
        RowDivider()
        AboutRow(
            Icons.Outlined.SystemUpdate,
            stringResource(R.string.update_title),
            updateSummary(update),
            onClick = actions.onUpdateRow,
            chevron = false,
        )
        RowDivider()
        AboutRow(Icons.Outlined.Gavel, stringResource(R.string.about_license_title), stringResource(R.string.about_license))
        RowDivider()
        AboutRow(
            Icons.Outlined.Code,
            stringResource(R.string.about_source_title),
            stringResource(R.string.about_source_summary),
            onClick = { onOpenUrl(sourceUrl) },
        )
        RowDivider()
        AboutRow(
            Icons.Outlined.Favorite,
            stringResource(R.string.about_credits_title),
            stringResource(R.string.about_shizuku_credit),
            onClick = { onOpenUrl(shizukuUrl) },
        )
    }
}

@Composable
private fun AboutRow(icon: ImageVector, title: String, summary: String, onClick: (() -> Unit)? = null, chevron: Boolean = true) {
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = { TonalIcon(icon) },
        headlineContent = { Text(title) },
        supportingContent = { Text(summary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = onClick?.takeIf { chevron }?.let { { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) } },
    )
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun AppSettingsScreenPreview() {
    TotchTheme {
        AppSettingsScreen(
            state = AppSettingsUiState(loading = false, settings = Settings(theme = ThemeMode.DARK)),
            actions = AppSettingsActions(),
            versionName = "1.2.0",
        )
    }
}
