package io.github.ptt98hd.totch.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
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
import androidx.navigation.toRoute
import io.github.ptt98hd.totch.AppContainer
import io.github.ptt98hd.totch.R
import io.github.ptt98hd.totch.core.model.Gesture
import io.github.ptt98hd.totch.ui.about.AboutRoute
import io.github.ptt98hd.totch.ui.gestures.AppPickerRoute
import io.github.ptt98hd.totch.ui.gestures.GesturesRoute
import io.github.ptt98hd.totch.ui.home.HomeRoute
import io.github.ptt98hd.totch.ui.settings.SettingsRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable data object HomeDestination
@Serializable data object GesturesDestination
@Serializable data object SettingsDestination
@Serializable data object AboutDestination
@Serializable data class AppPickerDestination(val gesture: String)

private data class TopLevel(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val topLevel = listOf(
    TopLevel(HomeDestination, HomeDestination::class, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    TopLevel(GesturesDestination, GesturesDestination::class, R.string.nav_gestures, Icons.Outlined.TouchApp, Icons.Filled.TouchApp),
    TopLevel(SettingsDestination, SettingsDestination::class, R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
)

private fun NavDestination?.isOn(routeClass: KClass<*>) = this?.hierarchy?.any { it.hasRoute(routeClass) } == true

@Composable
fun TotchNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = topLevel.any { destination.isOn(it.routeClass) }

    Scaffold(
        // Each screen has its own Scaffold handling insets; this one only hosts the NavigationBar.
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
            composable<HomeDestination> { HomeRoute(container) }
            composable<GesturesDestination> {
                GesturesRoute(container, onPickApp = { navController.navigate(AppPickerDestination(it.name)) })
            }
            composable<SettingsDestination> {
                SettingsRoute(container, onAbout = { navController.navigate(AboutDestination) })
            }
            composable<AboutDestination> { AboutRoute(onBack = { navController.popBackStack() }) }
            composable<AppPickerDestination> { entry ->
                val gesture = Gesture.valueOf(entry.toRoute<AppPickerDestination>().gesture)
                AppPickerRoute(container, gesture, onBack = { navController.popBackStack() })
            }
        }
    }
}
