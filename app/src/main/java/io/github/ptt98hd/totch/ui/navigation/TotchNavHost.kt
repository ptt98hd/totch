package io.github.ptt98hd.totch.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.ui.gestures.GesturesRoute
import io.github.ptt98hd.totch.ui.home.HomeRoute
import io.github.ptt98hd.totch.ui.notch.NotchSettingsRoute
import io.github.ptt98hd.totch.ui.settings.AppSettingsRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object HomeDestination
@Serializable data object GesturesDestination
@Serializable data object NotchDestination
@Serializable data object AppSettingsDestination

private data class TopLevel(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

/** UI-41: app settings are reached from the top bar; the preferences (notch settings) are the third tab. */
private val topLevel = listOf(
    TopLevel(HomeDestination, HomeDestination::class, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    TopLevel(GesturesDestination, GesturesDestination::class, R.string.nav_gestures, Icons.Outlined.TouchApp, Icons.Filled.TouchApp),
    TopLevel(NotchDestination, NotchDestination::class, R.string.nav_preferences, Icons.Outlined.Tune, Icons.Filled.Tune),
)

private fun NavDestination?.isOn(routeClass: KClass<*>) = this?.hierarchy?.any { it.hasRoute(routeClass) } == true

@Composable
fun TotchNavHost(container: AppContainer, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = topLevel.any { destination.isOn(it.routeClass) }
    val openSettings = { navController.navigate(AppSettingsDestination) { launchSingleTop = true } }
    val back: () -> Unit = { navController.popBackStack() }

    Scaffold(
        modifier = modifier,
        // Each screen has its own Scaffold with the shared top bar; this one only hosts the NavigationBar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevel.forEach { item ->
                        val selected = destination.isOn(item.routeClass)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeDestination,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<HomeDestination> { HomeRoute(container, onOpenSettings = openSettings) }
            composable<GesturesDestination> { GesturesRoute(container, onOpenSettings = openSettings) }
            composable<NotchDestination> { NotchSettingsRoute(container, onOpenSettings = openSettings) }
            composable<AppSettingsDestination> { AppSettingsRoute(container, onBack = back) }
        }
    }
}
