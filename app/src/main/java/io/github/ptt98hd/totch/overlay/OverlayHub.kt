package io.github.ptt98hd.totch.overlay

import android.graphics.Bitmap
import android.os.SystemClock
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.core.overlay.IndicatorState
import io.github.ptt98hd.totch.core.overlay.IslandContent
import io.github.ptt98hd.totch.core.overlay.IslandController
import io.github.ptt98hd.totch.core.overlay.IslandControl
import io.github.ptt98hd.totch.core.overlay.IslandEvent
import io.github.ptt98hd.totch.core.overlay.IslandTap
import io.github.ptt98hd.totch.core.overlay.MediaInfo
import io.github.ptt98hd.totch.core.overlay.NotificationIndicator
import io.github.ptt98hd.totch.core.overlay.NotificationMeta
import io.github.ptt98hd.totch.core.overlay.TouchEffect
import io.github.ptt98hd.totch.core.overlay.touchEffectFor
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.core.settings.activeIslandActivities
import io.github.ptt98hd.totch.engine.EngineObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** A finger down on the notch, for the press glow and squish (notch-overlay §4). */
data class Press(val x: Float, val y: Float, val timeMs: Long)

/**
 * State drawn by the notch overlay (docs/specs/notch-overlay.md): the notification indicator, the Dynamic Island and
 * touch effects. Decisions are made by the pure `:core` classes; every mutation runs on the main thread.
 */
class OverlayHub(ownPackage: String, settings: StateFlow<Settings>) : EngineObserver {
    private val main = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val indicator = NotificationIndicator(ownPackage)
    private val island = IslandController(settings.value.activeIslandActivities())
    private var timer: Job? = null

    private val _indicator = MutableStateFlow<IndicatorState?>(null)
    val indicatorState: StateFlow<IndicatorState?> = _indicator.asStateFlow()

    private val _island = MutableStateFlow<IslandContent>(IslandContent.Hidden)
    val islandContent: StateFlow<IslandContent> = _island.asStateFlow()

    /** Cover art of the media on the island, or null (the app icon is drawn instead). */
    val mediaArt = MutableStateFlow<Bitmap?>(null)

    private val _press = MutableStateFlow<Press?>(null)
    val press: StateFlow<Press?> = _press.asStateFlow()

    private val _effects = MutableSharedFlow<TouchEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<TouchEffect> = _effects.asSharedFlow()

    /** The overlay window reports whether the status bar is shown (OV-4). */
    val statusBarVisible = MutableStateFlow(true)

    /** Runs a media button of the expanded island; set by [MediaWatcher]. */
    @Volatile var mediaControl: (IslandControl) -> Unit = {}

    init {
        main.launch {
            settings.map { it.activeIslandActivities() }.distinctUntilChanged().collect {
                island.activities = it
                publish()
            }
        }
    }

    // Notifications (NI-*), from MediaListenerService on the main thread.

    fun onNotificationsReset(active: List<NotificationMeta>) = onMain {
        indicator.reset(active)
        _indicator.value = indicator.state
    }

    fun onNotificationPosted(meta: NotificationMeta) = onMain {
        if (indicator.onPosted(meta)) island.onEvent(IslandEvent.Notification(meta.packageName), now()) // NI-9
        _indicator.value = indicator.state
        publish()
    }

    fun onNotificationRemoved(key: String) = onMain {
        indicator.onRemoved(key)
        _indicator.value = indicator.state
    }

    /** NI-5: the shade was opened (System UI window, Totch's own action, or the island). */
    fun onShadeOpened() = onMain {
        indicator.onSeen()
        _indicator.value = indicator.state
    }

    // Island (IS-*).

    fun onMedia(media: MediaInfo?) = onMain {
        island.setMedia(media, now())
        publish()
    }

    fun onIslandEvent(event: IslandEvent) = onMain {
        island.onEvent(event, now())
        publish()
    }

    /** A tap on the island body; returns what it did (IS-8, IS-10). */
    fun onIslandTap(): IslandTap {
        val tap = island.onTap(now())
        if (tap == IslandTap.OPEN_NOTIFICATIONS) {
            indicator.onSeen()
            _indicator.value = indicator.state
        }
        publish()
        return tap
    }

    fun onIslandControl(control: IslandControl) {
        island.onInteraction(now())
        mediaControl(control)
        publish()
    }

    private fun publish() {
        _island.value = island.content
        timer?.cancel()
        val deadline = island.nextDeadlineMs() ?: return run { timer = null }
        timer = main.launch {
            delay((deadline - now()).coerceAtLeast(0))
            island.onTick(now())
            publish()
        }
    }

    // Touch effects (EN-16…EN-18); called from the engine's threads.

    override fun onPress(x: Float, y: Float, timeMs: Long) {
        _press.value = Press(x, y, timeMs)
    }

    override fun onRelease() {
        _press.value = null
    }

    override fun onGesture(g: Gesture) {
        _effects.tryEmit(touchEffectFor(g))
    }

    private fun now() = SystemClock.uptimeMillis()

    private fun onMain(block: () -> Unit) {
        main.launch { block() }
    }
}
