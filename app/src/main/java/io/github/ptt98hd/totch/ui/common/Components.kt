package io.github.ptt98hd.totch.ui.common

import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.TextButton
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
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

/** Actions offered on every screen (FR-U40, FR-U41); provided by `MainActivity` through [LocalAppActions]. */
data class AppActions(
    val onCheckUpdates: () -> Unit = {},
    val onDonate: () -> Unit = {},
    /** A newer release is known (UP-22). */
    val updateBadge: Boolean = false,
)

val LocalAppActions = compositionLocalOf { AppActions() }

/**
 * The static (pinned, never collapsing) top bar shared by every screen (UI-40): the screen title, then Check for
 * updates and Buy me a coffee on the right (UI-44).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotchTopBar(title: String) {
    val actions = LocalAppActions.current
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        actions = {
            IconButton(onClick = actions.onCheckUpdates) {
                BadgedBox(
                    badge = {
                        if (actions.updateBadge) {
                            val badgeDescription = stringResource(R.string.update_new_badge_cd)
                            Badge(Modifier.semantics { contentDescription = badgeDescription })
                        }
                    },
                ) {
                    Icon(Icons.Outlined.SystemUpdate, contentDescription = stringResource(R.string.update_title))
                }
            }
            IconButton(onClick = actions.onDonate) {
                Icon(Icons.Outlined.Coffee, contentDescription = stringResource(R.string.donate_title))
            }
        },
    )
}

/** Height of every list row in the app, with or without a description (FR-U39, UI-45). */
val RowHeight = 72.dp

/**
 * The list row used across the app (FR-U39): at least [RowHeight] tall, title and description on one line each,
 * cut with "…". [contentAlpha] dims the leading content and the texts (switched-off gestures, UI-14).
 */
@Composable
fun TotchListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    supportingColor: Color = Color.Unspecified,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentAlpha: Float = 1f,
) {
    val dim = if (contentAlpha < 1f) Modifier.alpha(contentAlpha) else Modifier
    ListItem(
        modifier = modifier.heightIn(min = RowHeight),
        colors = ListItemDefaults.colors(containerColor = containerColor),
        leadingContent = leading?.let { { Box(dim) { it() } } },
        headlineContent = { Text(headline, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = dim) },
        supportingContent = supporting?.let {
            { Text(it, color = supportingColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = dim) }
        },
        trailingContent = trailing,
    )
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
    TotchListItem(
        headline = title,
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        supporting = description,
        supportingColor = if (error) colors.error else colors.onSurfaceVariant,
        leading = {
            IconBox(size = 40.dp) {
                Icon(icon, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(22.dp))
            }
        },
        trailing = { TotchSwitch(checked = checked, onCheckedChange = null) },
    )
}

/** FR-U40: the Buy me a coffee row of Home and Settings; opens the donate dialog. */
@Composable
fun DonateRow(modifier: Modifier = Modifier) {
    val onDonate = LocalAppActions.current.onDonate
    TotchListItem(
        headline = stringResource(R.string.donate_title),
        modifier = modifier.clickable(onClick = onDonate),
        supporting = stringResource(R.string.donate_summary),
        leading = { TonalIcon(Icons.Outlined.Coffee) },
        trailing = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
    )
}

/**
 * FR-U48: the frame of the Buy me a coffee and update dialogs. A large [icon] in a tonal circle (ringed by a progress
 * indicator while [busy]; determinate when [progress] is set), a centred title, an optional [subtitle] (a chip), the
 * [content] and the [buttons], stacked at full width.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TotchDialog(
    onDismissRequest: () -> Unit,
    icon: ImageVector,
    title: String,
    buttons: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    iconContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    iconContent: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    busy: Boolean = false,
    progress: Float? = null,
    subtitle: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(
                Modifier.padding(24.dp).animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(64.dp).background(iconContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconContent, modifier = Modifier.size(32.dp))
                    }
                    when {
                        progress != null -> CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 4.dp,
                        )
                        busy -> CircularProgressIndicator(Modifier.fillMaxSize(), strokeWidth = 4.dp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(8.dp))
                    subtitle()
                }
                if (content != null) {
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
                }
                Spacer(Modifier.height(24.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp), content = buttons)
            }
        }
    }
}

/** A small rounded label under a [TotchDialog] title, e.g. a version. */
@Composable
fun DialogChip(text: String, icon: ImageVector? = null) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

/** Centred body text of a [TotchDialog]. */
@Composable
fun DialogText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/** The main button of a [TotchDialog], full width with an optional leading icon. */
@Composable
fun DialogButton(text: String, onClick: () -> Unit, icon: ImageVector? = null) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        }
        Text(text)
    }
}

/** A secondary button of a [TotchDialog], full width. */
@Composable
fun DialogTextButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(text) }
}

/** FR-U40, FR-U48: asks for a coffee and opens the donate page inside the app (FR-U52). */
@Composable
fun DonateDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.donate_url)
    TotchDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Outlined.Coffee,
        title = stringResource(R.string.donate_title),
        iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
        iconContent = MaterialTheme.colorScheme.onTertiaryContainer,
        content = { DialogText(stringResource(R.string.donate_text)) },
        buttons = {
            DialogButton(
                stringResource(R.string.donate_action),
                onClick = {
                    onDismiss()
                    uriHandler.openUri(url)
                },
                icon = Icons.Outlined.Favorite,
            )
            DialogTextButton(stringResource(R.string.donate_later), onClick = onDismiss)
        },
    )
}

/** FR-U54: the switch of every row; its thumb shows a check when on and a cross when off. */
@Composable
fun TotchSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = {
            Icon(
                if (checked) Icons.Outlined.Check else Icons.Outlined.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
    )
}

/** FR-U61: the slider of the whole app — the Material 3 slider with its default thumb and track. */
@Composable
fun TotchSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
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
            TotchSlider(
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
