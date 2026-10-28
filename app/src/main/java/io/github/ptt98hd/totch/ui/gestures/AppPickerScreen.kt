package io.github.ptt98hd.totch.ui.gestures

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.AppInfo
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.ui.common.Gutter
import io.github.ptt98hd.totch.ui.theme.TotchTheme

@Composable
fun AppPickerRoute(container: AppContainer, gesture: Gesture, onBack: () -> Unit) {
    val vm: AppPickerViewModel = viewModel(key = "picker-$gesture", factory = AppPickerViewModel.factory(container, gesture))
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onBack() }
    AppPickerScreen(state = state, onQueryChange = vm::setQuery, onSelect = vm::select, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    state: AppPickerUiState,
    onQueryChange: (String) -> Unit,
    onSelect: (AppInfo) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val focusManager = LocalFocusManager.current
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_picker_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(Modifier.padding(top = padding.calculateTopPadding()).fillMaxSize().imePadding()) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = state.query,
                        onQueryChange = onQueryChange,
                        onSearch = { focusManager.clearFocus() },
                        expanded = false,
                        onExpandedChange = {},
                        placeholder = { Text(stringResource(R.string.app_picker_search)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = if (state.query.isEmpty()) null else {
                            {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.app_picker_clear))
                                }
                            }
                        },
                    )
                },
                expanded = false,
                onExpandedChange = {},
                windowInsets = WindowInsets(0),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter).padding(bottom = 8.dp),
            ) {}

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.apps.isEmpty() -> Box(Modifier.fillMaxSize().padding(Gutter), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.query.isBlank()) {
                            stringResource(R.string.app_picker_none)
                        } else {
                            stringResource(R.string.app_picker_empty, state.query.trim())
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp),
                ) {
                    items(state.apps, key = { it.packageName }) { app ->
                        ListItem(
                            modifier = Modifier.clickable { onSelect(app) }.padding(horizontal = 8.dp),
                            leadingContent = { AppIcon(app.packageName) },
                            headlineContent = { Text(app.label) },
                            supportingContent = {
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppPickerScreenPreview() {
    TotchTheme {
        AppPickerScreen(
            state = AppPickerUiState(
                gesture = Gesture.TAP,
                loading = false,
                apps = listOf(
                    AppInfo("org.example.browser", "Browser"),
                    AppInfo("org.example.camera", "Camera"),
                    AppInfo("org.example.music", "Music"),
                ),
            ),
            onQueryChange = {},
            onSelect = {},
            onBack = {},
        )
    }
}
