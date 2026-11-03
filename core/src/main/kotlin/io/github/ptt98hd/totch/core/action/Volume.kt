package io.github.ptt98hd.totch.core.action

/** `AudioManager.FLAG_SHOW_UI`: the system volume panel shows the change. */
const val FLAG_SHOW_UI: Int = 1

/** AD-15 (FR-A24): no system panel while the island shows the volume. */
fun volumeFlags(islandShowsVolume: Boolean): Int = if (islandShowsVolume) 0 else FLAG_SHOW_UI
