package com.yogistudio.molkkyboard.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.ui.AboutScreen
import com.yogistudio.molkkyboard.ui.EmptyStateScreen
import com.yogistudio.molkkyboard.ui.GameScreen
import com.yogistudio.molkkyboard.ui.HomeScreen
import com.yogistudio.molkkyboard.ui.SettingsScreen
import com.yogistudio.molkkyboard.ui.ShowcaseScreen
import kotlin.reflect.KClass

/** Onglets de la barre de navigation du bas : toujours visibles au pouce, sauf pendant une partie. */
private enum class TopLevel(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    @StringRes val label: Int,
) {
    Home(HomeRoute, HomeRoute::class, MBIcons.Home, R.string.nav_home),
    History(HistoryRoute, HistoryRoute::class, MBIcons.History, R.string.nav_history),
    HallOfFame(HallOfFameRoute, HallOfFameRoute::class, MBIcons.Trophy, R.string.nav_hall_of_fame),
    Settings(SettingsRoute, SettingsRoute::class, MBIcons.Settings, R.string.nav_settings),
}

@Composable
fun MolkkyBoardNavHost(sunlight: Boolean, onSunlightChange: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    // L'écran de jeu et la vitrine sont en plein écran : pas de barre du bas.
    val showBar =
        destination?.let { d -> TopLevel.entries.any { d.hasRoute(it.routeClass) } || d.hasRoute(AboutRoute::class) }
            ?: true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    TopLevel.entries.forEach { item ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                            colors =
                                NavigationBarItemDefaults.colors(
                                    indicatorColor = MolkkyBoardTheme.colors.saffron,
                                    selectedIconColor = MolkkyBoardTheme.colors.onSaffron,
                                ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onNewGame = { navController.navigate(GameRoute) },
                    onJoin = { navController.navigate(JoinRoute) },
                )
            }
            composable<HistoryRoute> { EmptyStateScreen(R.string.nav_history, R.string.history_empty) }
            composable<HallOfFameRoute> { EmptyStateScreen(R.string.nav_hall_of_fame, R.string.hall_of_fame_empty) }
            composable<SettingsRoute> {
                SettingsScreen(
                    sunlight = sunlight,
                    onSunlightChange = onSunlightChange,
                    onAbout = { navController.navigate(AboutRoute) },
                    onShowcase = { navController.navigate(ShowcaseRoute) },
                )
            }
            composable<AboutRoute> { AboutScreen(onBack = navController::navigateUp) }
            composable<JoinRoute> {
                EmptyStateScreen(R.string.join_title, R.string.join_empty, onBack = navController::navigateUp)
            }
            composable<GameRoute> { GameScreen(onBack = navController::navigateUp) }
            composable<ShowcaseRoute> { ShowcaseScreen(onBack = navController::navigateUp) }
        }
    }
}
