package io.github.ptt98hd.totch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.ptt98hd.totch.core.settings.ThemeMode

/** TH-1: whether [this] mode renders dark, given the system setting. */
fun ThemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** TH-2: pure black background and surfaces with near-black containers; accents are kept. */
fun amoled(scheme: ColorScheme): ColorScheme = scheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0B),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF1F1F1F),
)

/** Material 3 theme with dynamic color (minSdk 31 guarantees Android 12+). */
@Composable
fun TotchTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dark = themeMode.isDark(isSystemInDarkTheme())
    val colorScheme = when {
        !dark -> dynamicLightColorScheme(context)
        amoledBlack -> amoled(dynamicDarkColorScheme(context))
        else -> dynamicDarkColorScheme(context)
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
