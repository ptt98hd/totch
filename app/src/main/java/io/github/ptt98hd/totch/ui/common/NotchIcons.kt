package io.github.ptt98hd.totch.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Phone outline with a pill-shaped notch, for the Notch tab (FR-U19). */
object NotchIcons {
    private const val BODY = "M8,2h8a3,3 0 0 1 3,3v14a3,3 0 0 1 -3,3h-8a3,3 0 0 1 -3,-3v-14a3,3 0 0 1 3,-3z"
    private const val SCREEN = "M8,4h8a1,1 0 0 1 1,1v14a1,1 0 0 1 -1,1h-8a1,1 0 0 1 -1,-1v-14a1,1 0 0 1 1,-1z"
    private const val NOTCH = "M10.5,5.25h3a0.75,0.75 0 0 1 0,1.5h-3a0.75,0.75 0 0 1 0,-1.5z"

    val Outlined: ImageVector by lazy { icon("Notch.Outlined", listOf(BODY + SCREEN, NOTCH)) }

    /** Filled body with the notch cut out. */
    val Filled: ImageVector by lazy { icon("Notch.Filled", listOf(BODY + NOTCH)) }

    private fun icon(name: String, paths: List<String>): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { addPath(addPathNodes(it), pathFillType = PathFillType.EvenOdd, fill = SolidColor(Color.Black)) }
        }.build()
}
