package io.github.ptt98hd.totch.ui.gestures

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.settings.DefaultBindings
import io.github.ptt98hd.totch.core.settings.isAvailable
import io.github.ptt98hd.totch.ui.common.Gutter
import io.github.ptt98hd.totch.ui.common.SectionHeader
import io.github.ptt98hd.totch.ui.common.TonalIcon
import io.github.ptt98hd.totch.ui.common.TotchListItem
import io.github.ptt98hd.totch.ui.common.TotchTopBar
import io.github.ptt98hd.totch.ui.common.actionDisplayName
import io.github.ptt98hd.totch.ui.common.icon
import io.github.ptt98hd.totch.ui.common.labelRes
import io.github.ptt98hd.totch.ui.common.withGutters
import io.github.ptt98hd.totch.ui.picker.ActionPickerSheet
import io.github.ptt98hd.totch.ui.theme.TotchTheme

@Composable
fun GesturesRoute(container: AppContainer) {
    val vm: GesturesViewModel = viewModel(factory = GesturesViewModel.factory(container))
    val state by vm.state.collectAsStateWithLifecycle()
    DisposableEffect(vm) { onDispose { vm.closePicker() } } // leaving the tab closes the picker
    GesturesScreen(
        state = state,
        onGestureClick = vm::openPicker,
        onSwitchChange = vm::setOn,
        picker = { gesture -> ActionPickerSheet(container, gesture, onDismiss = vm::closePicker) },
    )
}

@get:StringRes
private val GestureGroup.labelRes: Int
    get() = when (this) {
        GestureGroup.TAP -> R.string.group_tap
        GestureGroup.PRESS -> R.string.group_press
        GestureGroup.SWIPE -> R.string.group_swipe
        GestureGroup.SWIPE_HOLD -> R.string.group_swipe_hold
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesturesScreen(
    state: GesturesUiState,
    onGestureClick: (Gesture) -> Unit,
    onSwitchChange: (Gesture, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    picker: @Composable (Gesture) -> Unit = {},
) {
    // FR-U37: native standard sheet with only Hidden / Expanded; opened and closed from `picking` (UI-17), never dragged.
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        confirmValueChange = { it != SheetValue.PartiallyExpanded },
        skipHiddenState = false,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    // Keeps the last gesture while the sheet animates out; cleared once hidden so the next opening starts fresh.
    var sheetGesture by remember { mutableStateOf(state.picking) }
    if (state.picking != null) sheetGesture = state.picking
    LaunchedEffect(state.picking) {
        if (state.picking != null) {
            sheetState.expand()
        } else {
            sheetState.hide()
            sheetGesture = null
        }
    }
    BottomSheetScaffold(
        sheetContent = {
            // Full height of the Gestures screen, over the top bar; the bottom bar stays outside (FR-U37).
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
                sheetGesture?.let { picker(it) }
            }
        },
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        sheetDragHandle = null,
        sheetSwipeEnabled = false,
        topBar = { TotchTopBar(stringResource(R.string.nav_gestures)) },
        containerColor = MaterialTheme.colorScheme.background, // as the other screens' Scaffold
    ) { padding ->
        LazyColumn(
            contentPadding = padding.withGutters(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.groups, key = { it.group }) { section ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                    SectionHeader(stringResource(section.group.labelRes))
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        section.rows.forEachIndexed { index, row ->
                            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = Gutter))
                            GestureListItem(
                                row = row,
                                onClick = { onGestureClick(row.gesture) },
                                onSwitchChange = { onSwitchChange(row.gesture, it) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** UI-16: a gesture row with its switch; switched-off and unavailable gestures are dimmed (UI-14). */
@Composable
private fun GestureListItem(row: GestureRow, onClick: () -> Unit, onSwitchChange: (Boolean) -> Unit) {
    val bound = row.action != Action.None
    val actionName = if (bound) actionDisplayName(row.action, row.appLabel) else null
    val packageAction = row.action is Action.LaunchApp || row.action is Action.LaunchShortcut ||
        row.action is Action.LaunchActivity || row.action is Action.ClickTile
    val notInstalled = packageAction && row.appLabel == null
    val supporting = when {
        !row.enabled -> stringResource(R.string.gesture_unavailable)
        actionName == null -> null
        notInstalled -> "$actionName · ${stringResource(R.string.app_not_installed)}"
        else -> actionName
    }
    val colors = MaterialTheme.colorScheme
    val contentAlpha by animateFloatAsState(if (row.on) 1f else 0.5f, label = "gestureAlpha")
    TotchListItem(
        headline = stringResource(row.gesture.labelRes),
        modifier = Modifier.clickable(enabled = row.enabled && row.on, onClick = onClick),
        supporting = supporting,
        supportingColor = if (notInstalled && row.on) colors.error else colors.onSurfaceVariant,
        leading = { TonalIcon(row.gesture.icon) },
        trailing = {
            val switchDescription = stringResource(R.string.gesture_switch_cd, stringResource(row.gesture.labelRes))
            Switch(
                checked = row.on,
                onCheckedChange = onSwitchChange,
                enabled = row.enabled,
                modifier = Modifier.semantics { contentDescription = switchDescription },
            )
        },
        contentAlpha = contentAlpha,
    )
}

@Preview(showBackground = true)
@Composable
private fun GesturesScreenPreview() {
    TotchTheme {
        GesturesScreen(
            state = GesturesUiState(
                groups = GestureGroups.map { (group, gestures) ->
                    GestureSection(
                        group,
                        gestures.map { g ->
                            val action = if (g == Gesture.TAP) Action.LaunchApp("org.example.camera") else DefaultBindings.getValue(g)
                            GestureRow(
                                g, action, appLabel = if (g == Gesture.TAP) "Camera" else null,
                                enabled = g.isAvailable(false), on = g.isAvailable(false) && action != Action.None && g != Gesture.SWIPE_LEFT,
                            )
                        },
                    )
                },
            ),
            onGestureClick = {},
            onSwitchChange = { _, _ -> },
        )
    }
}
