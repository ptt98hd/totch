package io.github.ow3n.totch.accessibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Exposes the connected [NotchAccessibilityService] (set on connect, cleared on unbind/destroy). */
class AccessibilityBridge {
    private val _service = MutableStateFlow<NotchAccessibilityService?>(null)
    val service: StateFlow<NotchAccessibilityService?> = _service.asStateFlow()

    internal fun attach(service: NotchAccessibilityService) {
        _service.value = service
    }

    internal fun detach(service: NotchAccessibilityService) {
        _service.compareAndSet(service, null)
    }
}
