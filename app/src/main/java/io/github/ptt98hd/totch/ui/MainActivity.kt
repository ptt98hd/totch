package io.github.ptt98hd.totch.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.ptt98hd.totch.BuildConfig
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.TotchApp
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.locale.AppLocales
import io.github.ptt98hd.totch.ui.common.AppActions
import io.github.ptt98hd.totch.ui.common.DonateDialog
import io.github.ptt98hd.totch.ui.common.LocalAppActions
import io.github.ptt98hd.totch.ui.common.LocalNotchHighlight
import io.github.ptt98hd.totch.ui.common.NotchHighlightOverlay
import io.github.ptt98hd.totch.ui.common.NotchHighlightState
import io.github.ptt98hd.totch.ui.navigation.TotchNavHost
import io.github.ptt98hd.totch.ui.settings.UpdateDialogHost
import io.github.ptt98hd.totch.ui.theme.TotchTheme
import io.github.ptt98hd.totch.ui.theme.isDark
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** FR-U31: the splash stays until the settings are read (the first real frame). */
    private var contentReady = false

    /** LG-2: Android 12 has no per-app locale, so the saved language is applied to this context. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocales.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as TotchApp).container
        keepSplashUntilReady()
        if (savedInstanceState == null) {
            if (intent?.action == updateFailedAction) onUpdateFailed() // UP-17
            lifecycleScope.launch {
                // FR-U29, UP-9: silent check when the app opens, or from the update notification.
                if (intent?.action == ACTION_SHOW_UPDATE || container.settingsRepository.settings.first().updateOnLaunch) {
                    container.updates.check(silent = true)
                }
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ applies the language through LocaleManager (platform layer).
            lifecycleScope.launch {
                var first = true
                container.settingsRepository.settings.map { it.language }.distinctUntilChanged().collect {
                    AppLocales.apply(this@MainActivity, it)
                    if (!first) recreate()
                    first = false
                }
            }
        }
        setContent {
            val loaded: Settings? by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val settings = loaded ?: return@setContent // the splash shows until settings are read
            SideEffect { contentReady = true }
            val dark = settings.theme.isDark(isSystemInDarkTheme())
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            TotchTheme(themeMode = settings.theme, amoledBlack = settings.amoledBlack) {
                val highlight = remember { NotchHighlightState() }
                val area by container.touchArea.area.collectAsStateWithLifecycle()
                val update by container.updates.state.collectAsStateWithLifecycle()
                val requirements by container.systemStatus.requirements.collectAsStateWithLifecycle()
                var donate by rememberSaveable { mutableStateOf(false) }
                val actions = AppActions( // FR-U40, FR-U41: top bar buttons and the Support rows
                    onCheckUpdates = container.updates::openDialog,
                    onDonate = { donate = true },
                    updateBadge = update.badge,
                )
                CompositionLocalProvider(LocalNotchHighlight provides highlight, LocalAppActions provides actions) {
                    Box(Modifier.fillMaxSize()) {
                        TotchNavHost(container)
                        NotchHighlightOverlay(area, highlight) // FR-U8, drawn above every screen
                        // update spec §5, §8: on any screen
                        UpdateDialogHost(
                            container.updates,
                            versionName = BuildConfig.VERSION_NAME,
                            shizukuReady = requirements.shizukuRunning && requirements.shizukuGranted,
                        )
                        if (donate) DonateDialog(onDismiss = { donate = false })
                    }
                }
            }
        }
    }

    /** Sent by the detached `pm install` when it fails (update spec §7); built from the package like the script. */
    private val updateFailedAction get() = "$packageName.action.UPDATE_FAILED"

    private fun onUpdateFailed() {
        (application as TotchApp).container.updates.installFailed()
        Toast.makeText(this, R.string.update_install_failed, Toast.LENGTH_SHORT).show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val updates = (application as TotchApp).container.updates
        if (intent.action == ACTION_SHOW_UPDATE) updates.check(silent = true)
        if (intent.action == updateFailedAction) onUpdateFailed()
    }

    private fun keepSplashUntilReady() {
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (!contentReady) return false
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )
        splashScreen.setOnExitAnimationListener { view ->
            view.animate()
                .alpha(0f)
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setDuration(SPLASH_EXIT_MS)
                .withEndAction { view.remove() }
                .start()
        }
    }

    companion object {
        /** Opens the dialog of a new release; sent by the update notification (update spec §6). */
        const val ACTION_SHOW_UPDATE = "io.github.ptt98hd.totch.action.SHOW_UPDATE"
        private const val SPLASH_EXIT_MS = 250L
    }
}
