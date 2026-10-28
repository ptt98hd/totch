package io.github.ptt98hd.totch.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.ptt98hd.totch.R
import kotlin.math.roundToInt

/** Horizontal gutter used by every screen. */
val Gutter = 16.dp

/** Scaffold inner padding plus the standard gutters, for a screen's scrolling list. */
@Composable
fun PaddingValues.withGutters(top: Dp = 8.dp, bottom: Dp = 24.dp): PaddingValues {
    val dir = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(dir) + Gutter,
        top = calculateTopPadding() + top,
        end = calculateEndPadding(dir) + Gutter,
        bottom = calculateBottomPadding() + bottom,
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = 4.dp, top = 8.dp, bottom = 0.dp)
            .semantics { heading() },
    )
}

/** Icon inside a tonal circle, used as ListItem leading content. */
@Composable
fun TonalIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Box(
        modifier = modifier.size(40.dp).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
    }
}

/** Left slot of [TotchTopBar]: Settings on top-level screens, Back elsewhere (UI-40). */
sealed interface TopBarNav {
    val onClick: () -> Unit

    data class Settings(override val onClick: () -> Unit) : TopBarNav
    data class Back(override val onClick: () -> Unit) : TopBarNav
}

/** The static (pinned, never collapsing) top bar shared by every screen (UI-40). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotchTopBar(title: String, nav: TopBarNav, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = nav.onClick) {
                when (nav) {
                    is TopBarNav.Settings ->
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.nav_settings))
                    is TopBarNav.Back ->
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                }
            }
        },
        actions = actions,
    )
}

/** Per-item reset (UI-30); enabled only when the value differs from its default. */
@Composable
fun ResetButton(enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(Icons.Outlined.Replay, contentDescription = stringResource(R.string.reset_item_cd))
    }
}

/**
 * Slider with a title and a value label. The value is only saved through [onValueChangeFinished]
 * so the repository is not written on every drag frame; [onValueChange] reports the draft live.
 */
@Composable
fun SliderSetting(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String,
    onValueChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    step: Float = 1f,
    onValueChange: (Float) -> Unit = {},
    trailing: (@Composable () -> Unit)? = null,
) {
    var draft by remember(value) { mutableFloatStateOf(value) }
    Column(modifier.fillMaxWidth().padding(start = Gutter, end = if (trailing == null) Gutter else 4.dp, top = 4.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                valueLabel(draft),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            trailing?.invoke()
        }
        Slider(
            value = draft,
            onValueChange = {
                draft = ((it / step).roundToInt() * step).coerceIn(valueRange)
                onValueChange(draft)
            },
            onValueChangeFinished = { onValueChangeFinished(draft) },
            valueRange = valueRange,
            modifier = Modifier.padding(end = if (trailing == null) 0.dp else 12.dp),
        )
    }
}
