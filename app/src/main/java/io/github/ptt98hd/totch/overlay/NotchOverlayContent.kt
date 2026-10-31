package io.github.ptt98hd.totch.overlay

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DoNotDisturbOff
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.core.model.Rect
import io.github.ptt98hd.totch.core.overlay.IslandContent
import io.github.ptt98hd.totch.core.overlay.IslandControl
import io.github.ptt98hd.totch.core.overlay.IslandLayout
import io.github.ptt98hd.totch.core.overlay.SoundMode
import kotlinx.coroutines.launch
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalView
import io.github.ptt98hd.totch.core.overlay.IslandMotion
import io.github.ptt98hd.totch.core.overlay.IslandStep
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import io.github.ptt98hd.totch.core.model.Action
import io.github.ptt98hd.totch.core.overlay.TouchFeedback
import io.github.ptt98hd.totch.core.overlay.islandShown
import io.github.ptt98hd.totch.core.overlay.pressStretch
import io.github.ptt98hd.totch.ui.common.icon
import kotlin.math.roundToInt

private val ChargingGreen = Color(0xFF34C759)
private const val HALO_ALPHA = 0.55f
/** Extra width of the dark halo on each side of a touch-feedback ring. */
private const val HALO_DP = 3
/** The island's icons fade in once it has mostly grown. */
private const val BODY_DELAY_MS = 180L
/** While pressed, the pill moves this fraction of the way to the finger's shape every frame (§4). */
private const val FOLLOW = 0.35f

/** Degrees that turn the island's icons upright again inside the rotated natural layer (§8). */
private val LocalUpright = compositionLocalOf { 0f }

/** Draws [frame] (notch-overlay §2–§4, §8): island pill with its ring, indicator ring and touch feedback. */
@Composable
fun NotchOverlayContent(hub: OverlayHub, frame: OverlayFrame) {
    val layer = naturalLayer(frame.rotation, frame.window.width, frame.window.height)
    CompositionLocalProvider(LocalUpright provides -layer.degrees) {
        // §8: everything below is laid out in natural coordinates, then rotated onto the edge with the notch.
        Box(
            Modifier
                .fillMaxSize()
                .layout { measurable, constraints ->
                    val natural = measurable.measure(Constraints.fixed(frame.metrics.screenWidthPx, frame.heightPx))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        natural.placeWithLayer(layer.x, layer.y) {
                            rotationZ = layer.degrees
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                    }
                },
        ) { NaturalContent(hub, frame) }
    }
}

@Composable
private fun NaturalContent(hub: OverlayHub, frame: OverlayFrame) {
    val statusBar by hub.statusBarVisible.collectAsState()
    val islandState by hub.islandContent.collectAsState()
    val indicatorState by hub.indicatorState.collectAsState()
    val touchState by hub.feedback.collectAsState()

    val accent = colorResource(android.R.color.system_accent1_200)
    // FR-O10: a light core over a dark halo reads on light and dark status bars alike.
    val effectCore = colorResource(android.R.color.system_accent1_100)
    val effectHalo = colorResource(android.R.color.system_accent1_900).copy(alpha = HALO_ALPHA)
    val notch = frame.notch
    val m = frame.metrics
    val content = if (frame.island) islandShown(islandState, statusBar) else IslandContent.Hidden // OV-4
    val indicatorOn = frame.indicator && statusBar && indicatorState != null
    val indicatorColor = indicatorState?.color?.let { Color(it) } ?: accent
    // Touch feedback (§4) takes the island's place while it lasts.
    val feedback = if (frame.touchEffects) touchState else TouchFeedback.Idle
    val touching = feedback != TouchFeedback.Idle
    val pressing = feedback is TouchFeedback.Pressing
    val wing = IslandLayout.feedbackWing(notch, m)

    // Shape: ring around the notch → grow (spring, dampingRatio 0.7) → shrink → ring out (IM-1…IM-3).
    val target = when (feedback) {
        is TouchFeedback.Result -> IslandLayout.stretched(notch, m, wing)
        is TouchFeedback.Pressing, TouchFeedback.Waiting -> notch
        TouchFeedback.Idle -> IslandMotion.target(content, notch, m)
    }
    val shown = target != null
    val shape = target ?: notch
    val expanded = !touching && content is IslandContent.Media && content.expanded
    val targetCorner = if (expanded) m.dp(28).toFloat() else notch.height / 2f
    val left = remember { Animatable(notch.left.toFloat()) }
    val right = remember { Animatable(notch.right.toFloat()) }
    val bottom = remember { Animatable(notch.bottom.toFloat()) }
    val corner = remember { Animatable(notch.height / 2f) }
    val ring = remember { Animatable(0f) }
    var grown by remember { mutableStateOf(false) }
    val currentFeedback by rememberUpdatedState(feedback)
    LaunchedEffect(shape, targetCorner, shown, pressing) {
        if (pressing) {
            // The ring comes in while the pill already follows the finger and the hold time (TF-6).
            grown = false
            if (ring.value < 1f) launch { ring.animateTo(1f, tween(IslandMotion.RING_IN_MS)) }
            while (true) {
                withFrameMillis { }
                val p = currentFeedback as? TouchFeedback.Pressing ?: break
                val stretch = pressStretch(p, SystemClock.uptimeMillis(), frame.longPressMs, wing.toFloat())
                val goal = IslandLayout.stretched(notch, m, stretch.roundToInt())
                left.snapTo(lerp(left.value, goal.left.toFloat(), FOLLOW))
                right.snapTo(lerp(right.value, goal.right.toFloat(), FOLLOW))
                bottom.snapTo(lerp(bottom.value, goal.bottom.toFloat(), FOLLOW))
                corner.snapTo(lerp(corner.value, notch.height / 2f, FOLLOW))
            }
            return@LaunchedEffect
        }
        val steps = IslandMotion.steps(ringLit = ring.value >= 1f, shown = shown)
        if (!shown || IslandStep.RING_IN in steps || currentFeedback is TouchFeedback.Result) grown = false
        val shapeSpec = spring<Float>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)
        for (step in steps) when (step) {
            IslandStep.RING_IN -> ring.animateTo(1f, tween(IslandMotion.RING_IN_MS))
            IslandStep.MORPH -> coroutineScope {
                launch { left.animateTo(shape.left.toFloat(), shapeSpec) }
                launch { right.animateTo(shape.right.toFloat(), shapeSpec) }
                launch { bottom.animateTo(shape.bottom.toFloat(), shapeSpec) }
                launch { corner.animateTo(targetCorner, shapeSpec) }
                if (shown) launch { delay(BODY_DELAY_MS); grown = true }
            }
            IslandStep.RING_OUT -> ring.animateTo(0f, tween(IslandMotion.RING_OUT_MS))
        }
    }
    val islandRingColor = when {
        content is IslandContent.Charging -> ChargingGreen
        content is IslandContent.Notification -> indicatorColor
        else -> accent
    }
    // Keep the last look while the ring fades out after the content is gone.
    var ringColor by remember { mutableStateOf(islandRingColor) }
    var touchRing by remember { mutableStateOf(false) }
    SideEffect {
        if (shown) {
            ringColor = islandRingColor
            touchRing = touching
        }
    }

    // Indicator (§2): breathing glow in the newest notification's color.
    val breathing by rememberInfiniteTransition(label = "breathing").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "breath",
    )

    // FR-O9: once nothing is drawn, repaint the whole window so no stale pixels stay on the transparent buffer.
    val view = LocalView.current
    val touchingNow by rememberUpdatedState(touching)
    val indicatorNow by rememberUpdatedState(indicatorOn)
    val idle by remember { derivedStateOf { !indicatorNow && !touchingNow && ring.value == 0f } }
    LaunchedEffect(idle) {
        if (!idle) return@LaunchedEffect
        withFrameNanos { }
        view.rootView.invalidate()
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val origin = Offset(left.value, notch.top.toFloat())
            val size = Size(right.value - left.value, bottom.value - notch.top)
            val radius = corner.value

            val blobAlpha = maxOf(ring.value, if (touching) 1f else 0f)
            if (blobAlpha > 0f) drawRoundRect(Color.Black.copy(alpha = blobAlpha), origin, size, CornerRadius(radius))

            val ringWidth = m.dp(3).toFloat()
            val ringOrigin = origin - Offset(ringWidth, ringWidth)
            val ringSize = Size(size.width + 2 * ringWidth, size.height + 2 * ringWidth)
            if (ring.value > 0f) {
                if (touchRing) {
                    haloRing(effectHalo, ring.value, ringOrigin, ringSize, radius + ringWidth)
                    glowRing(effectCore, ring.value, ringOrigin, ringSize, radius + ringWidth)
                } else {
                    glowRing(ringColor, ring.value, ringOrigin, ringSize, radius + ringWidth)
                }
            }
            if (indicatorOn && !touching) glowRing(indicatorColor, breathing, ringOrigin, ringSize, radius + ringWidth)
        }

        // FR-O9: fill the window so the island's layer bounds cover everything drawn in it.
        val body: Any = when {
            !grown -> IslandContent.Hidden
            feedback is TouchFeedback.Result -> feedback
            touching -> IslandContent.Hidden
            else -> content
        }
        AnimatedContent(
            targetState = body,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(100)) },
            contentKey = { if (it is IslandContent) it::class to (it as? IslandContent.Media)?.expanded else it },
            label = "island",
        ) { shownBody ->
            Box(Modifier.fillMaxSize()) {
                when (shownBody) {
                    is TouchFeedback.Result -> ResultBody(shownBody, IslandLayout.stretched(notch, m, wing), frame)
                    is IslandContent -> if (shownBody != IslandContent.Hidden) {
                        val pill = IslandLayout.pill(shownBody, notch, m)
                        IslandBody(hub, shownBody, pill, frame, indicatorColor, accent)
                    }
                }
            }
        }
    }
}

private fun lerp(from: Float, to: Float, fraction: Float) = from + (to - from) * fraction

/** FR-O10: the dark halo under a touch ring, wider than [glowRing] so it outlines it on light backgrounds. */
private fun DrawScope.haloRing(color: Color, alpha: Float, origin: Offset, size: Size, radius: Float) {
    drawRoundRect(
        color = color.copy(alpha = (color.alpha * alpha).coerceIn(0f, 1f)),
        topLeft = origin,
        size = size,
        cornerRadius = CornerRadius(radius),
        style = Stroke(width = (2.5f + 2 * HALO_DP) * density),
    )
}

/** A glowing outline: two soft wide strokes under a sharp one. */
private fun DrawScope.glowRing(color: Color, alpha: Float, origin: Offset, size: Size, radius: Float) {
    val base = density
    listOf(10f to 0.15f, 6f to 0.3f, 2.5f to 1f).forEach { (width, a) ->
        drawRoundRect(
            color = color.copy(alpha = (a * alpha).coerceIn(0f, 1f)),
            topLeft = origin,
            size = size,
            cornerRadius = CornerRadius(radius),
            style = Stroke(width = width * base),
        )
    }
}

/** FR-O14: the gesture that ran on the left, its action on the right. */
@Composable
private fun ResultBody(result: TouchFeedback.Result, pill: Rect, frame: OverlayFrame) {
    val notch = frame.notch
    val icon = islandIconSize(pill, frame)
    Slot(Rect(pill.left, pill.top, notch.left, pill.bottom)) {
        Icon(result.gesture.icon, null, Modifier.size(icon), tint = Color.White)
    }
    Slot(Rect(notch.right, pill.top, pill.right, pill.bottom)) {
        val app = when (val a = result.action) {
            is Action.LaunchApp -> a.packageName
            is Action.LaunchShortcut -> a.packageName
            is Action.LaunchActivity -> a.packageName
            else -> null
        }
        val appIcon = app?.let { rememberAppIcon(it) }
        if (appIcon != null) AppIconImage(appIcon, icon) else Icon(result.action.icon, null, Modifier.size(icon), tint = Color.White)
    }
}

@Composable
private fun islandIconSize(pill: Rect, frame: OverlayFrame): Dp =
    (pill.height - frame.metrics.dp(10)).coerceIn(frame.metrics.dp(14), frame.metrics.dp(26)).pxDp()

@Composable
private fun Int.pxDp(): Dp = with(LocalDensity.current) { this@pxDp.toDp() }

@Composable
private fun IslandBody(
    hub: OverlayHub,
    content: IslandContent,
    pill: Rect,
    frame: OverlayFrame,
    dotColor: Color,
    accent: Color,
) {
    val notch = frame.notch
    if (content is IslandContent.Media && content.expanded) {
        ExpandedMedia(hub, content, pill, frame)
        return
    }
    val icon = islandIconSize(pill, frame)
    // Left wing: between the pill's left end and the notch; right wing: after the notch.
    Slot(Rect(pill.left, pill.top, notch.left, pill.bottom)) {
        when (content) {
            is IslandContent.Media -> MediaArt(hub, content.info.packageName, icon, RoundedCornerShape(6.dp))
            is IslandContent.Notification -> rememberAppIcon(content.packageName)?.let { AppIconImage(it, icon) }
            is IslandContent.Charging -> Icon(Icons.Rounded.Bolt, null, Modifier.size(icon), tint = ChargingGreen)
            is IslandContent.Sound -> Icon(soundIcon(content.mode), null, Modifier.size(icon), tint = Color.White)
            is IslandContent.Volume -> Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, Modifier.size(icon), tint = Color.White)
            IslandContent.Hidden -> Unit
        }
    }
    Slot(Rect(notch.right, pill.top, pill.right, pill.bottom)) {
        when (content) {
            is IslandContent.Media -> Equalizer(content.info.playing, accent, icon)
            is IslandContent.Notification -> Box(Modifier.size(icon / 2.5f).background(dotColor, CircleShape))
            is IslandContent.Charging -> PillText("${content.level}%", ChargingGreen)
            is IslandContent.Sound -> PillText(stringResource(soundLabel(content.mode)), Color.White)
            is IslandContent.Volume -> VolumeBar(content.fraction, (pill.right - notch.right - frame.metrics.dp(20)).pxDp())
            IslandContent.Hidden -> Unit
        }
    }
}

@Composable
private fun Slot(rect: Rect, content: @Composable BoxScope.() -> Unit) {
    if (rect.width <= 0) return
    val upright = LocalUpright.current
    Box(
        Modifier
            .offset { IntOffset(rect.left, rect.top) }
            .size(rect.width.pxDp(), rect.height.pxDp())
            .graphicsLayer { rotationZ = upright }, // §8: icons stay upright when rotated
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun ExpandedMedia(
    hub: OverlayHub,
    media: IslandContent.Media,
    pill: Rect,
    frame: OverlayFrame,
) {
    val m = frame.metrics
    val controls = IslandLayout.controls(pill, m)
    val bodyTop = frame.notch.bottom + m.dp(8)
    val bodyBottom = controls.getValue(IslandControl.PLAY_PAUSE).top
    Slot(Rect(pill.left + m.dp(16), bodyTop, pill.right - m.dp(16), bodyBottom)) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MediaArt(hub, media.info.packageName, 56.dp, RoundedCornerShape(12.dp))
            Column(Modifier.fillMaxWidth()) {
                media.info.title?.let {
                    Text(it, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                media.info.artist?.let {
                    Text(it, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    controls.forEach { (control, rect) ->
        Slot(rect) {
            val icon = when (control) {
                IslandControl.PREVIOUS -> Icons.Rounded.SkipPrevious
                IslandControl.NEXT -> Icons.Rounded.SkipNext
                IslandControl.PLAY_PAUSE -> if (media.info.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
            }
            Icon(icon, null, Modifier.size(if (control == IslandControl.PLAY_PAUSE) 36.dp else 30.dp), tint = Color.White)
        }
    }
}

@Composable
private fun MediaArt(hub: OverlayHub, packageName: String, size: Dp, shape: RoundedCornerShape) {
    val art by hub.mediaArt.collectAsState()
    val bitmap = art?.let { remember(it) { it.asImageBitmap() } }
    val appIcon = rememberAppIcon(packageName)
    when {
        bitmap != null -> Image(bitmap, null, Modifier.size(size).clip(shape), contentScale = ContentScale.Crop)
        appIcon != null -> AppIconImage(appIcon, size)
        else -> Icon(Icons.Rounded.MusicNote, null, Modifier.size(size), tint = Color.White)
    }
}

/** The launcher icon of [packageName], or null when it has none (only the package is known, FR-O7). */
@Composable
private fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
    }
}

@Composable
private fun AppIconImage(icon: ImageBitmap, size: Dp) = Image(icon, null, Modifier.size(size).clip(CircleShape))

@Composable
private fun Equalizer(playing: Boolean, color: Color, size: Dp) {
    val transition = rememberInfiniteTransition(label = "eq")
    val bars = listOf(0 to 420, 1 to 300, 2 to 520).map { (i, ms) ->
        transition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(ms, delayMillis = i * 60), RepeatMode.Reverse), label = "bar$i")
    }
    Row(Modifier.height(size * 0.7f), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        bars.forEach { bar ->
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(if (playing) bar.value else 0.3f)
                    .background(color, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun VolumeBar(fraction: Float, width: Dp) {
    Box(
        Modifier
            .width(width.coerceAtLeast(16.dp))
            .height(4.dp)
            .background(Color.White.copy(alpha = 0.25f), CircleShape),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(Color.White, CircleShape))
    }
}

@Composable
private fun PillText(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

private fun soundIcon(mode: SoundMode): ImageVector = when (mode) {
    SoundMode.RING -> Icons.Rounded.NotificationsActive
    SoundMode.VIBRATE -> Icons.Rounded.Vibration
    SoundMode.SILENT -> Icons.Rounded.NotificationsOff
    SoundMode.DND_ON -> Icons.Rounded.DoNotDisturbOn
    SoundMode.DND_OFF -> Icons.Rounded.DoNotDisturbOff
}

private fun soundLabel(mode: SoundMode): Int = when (mode) {
    SoundMode.RING -> R.string.island_sound_ring
    SoundMode.VIBRATE -> R.string.island_sound_vibrate
    SoundMode.SILENT -> R.string.island_sound_silent
    SoundMode.DND_ON -> R.string.island_dnd_on
    SoundMode.DND_OFF -> R.string.island_dnd_off
}
