package io.github.ptt98hd.totch.overlay

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
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
import io.github.ptt98hd.totch.core.overlay.TouchEffect
import io.github.ptt98hd.totch.core.overlay.pressProgress
import kotlinx.coroutines.launch
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalView
import io.github.ptt98hd.totch.core.overlay.IslandMotion
import io.github.ptt98hd.totch.core.overlay.IslandStep
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

private val ChargingGreen = Color(0xFF34C759)
private const val SQUISH_SCALE = 1.12f
private const val HALO_ALPHA = 0.55f
/** Extra width of the dark halo on each side of a touch-effect stroke. */
private const val HALO_DP = 3
/** The island's icons fade in once it has mostly grown. */
private const val BODY_DELAY_MS = 180L

/** A ripple started by a press, in window pixels (= screen pixels, the window sits at the top-left corner). */
private class Ripple(val center: Offset) {
    val progress = Animatable(0f)
}

/** Draws [frame] (notch-overlay §2–§4): island pill with its ring, indicator ring and touch effects. */
@Composable
fun NotchOverlayContent(hub: OverlayHub, frame: OverlayFrame) {
    val statusBar by hub.statusBarVisible.collectAsState()
    val islandState by hub.islandContent.collectAsState()
    val indicatorState by hub.indicatorState.collectAsState()
    val press by hub.press.collectAsState()
    if (!statusBar) return // OV-4

    val accent = colorResource(android.R.color.system_accent1_200)
    // FR-O10: a light core over a dark halo reads on light and dark status bars alike.
    val effectCore = colorResource(android.R.color.system_accent1_100)
    val effectHalo = colorResource(android.R.color.system_accent1_900).copy(alpha = HALO_ALPHA)
    val content = if (frame.island) islandState else IslandContent.Hidden
    val indicatorOn = frame.indicator && indicatorState != null
    val indicatorColor = indicatorState?.color?.let { Color(it) } ?: accent

    // Island shape: ring around the notch → grow (spring, dampingRatio 0.7) → shrink → ring out (IM-1…IM-3).
    val target = IslandMotion.target(content, frame.notch, frame.metrics)
    val shown = target != null
    val shape = target ?: frame.notch
    val expanded = content is IslandContent.Media && content.expanded
    val targetCorner = if (expanded) frame.metrics.dp(28).toFloat() else frame.notch.height / 2f
    val left = remember { Animatable(frame.notch.left.toFloat()) }
    val right = remember { Animatable(frame.notch.right.toFloat()) }
    val bottom = remember { Animatable(frame.notch.bottom.toFloat()) }
    val corner = remember { Animatable(frame.notch.height / 2f) }
    val ring = remember { Animatable(0f) }
    var grown by remember { mutableStateOf(false) }
    LaunchedEffect(shape, targetCorner, shown) {
        val steps = IslandMotion.steps(ringLit = ring.value >= 1f, shown = shown)
        if (!shown || IslandStep.RING_IN in steps) grown = false
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
    // Keep the last color while the ring fades out after the content is gone.
    var ringColor by remember { mutableStateOf(islandRingColor) }
    SideEffect { if (shown) ringColor = islandRingColor }

    // Touch effects (§4): squish while pressed, nudge in the swipe direction, flashes, ripples, streaks.
    val effects = frame.touchEffects
    val pressed = effects && press != null
    val squish by animateFloatAsState(
        if (pressed) SQUISH_SCALE else 1f,
        spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "squish",
    )
    val nudge = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val flash = remember { Animatable(0f) }
    val streak = remember { Animatable(0f) }
    var streakDir by remember { mutableStateOf(Offset.Zero) }
    var glowHeld by remember { mutableStateOf(false) }
    val ripples = remember { mutableStateListOf<Ripple>() }
    val rippleScope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(effects) {
        if (!effects) return@LaunchedEffect
        hub.effects.collect { effect ->
            when (effect) {
                TouchEffect.Pulse -> launch { flash.snapTo(1f); flash.animateTo(0f, tween(350)) }
                TouchEffect.DoublePulse -> launch {
                    repeat(2) { flash.snapTo(1f); flash.animateTo(0f, tween(220)) }
                }
                TouchEffect.Glow -> glowHeld = true
                is TouchEffect.Streak -> {
                    val dir = Offset(effect.dx.toFloat(), effect.dy.toFloat())
                    streakDir = dir
                    launch { streak.snapTo(0f); streak.animateTo(1f, tween(380)) }
                    launch {
                        nudge.animateTo(dir * frame.metrics.dp(6).toFloat(), tween(90))
                        nudge.animateTo(Offset.Zero, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
                    }
                }
            }
        }
    }
    LaunchedEffect(press, effects) {
        val p = press
        if (p == null || !effects) {
            progress = 0f
            glowHeld = false
            return@LaunchedEffect
        }
        val ripple = Ripple(Offset(p.x, p.y))
        ripples += ripple
        // FR-O9: outlive this effect, which restarts on release — a tap is shorter than the ripple.
        rippleScope.launch {
            try {
                ripple.progress.animateTo(1f, tween(400))
            } finally {
                ripples -= ripple
            }
        }
        while (true) withFrameMillis { progress = pressProgress(p.timeMs, SystemClock.uptimeMillis(), frame.longPressMs) }
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
    val pressedNow by rememberUpdatedState(pressed)
    val indicatorNow by rememberUpdatedState(indicatorOn)
    val idle by remember {
        derivedStateOf {
            !indicatorNow && !pressedNow && ring.value == 0f && squish == 1f && flash.value == 0f && !glowHeld &&
                ripples.isEmpty() && !streak.isRunning && nudge.value == Offset.Zero
        }
    }
    LaunchedEffect(idle) {
        if (!idle) return@LaunchedEffect
        withFrameNanos { }
        view.rootView.invalidate()
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = (left.value + right.value) / 2f
            val cy = (frame.notch.top + bottom.value) / 2f
            val w = (right.value - left.value) * squish
            val h = (bottom.value - frame.notch.top) * squish
            val origin = Offset(cx - w / 2f, cy - h / 2f) + nudge.value
            val size = Size(w, h)
            val radius = corner.value * squish

            val blobAlpha = maxOf(ring.value, if (effects && (pressed || squish > 1.001f)) 1f else 0f)
            if (blobAlpha > 0f) drawRoundRect(Color.Black.copy(alpha = blobAlpha), origin, size, CornerRadius(radius))

            val ringWidth = frame.metrics.dp(3).toFloat()
            val ringOrigin = origin - Offset(ringWidth, ringWidth)
            val ringSize = Size(w + 2 * ringWidth, h + 2 * ringWidth)
            if (ring.value > 0f) glowRing(ringColor, ring.value, ringOrigin, ringSize, radius + ringWidth)
            if (indicatorOn) glowRing(indicatorColor, breathing, ringOrigin, ringSize, radius + ringWidth)
            val touchGlow = if (effects) maxOf(progress, flash.value, if (glowHeld) 1f else 0f) else 0f
            if (touchGlow > 0f) {
                haloRing(effectHalo, touchGlow, ringOrigin, ringSize, radius + ringWidth)
                glowRing(effectCore, touchGlow, ringOrigin, ringSize, radius + ringWidth)
            }

            ripples.forEach { r ->
                val t = r.progress.value
                val rippleRadius = frame.metrics.dp(8) + t * frame.metrics.dp(48)
                val core = frame.metrics.dp(3).toFloat()
                drawCircle(
                    color = effectHalo.copy(alpha = effectHalo.alpha * (1f - t)),
                    radius = rippleRadius,
                    center = r.center,
                    style = Stroke(width = core + 2 * frame.metrics.dp(HALO_DP)),
                )
                drawCircle(
                    color = effectCore.copy(alpha = 0.95f * (1f - t)),
                    radius = rippleRadius,
                    center = r.center,
                    style = Stroke(width = core),
                )
            }
            if (effects && streak.value in 0.001f..0.999f) {
                val t = streak.value
                val length = frame.metrics.dp(64).toFloat()
                val start = Offset(cx, cy) + streakDir * (t * length)
                val end = start + streakDir * frame.metrics.dp(28).toFloat()
                val core = frame.metrics.dp(5).toFloat()
                listOf(effectHalo to core + 2 * frame.metrics.dp(HALO_DP), effectCore to core).forEach { (color, width) ->
                    drawLine(
                        brush = Brush.linearGradient(
                            listOf(color.copy(alpha = 0f), color.copy(alpha = color.alpha * (1f - t))),
                            start,
                            end,
                        ),
                        start = start,
                        end = end,
                        strokeWidth = width,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        // FR-O9: fill the window so the island's layer bounds cover everything drawn in it.
        AnimatedContent(
            targetState = if (grown) content else IslandContent.Hidden,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(100)) },
            contentKey = { it::class to (it as? IslandContent.Media)?.expanded },
            label = "island",
        ) { shown ->
            Box(Modifier.fillMaxSize()) {
                if (shown != IslandContent.Hidden) {
                    val pill = IslandLayout.pill(shown, frame.notch, frame.metrics)
                    IslandBody(hub, shown, pill, frame, indicatorColor, accent)
                }
            }
        }
    }
}

/** FR-O10: the dark halo under a touch glow, wider than [glowRing] so it outlines it on light backgrounds. */
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
    val icon = (pill.height - frame.metrics.dp(10)).coerceIn(frame.metrics.dp(14), frame.metrics.dp(26)).pxDp()
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
    Box(
        Modifier
            .offset { IntOffset(rect.left, rect.top) }
            .size(rect.width.pxDp(), rect.height.pxDp()),
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
