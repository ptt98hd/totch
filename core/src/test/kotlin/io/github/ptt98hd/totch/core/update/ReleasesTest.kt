package io.github.ptt98hd.totch.core.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReleasesTest {
    private val hash = "ee12cc62682b76ce6ead81a5f22b6229f6c200719dda5a1dd9f080532c9e1cea"

    private fun asset(name: String, url: String = Releases.DOWNLOAD_PREFIX + "v1.6.0/$name", size: Long = 3_000_000, digest: String? = "sha256:$hash") =
        """{"name":"$name","size":$size,${digest?.let { "\"digest\":\"$it\"," } ?: ""}"browser_download_url":"$url"}"""

    private fun release(
        tag: String = "v1.6.0",
        assets: List<String> = listOf(asset("totch-1.6.0-unsigned.apk"), asset("totch-1.6.0.apk")),
        extra: String = "",
    ) = """{"tag_name":"$tag","name":"Totch","body":"* Notes","draft":false,"prerelease":false,$extra"assets":[${assets.joinToString(",")}]}"""

    @Test fun `UP-1 parseVersion accepts plain semver with optional v`() {
        assertEquals(SemVer(1, 6, 0), Releases.parseVersion("v1.6.0"))
        assertEquals(SemVer(1, 6, 0), Releases.parseVersion("1.6.0"))
        listOf("1.6", "v1.6.0-beta", "x", "", "v1.6.0.1").forEach { assertNull(Releases.parseVersion(it), it) }
        assertTrue(SemVer(1, 10, 0) > SemVer(1, 9, 0))
        assertTrue(SemVer(2, 0, 0) > SemVer(1, 99, 99))
    }

    @Test fun `UP-2 parseLatest picks the signed apk of the tag`() {
        val r = Releases.parseLatest(release(extra = "\"unknown\":{\"a\":1},"))!!
        assertEquals(SemVer(1, 6, 0), r.version)
        assertEquals("* Notes", r.notes)
        assertEquals(Releases.DOWNLOAD_PREFIX + "v1.6.0/totch-1.6.0.apk", r.apkUrl)
        assertEquals(3_000_000, r.apkSize)
        assertEquals(hash, r.sha256)
        val upper = Releases.parseLatest(release(assets = listOf(asset("totch-1.6.0.apk", digest = "sha256:${hash.uppercase()}"))))!!
        assertEquals(hash, upper.sha256)
        assertNull(Releases.parseLatest(release(assets = listOf(asset("totch-1.6.0.apk", digest = null))))!!.sha256)
        assertNull(Releases.parseLatest(release(assets = listOf(asset("totch-1.6.0.apk", digest = "md5:abc"))))!!.sha256)
        assertEquals("", Releases.parseLatest(release().replace("\"* Notes\"", "null"))!!.notes)
    }

    @Test fun `UP-3 parseLatest rejects unusable releases`() {
        listOf(
            "",
            "{",
            """{"assets":[]}""",
            release(tag = "nightly"),
            release().replace("\"draft\":false", "\"draft\":true"),
            release().replace("\"prerelease\":false", "\"prerelease\":true"),
            release(assets = listOf(asset("totch-1.6.0-unsigned.apk"), asset("totch-1.5.0.apk"))),
            release(assets = listOf(asset("totch-1.6.0.apk", url = "https://evil.example/totch-1.6.0.apk"))),
            release(assets = listOf(asset("totch-1.6.0.apk", size = 0))),
        ).forEach { assertNull(Releases.parseLatest(it), it) }
    }

    @Test fun `UP-4 isNewer compares with the running version`() {
        val r = Releases.parseLatest(release())!!
        assertTrue(Releases.isNewer(r, "1.5.0"))
        assertTrue(Releases.isNewer(r, "1.5.10"))
        assertFalse(Releases.isNewer(r, "1.6.0"))
        assertFalse(Releases.isNewer(r, "1.10.0"))
        assertTrue(Releases.isNewer(r, "dev"))
    }
}
