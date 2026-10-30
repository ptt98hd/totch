package io.github.ptt98hd.totch.core.overlay

/**
 * Metadata of a posted notification (notch-overlay §2, FR-O7): never its title, text or extras.
 * [alerting] = importance ≥ default (not a silent notification); [color] is `null` for `COLOR_DEFAULT`.
 */
data class NotificationMeta(
    val key: String,
    val packageName: String,
    val postTimeMs: Long,
    val ongoing: Boolean,
    val clearable: Boolean,
    val groupSummary: Boolean,
    val alerting: Boolean,
    val onlyAlertOnce: Boolean,
    val color: Int?,
)

/** The lit indicator: [count] unseen notifications, [color] of the newest one (`null` = theme accent). */
data class IndicatorState(val count: Int, val color: Int?)

/** Tracks unseen notifications for the notch border (NI-1…NI-8). Not thread-safe; call from one thread. */
class NotificationIndicator(private val ownPackage: String) {
    private val unseen = mutableMapOf<String, NotificationMeta>()
    private val seen = mutableSetOf<String>()

    var state: IndicatorState? = null
        private set

    /** NI-7: the listener just connected; what is already there counts as seen. */
    fun reset(active: List<NotificationMeta>) {
        unseen.clear()
        seen.clear()
        active.mapTo(seen) { it.key }
        update()
    }

    /** NI-9: returns true when [meta] alerts: it counts and is new or not "only alert once". */
    fun onPosted(meta: NotificationMeta): Boolean {
        val isNew = meta.key !in unseen && meta.key !in seen
        when {
            !meta.counts() -> { // NI-1, NI-8
                unseen.remove(meta.key)
                seen.add(meta.key)
            }
            meta.key in unseen -> unseen[meta.key] = meta
            meta.key in seen && meta.onlyAlertOnce -> Unit // NI-3
            else -> {
                seen.remove(meta.key)
                unseen[meta.key] = meta
            }
        }
        update()
        return meta.counts() && (isNew || !meta.onlyAlertOnce)
    }

    fun onRemoved(key: String) {
        unseen.remove(key)
        seen.remove(key)
        update()
    }

    /** NI-5: the shade was opened. */
    fun onSeen() {
        seen.addAll(unseen.keys)
        unseen.clear()
        update()
    }

    private fun NotificationMeta.counts() =
        !ongoing && clearable && !groupSummary && alerting && packageName != ownPackage

    private fun update() {
        state = unseen.values.maxByOrNull { it.postTimeMs }?.let { IndicatorState(unseen.size, it.color) } // NI-6
    }
}
