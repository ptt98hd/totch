package io.github.ptt98hd.totch.core.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int =
        compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)

    override fun toString() = "$major.$minor.$patch"
}

/** A GitHub release that can be installed: its signed APK and the expected SHA-256 (lowercase hex) when known. */
data class Release(val version: SemVer, val notes: String, val apkUrl: String, val apkSize: Long, val sha256: String?)

/** Reads the latest GitHub release of Totch (docs/specs/update.md §3). */
object Releases {
    const val LATEST_URL = "https://api.github.com/repos/ptt98hd/totch/releases/latest"
    const val DOWNLOAD_PREFIX = "https://github.com/ptt98hd/totch/releases/download/"

    private val version = Regex("""v?(\d+)\.(\d+)\.(\d+)""")
    private val sha256 = Regex("""sha256:([0-9a-fA-F]{64})""")
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class GitHubAsset(
        val name: String,
        val size: Long = 0,
        val digest: String? = null,
        @SerialName("browser_download_url") val url: String = "",
    )

    @Serializable
    private data class GitHubRelease(
        @SerialName("tag_name") val tag: String,
        val body: String? = null,
        val draft: Boolean = false,
        val prerelease: Boolean = false,
        val assets: List<GitHubAsset> = emptyList(),
    )

    /** UP-1 */
    fun parseVersion(value: String): SemVer? = version.matchEntire(value)?.destructured?.let { (major, minor, patch) ->
        SemVer(major.toIntOrNull() ?: return null, minor.toIntOrNull() ?: return null, patch.toIntOrNull() ?: return null)
    }

    /** UP-2, UP-3: `null` for anything that is not a published release with its signed APK on GitHub. */
    fun parseLatest(body: String): Release? {
        val release = try {
            json.decodeFromString<GitHubRelease>(body)
        } catch (_: Exception) {
            return null
        }
        if (release.draft || release.prerelease) return null
        val version = parseVersion(release.tag) ?: return null
        val apk = release.assets.firstOrNull { it.name == "totch-$version.apk" } ?: return null
        if (!apk.url.startsWith(DOWNLOAD_PREFIX) || apk.size <= 0) return null
        val hash = apk.digest?.let { sha256.matchEntire(it) }?.groupValues?.get(1)?.lowercase()
        return Release(version, release.body.orEmpty(), apk.url, apk.size, hash)
    }

    /** UP-4: an unparsable running version (a dev build) is always older. */
    fun isNewer(release: Release, currentVersionName: String): Boolean {
        val current = parseVersion(currentVersionName) ?: return true
        return release.version > current
    }
}
