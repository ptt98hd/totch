package io.github.ptt98hd.totch.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import io.github.ptt98hd.totch.TotchApp
import io.github.ptt98hd.totch.service.DefaultAppContainer

/**
 * Optional accessibility service (detection-modes §6): only runs actions — global actions, launching apps and
 * shortcuts from the background. Touches are read through Shizuku; it draws nothing and receives no events.
 */
class NotchAccessibilityService : AccessibilityService() {
    private var platform: DefaultAppContainer? = null

    override fun onServiceConnected() {
        platform = (application as TotchApp).platform.also { it.accessibility.attach(this) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        tearDown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        tearDown()
        super.onDestroy()
    }

    /** Launches [packageName]'s launcher activity; allowed from the background for a bound a11y service. */
    fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: error("not installed")
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Opens a static app shortcut from its stored intent URI, without selector or URI grants (FR-A12). */
    fun launchShortcut(intentUri: String) {
        val intent = Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME).apply {
            selector = null
            removeFlags(URI_GRANT_FLAGS)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    /** Opens the default assistant (FR-A11); ACTION_ASSIST first, voice command as fallback. */
    fun openAssistant() {
        val intent = listOf(Intent.ACTION_ASSIST, Intent.ACTION_VOICE_COMMAND)
            .map { Intent(it).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            .firstOrNull { it.resolveActivity(packageManager) != null }
            ?: error("no assistant")
        startActivity(intent)
    }

    private fun tearDown() {
        platform?.accessibility?.detach(this)
        platform = null
    }

    private companion object {
        const val URI_GRANT_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    }
}
