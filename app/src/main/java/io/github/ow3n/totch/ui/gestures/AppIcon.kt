package io.github.ow3n.totch.ui.gestures

import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Process-wide cache of app icons, loaded off the main thread. */
object AppIconCache {
    private val cache = LruCache<String, ImageBitmap>(256)

    fun peek(packageName: String): ImageBitmap? = cache.get(packageName)

    suspend fun load(pm: PackageManager, packageName: String, sizePx: Int): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                pm.getApplicationIcon(packageName).toBitmap(sizePx, sizePx).asImageBitmap()
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }?.also { cache.put(packageName, it) }
    }
}

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val inPreview = LocalInspectionMode.current
    val pm = LocalContext.current.packageManager
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap by produceState(if (inPreview) null else AppIconCache.peek(packageName), packageName, sizePx) {
        if (!inPreview && value == null) value = AppIconCache.load(pm, packageName, sizePx)
    }
    val current = bitmap
    if (current != null) {
        Image(bitmap = current, contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(modifier.size(size).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
    }
}
