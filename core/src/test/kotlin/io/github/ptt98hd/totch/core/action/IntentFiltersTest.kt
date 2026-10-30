package io.github.ptt98hd.totch.core.action

import io.github.ptt98hd.totch.core.model.IntentTarget
import kotlin.test.Test
import kotlin.test.assertEquals

class IntentFiltersTest {
    @Test fun `IN-1 filters with data are dropped, actions merged without duplicates`() {
        val filters = listOf(
            ManifestFilter(listOf("com.a.OPEN", "com.a.EDIT"), hasData = false),
            ManifestFilter(listOf("com.a.VIEW"), hasData = true),
            ManifestFilter(listOf("com.a.OPEN", "com.a.SHARE", "bad action"), hasData = false),
        )
        assertEquals(
            listOf("com.a.OPEN", "com.a.EDIT", "com.a.SHARE"),
            IntentFilters.usableActions(IntentTarget.ACTIVITY, filters),
        )
    }

    @Test fun `IN-2 activities drop MAIN`() {
        val filters = listOf(ManifestFilter(listOf("android.intent.action.MAIN", "com.a.OPEN"), hasData = false))
        assertEquals(listOf("com.a.OPEN"), IntentFilters.usableActions(IntentTarget.ACTIVITY, filters))
    }

    @Test fun `IN-3 receivers drop system broadcasts`() {
        val filters = listOf(
            ManifestFilter(
                listOf("android.intent.action.BOOT_COMPLETED", "com.android.vending.INSTALL_REFERRER", "com.a.PING", "REFRESH"),
                hasData = false,
            ),
        )
        assertEquals(listOf("com.a.PING", "REFRESH"), IntentFilters.usableActions(IntentTarget.BROADCAST, filters))
    }

    @Test fun `IN-4 class names are made fully qualified`() {
        assertEquals("com.a.ui.Main", IntentFilters.resolveClassName("com.a", ".ui.Main"))
        assertEquals("com.a.Main", IntentFilters.resolveClassName("com.a", "Main"))
        assertEquals("org.b.X", IntentFilters.resolveClassName("com.a", "org.b.X"))
    }

    @Test fun `IN-5 short action is the last segment`() {
        assertEquals("TOGGLE", IntentFilters.shortIntentAction("com.a.action.TOGGLE"))
        assertEquals("REFRESH", IntentFilters.shortIntentAction("REFRESH"))
    }
}
