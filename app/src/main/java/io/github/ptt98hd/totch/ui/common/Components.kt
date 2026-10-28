package io.github.ptt98hd.totch.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Replay
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

/** The static (pinned, never collapsing) top bar shared by every screen (UI-40): only the screen title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotchTopBar(title: String) {
    TopAppBar(title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) })
}

/** Icon area in a rounded box, as on the master card and the Home rows. */
@Composable
fun IconBox(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    size: Dp = 48.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier.size(size).background(container, RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** A rounded card holding one group of rows. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        content = content,
    )
}

/** A rounded card of [items], separated by horizontal dividers. */
@Composable
fun <T> GroupCard(items: List<T>, modifier: Modifier = Modifier, row: @Composable (T) -> Unit) {
    GroupCard(modifier) {
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = Gutter))
            row(item)
        }
    }
}

/**
 * Background of row [index] of [count] rows drawn as one rounded card in a lazy list (each row is its own
 * lazy item); a divider separates it from the previous row.
 */
@Composable
fun Modifier.groupItem(index: Int, count: Int): Modifier {
    val corner = 12.dp
    val shape = RoundedCornerShape(
        topStart = if (index == 0) corner else 0.dp,
        topEnd = if (index == 0) corner else 0.dp,
        bottomStart = if (index == count - 1) corner else 0.dp,
        bottomEnd = if (index == count - 1) corner else 0.dp,
    )
    val color = MaterialTheme.colorScheme.surfaceContainer
    val divider = MaterialTheme.colorScheme.outlineVariant
    return this
        .clip(shape)
        .background(color)
        .drawWithContent {
            drawContent()
            if (index > 0) {
                val inset = Gutter.toPx()
                drawLine(divider, Offset(inset, 0f), Offset(size.width - inset, 0f), strokeWidth = 1.dp.toPx())
            }
        }
}

/** A row with an icon box, a title, a description and a switch; the whole row toggles. */
@Composable
fun ToggleListItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    error: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    ListItem(
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        colors = ListItemDefaults.colors(containerColor = colors.surfaceContainer),
        leadingContent = {
            IconBox(size = 40.dp) {
                Icon(icon, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(22.dp))
            }
        },
        headlineContent = { Text(title) },
        supportingContent = { Text(description, color = if (error) colors.error else colors.onSurfaceVariant) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

/** Reset of a slider (UI-30); enabled only when the value differs from its default. */
@Composable
fun ResetButton(enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(Icons.Outlined.Replay, contentDescription = stringResource(R.string.reset_item_cd))
    }
}

/** UI-35: [value] rounded to the nearest multiple of [step], kept inside [range] (so both ends stay reachable). */
fun snapToStep(value: Float, range: ClosedFloatingPointRange<Float>, step: Float): Float =
    ((value / step).roundToInt() * step).coerceIn(range)

/** UI-36: [value] moved by [delta] (one unit of the − / + buttons), kept inside [range]. */
fun stepBy(value: Float, delta: Float, range: ClosedFloatingPointRange<Float>): Float = (value + delta).coerceIn(range)

/**
 * Slider with a title, a value label and − / + buttons moving by [increment]; dragging snaps to [step] and no
 * tick marks are drawn (FR-U18). [trailing] (the reset button) sits right of the + button. The value is only
 * saved through [onValueChangeFinished] so the repository is not written on every drag frame; [onValueChange]
 * reports the draft live.
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
    increment: Float = 1f,
    onValueChange: (Float) -> Unit = {},
    trailing: (@Composable () -> Unit)? = null,
) {
    var draft by remember(value) { mutableFloatStateOf(value) }
    fun nudge(delta: Float) {
        draft = stepBy(draft, delta, valueRange)
        onValueChange(draft)
        onValueChangeFinished(draft)
    }
    Column(modifier.fillMaxWidth().padding(start = Gutter, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(end = 12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                valueLabel(draft),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nudge(-increment) }, enabled = draft > valueRange.start, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(Icons.Outlined.Remove, contentDescription = stringResource(R.string.slider_decrease_cd, title))
            }
            Slider(
                value = draft,
                onValueChange = {
                    draft = snapToStep(it, valueRange, step)
                    onValueChange(draft)
                },
                onValueChangeFinished = { onValueChangeFinished(draft) },
                valueRange = valueRange,
                modifier = Modifier.weight(1f).offset(x = (-8).dp),
            )
            IconButton(onClick = { nudge(increment) }, enabled = draft < valueRange.endInclusive) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.slider_increase_cd, title))
            }
            trailing?.invoke()
        }
    }
}
