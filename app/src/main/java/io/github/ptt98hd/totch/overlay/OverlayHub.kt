package io.github.ptt98hd.totch.overlay

import android.graphics.Bitmap
import android.os.SystemClock
import io.github.ptt98hd.totch.core.model.Action
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
import io.github.ptt98hd.totch.core.overlay.TouchFeedback
import io.github.ptt98hd.totch.core.overlay.TouchFeedbackController
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.core.settings.activeIslandActivities
import io.github.ptt98hd.totch.engine.EngineObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State drawn by the notch overlay (docs/specs/notch-overlay.md): the notification indicator, the Dynamic Island and
 * touch effects. Decisions are made by the pure `:core` classes; every mutation runs on the main thread.
 */
class OverlayHub(ownPackage: String, private val settings: StateFlow<Settings>) : EngineObserver {
    private val main = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val indicator = NotificationIndicator(ownPackage)
    private val island = IslandController(settings.value.activeIslandActivities())
    private val touch = TouchFeedbackController()
    private var timer: Job? = null
    private var touchTimer: Job? = null

    private val _indicator = MutableStateFlow<IndicatorState?>(null)
    val indicatorState: StateFlow<IndicatorState?> = _indicator.asStateFlow()

    private val _island = MutableStateFlow<IslandContent>(IslandContent.Hidden)
    val islandContent: StateFlow<IslandContent> = _island.asStateFlow()

    /** Cover art of the media on the island, or null (the app icon is drawn instead). */
    val mediaArt = MutableStateFlow<Bitmap?>(null)

    private val _feedback = MutableStateFlow<TouchFeedback>(TouchFeedback.Idle)
    val feedback: StateFlow<TouchFeedback> = _feedback.asStateFlow()

    /** The overlay window's insets say the status bar is shown (OV-4); always true above it on most devices. */
    val windowStatusBar = MutableStateFlow(true)

    /** The display's insets say the status bar is shown (§10.2); set by the accessibility service. */
    val screenStatusBar = MutableStateFlow(true)

    /** OV-4: both say shown — a full-screen app hides the island's activities. */
    val statusBarVisible: StateFlow<Boolean> =
        combine(windowStatusBar, screenStatusBar) { w, s -> w && s }.stateIn(main, SharingStarted.Eagerly, true)

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

    /** IS-15: media expands only in the natural orientation. */
    fun setExpandable(expandable: Boolean) = onMain {
        island.expandable = expandable
        publish()
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

    // Touch feedback (EN-16…EN-19, TF-*); called from the engine's threads.

    override fun onPress(x: Float, y: Float, timeMs: Long) = onTouch { touch.onPress(x, y, now()) }

    override fun onMove(x: Float, y: Float) = onTouch { touch.onMove(x, y) }

    override fun onRelease() = onTouch {
        touch.onRelease(now(), settings.value.doubleTapMs + TouchFeedbackController.WAIT_MARGIN_MS)
    }

    override fun onGesture(g: Gesture, action: Action) = onTouch { touch.onGesture(g, action, now()) }

    private fun onTouch(block: () -> Unit) = onMain {
        block()
        publishTouch()
    }

    private fun publishTouch() {
        _feedback.value = touch.state
        touchTimer?.cancel()
        val deadline = touch.nextDeadlineMs() ?: return run { touchTimer = null }
        touchTimer = main.launch {
            delay((deadline - now()).coerceAtLeast(0))
            touch.onTick(now())
            publishTouch()
        }
    }

    private fun now() = SystemClock.uptimeMillis()

    private fun onMain(block: () -> Unit) {
        main.launch { block() }
    }
}
