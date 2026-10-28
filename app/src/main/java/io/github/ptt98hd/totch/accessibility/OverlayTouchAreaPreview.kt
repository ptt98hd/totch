package io.github.ptt98hd.totch.accessibility

import io.github.ptt98hd.totch.TouchAreaPreview
import io.github.ptt98hd.totch.core.model.Rect
import kotlinx.coroutines.flow.StateFlow

/** Highlights the region through the accessibility overlay; impossible while the service is off. */
class OverlayTouchAreaPreview(
    override val region: StateFlow<Rect?>,
    private val bridge: AccessibilityBridge,
) : TouchAreaPreview {
    override fun show(durationMs: Long): Boolean {
        val rect = region.value ?: return false
        return bridge.service.value?.showPreview(rect, durationMs) ?: false
    }
}
