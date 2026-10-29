package io.github.ptt98hd.totch.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.navigation.NavBackStackEntry
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
import io.github.ptt98hd.totch.ui.common.NotchIcons
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

/** UI-41: Home, Gestures, Notch (preferences) and Settings (app settings), in this order. */
private val topLevel = listOf(
    TopLevel(HomeDestination, HomeDestination::class, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    TopLevel(GesturesDestination, GesturesDestination::class, R.string.nav_gestures, Icons.Outlined.TouchApp, Icons.Filled.TouchApp),
    TopLevel(NotchDestination, NotchDestination::class, R.string.nav_notch, NotchIcons.Outlined, NotchIcons.Filled),
    TopLevel(AppSettingsDestination, AppSettingsDestination::class, R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
)

private fun NavDestination?.isOn(routeClass: KClass<*>) = this?.hierarchy?.any { it.hasRoute(routeClass) } == true

private fun NavBackStackEntry.tabIndex(): Int = topLevel.indexOfFirst { destination.isOn(it.routeClass) }

private const val TRANSITION_MS = 300

/** UI-43: +1 when moving to a tab on the right, -1 to the left. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.direction(): Int =
    if (targetState.tabIndex() >= initialState.tabIndex()) 1 else -1

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(): EnterTransition {
    val direction = direction()
    return slideInHorizontally(tween(TRANSITION_MS, easing = FastOutSlowInEasing)) { it / 5 * direction } +
        fadeIn(tween(TRANSITION_MS))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(): ExitTransition {
    val direction = direction()
    return slideOutHorizontally(tween(TRANSITION_MS, easing = FastOutSlowInEasing)) { -it / 5 * direction } +
        fadeOut(tween(TRANSITION_MS / 2))
}

/** [navLabels]: labels under the bottom bar icons (FR-U49). */
@Composable
fun TotchNavHost(container: AppContainer, navLabels: Boolean, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    Scaffold(
        modifier = modifier,
        // Each screen has its own Scaffold with the shared top bar; this one only hosts the NavigationBar.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
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
                        icon = {
                            // Without labels the icon names the tab for accessibility.
                            val description = if (navLabels) null else stringResource(item.label)
                            Icon(if (selected) item.selectedIcon else item.icon, contentDescription = description)
                        },
                        label = if (navLabels) {
                            { Text(stringResource(item.label)) }
                        } else {
                            null
                        },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeDestination,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { enter() },
            exitTransition = { exit() },
            popEnterTransition = { enter() },
            popExitTransition = { exit() },
        ) {
            composable<HomeDestination> { HomeRoute(container) }
            composable<GesturesDestination> { GesturesRoute(container) }
            composable<NotchDestination> { NotchSettingsRoute(container) }
            composable<AppSettingsDestination> { AppSettingsRoute(container) }
        }
    }
}
