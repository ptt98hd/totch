package io.github.ptt98hd.totch.core.update

import io.github.ptt98hd.totch.core.settings.UpdateInterval
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdatePolicyTest {
    private val release = Release(SemVer(1, 7, 0), "", Releases.DOWNLOAD_PREFIX + "v1.7.0/totch-1.7.0.apk", 10, null)

    @Test fun `UP-10 interval days and names`() {
        assertEquals(listOf(0, 1, 7, 30), UpdateInterval.entries.map { it.days })
        assertEquals(
            listOf("\"never\"", "\"daily\"", "\"weekly\"", "\"monthly\""),
            UpdateInterval.entries.map { Json.encodeToString(UpdateInterval.serializer(), it) },
        )
    }

    @Test fun `UP-11 job period`() {
        assertNull(UpdatePolicy.periodMillis(UpdateInterval.NEVER))
        assertEquals(86_400_000L, UpdatePolicy.periodMillis(UpdateInterval.DAILY))
        assertEquals(7 * 86_400_000L, UpdatePolicy.periodMillis(UpdateInterval.WEEKLY))
    }

    @Test fun `UP-12 each newer version is notified once`() {
        assertFalse(UpdatePolicy.shouldNotify(null, "1.6.1", null))
        assertFalse(UpdatePolicy.shouldNotify(release, "1.7.0", null))
        assertFalse(UpdatePolicy.shouldNotify(release, "1.6.1", "1.7.0"))
        assertTrue(UpdatePolicy.shouldNotify(release, "1.6.1", null))
        assertTrue(UpdatePolicy.shouldNotify(release, "1.6.1", "1.6.5"))
    }
}
