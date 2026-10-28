package io.github.ptt98hd.totch.ui.settings

import io.github.ptt98hd.totch.core.settings.AppLanguage
import io.github.ptt98hd.totch.core.settings.ThemeMode
import io.github.ptt98hd.totch.ui.FakeSettingsRepository
import io.github.ptt98hd.totch.ui.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `UI-32 appearance, language and boot setters persist`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = AppSettingsViewModel(repo)
        vm.setTheme(ThemeMode.LIGHT)
        vm.setAmoledBlack(true)
        vm.setLanguage(AppLanguage.ENGLISH)
        vm.setStartOnBoot(false)
        with(repo.current) {
            assertEquals(ThemeMode.LIGHT, theme)
            assertTrue(amoledBlack)
            assertEquals(AppLanguage.ENGLISH, language)
            assertFalse(startOnBoot)
        }
    }
}
