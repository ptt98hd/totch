package io.github.ptt98hd.totch.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import io.github.ptt98hd.totch.ui.common.Gutter
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
    val onOpenUrl: (String) -> Unit = {},
    val onBack: () -> Unit = {},
)

@Composable
fun AppSettingsRoute(container: AppContainer, onBack: () -> Unit) {
    val vm: AppSettingsViewModel = viewModel(factory = AppSettingsViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val actions = remember(vm, onBack) {
        AppSettingsActions(
            onTheme = vm::setTheme,
            onAmoledBlack = vm::setAmoledBlack,
            onLanguage = vm::setLanguage,
            onStartOnBoot = vm::setStartOnBoot,
            onOpenUrl = { uriHandler.openUri(it) },
            onBack = onBack,
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
        topBar = { TotchTopBar(stringResource(R.string.settings_title), onBack = actions.onBack) },
    ) { padding ->
        LazyColumn(contentPadding = padding.withGutters(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "appearance") {
                SettingsSection(stringResource(R.string.section_appearance)) {
                    ChoiceRow(stringResource(R.string.setting_theme), themeName(s.theme)) { picker = Picker.THEME }
                    SwitchRow(stringResource(R.string.setting_amoled), s.amoledBlack, actions.onAmoledBlack)
                    ChoiceRow(stringResource(R.string.setting_language), languageName(s.language)) { picker = Picker.LANGUAGE }
                }
            }
            item(key = "system") {
                SettingsSection(stringResource(R.string.section_system)) {
                    SwitchRow(stringResource(R.string.setting_start_on_boot), s.startOnBoot, actions.onStartOnBoot)
                }
            }
            item(key = "about") { AboutSection(versionName, actions.onOpenUrl) }
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

/** FR-U5, FR-U10: About is the last section of the app settings. */
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
