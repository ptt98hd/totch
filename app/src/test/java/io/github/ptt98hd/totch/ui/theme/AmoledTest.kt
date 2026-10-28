package io.github.ptt98hd.totch.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.ptt98hd.totch.core.settings.ThemeMode
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AmoledTest {
    @Test fun `TH-2 amoled turns background and surfaces black and keeps accents`() {
        val base = darkColorScheme()
        val black = amoled(base)
        assertEquals(Color.Black, black.background)
        assertEquals(Color.Black, black.surface)
        assertEquals(Color.Black, black.surfaceContainerLowest)
        listOf(black.surfaceContainerLow, black.surfaceContainer, black.surfaceContainerHigh, black.surfaceContainerHighest)
            .forEach { assertTrue(it.luminance() < 0.03f, "$it") }
        assertTrue(black.surfaceContainer.luminance() <= black.surfaceContainerHigh.luminance())
        assertEquals(base.primary, black.primary)
        assertEquals(base.onSurface, black.onSurface)
    }

    @Test fun `TH-1 theme mode resolves dark`() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
    }
}
