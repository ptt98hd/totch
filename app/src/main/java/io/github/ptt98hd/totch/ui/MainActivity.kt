package io.github.ptt98hd.totch.ui

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import io.github.ptt98hd.totch.TotchApp
import io.github.ptt98hd.totch.core.settings.Settings
import io.github.ptt98hd.totch.locale.AppLocales
import io.github.ptt98hd.totch.ui.common.LocalNotchHighlight
import io.github.ptt98hd.totch.ui.common.NotchHighlightOverlay
import io.github.ptt98hd.totch.ui.common.NotchHighlightState
import io.github.ptt98hd.totch.ui.navigation.TotchNavHost
import io.github.ptt98hd.totch.ui.theme.TotchTheme
import io.github.ptt98hd.totch.ui.theme.isDark
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** LG-2: Android 12 has no per-app locale, so the saved language is applied to this context. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocales.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as TotchApp).container
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
            val settings = loaded ?: return@setContent // the window background shows until settings are read
            val dark = settings.theme.isDark(isSystemInDarkTheme())
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            TotchTheme(themeMode = settings.theme, amoledBlack = settings.amoledBlack) {
                val highlight = remember { NotchHighlightState() }
                val region by container.touchArea.region.collectAsStateWithLifecycle()
                CompositionLocalProvider(LocalNotchHighlight provides highlight) {
                    Box(Modifier.fillMaxSize()) {
                        TotchNavHost(container)
                        NotchHighlightOverlay(region, highlight) // FR-U8, drawn above every screen
                    }
                }
            }
        }
    }
}
