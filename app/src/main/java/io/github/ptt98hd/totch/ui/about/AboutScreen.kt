package io.github.ptt98hd.totch.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.ptt98hd.totch.BuildConfig
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.ui.common.Gutter
import io.github.ptt98hd.totch.ui.common.TonalIcon
import io.github.ptt98hd.totch.ui.common.withGutters
import io.github.ptt98hd.totch.ui.theme.TotchTheme

/** About has static content only, so it needs no ViewModel. */
@Composable
fun AboutRoute(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AboutScreen(versionName = BuildConfig.VERSION_NAME, onOpenUrl = { uriHandler.openUri(it) }, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(versionName: String, onOpenUrl: (String) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val sourceUrl = stringResource(R.string.about_source_url)
    val shizukuUrl = stringResource(R.string.about_shizuku_url)
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding.withGutters(top = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item(key = "header") {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    )
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        stringResource(R.string.about_version, versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.about_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp, start = Gutter, end = Gutter),
                    )
                }
            }
            item(key = "links") {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    AboutRow(Icons.Outlined.Gavel, stringResource(R.string.about_license_title), stringResource(R.string.about_license))
                    HorizontalDivider(Modifier.padding(horizontal = Gutter))
                    AboutRow(
                        Icons.Outlined.Code,
                        stringResource(R.string.about_source_title),
                        sourceUrl,
                        onClick = { onOpenUrl(sourceUrl) },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = Gutter))
                    AboutRow(
                        Icons.Outlined.Favorite,
                        stringResource(R.string.about_credits_title),
                        stringResource(R.string.about_shizuku_credit),
                        onClick = { onOpenUrl(shizukuUrl) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutRow(icon: ImageVector, title: String, summary: String, onClick: (() -> Unit)? = null) {
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = { TonalIcon(icon) },
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = onClick?.let { { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    TotchTheme { AboutScreen(versionName = "1.0.0", onOpenUrl = {}, onBack = {}) }
}
