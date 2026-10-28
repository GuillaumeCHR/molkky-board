package com.yogistudio.molkkyboard.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.yogistudio.molkkyboard.R
import com.yogistudio.molkkyboard.core.designsystem.component.MolkkyBoardLogotype
import com.yogistudio.molkkyboard.core.designsystem.icon.MBIcons
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import com.yogistudio.molkkyboard.core.model.UserSettings
import com.yogistudio.molkkyboard.crash.CrashReportDialog
import com.yogistudio.molkkyboard.feature.game.GameScreen
import com.yogistudio.molkkyboard.feature.game.PlacementScreen
import com.yogistudio.molkkyboard.feature.game.RulesScreen
import com.yogistudio.molkkyboard.feature.history.HistoryScreen
import com.yogistudio.molkkyboard.feature.intro.IntroScreen
import com.yogistudio.molkkyboard.feature.setup.PlayfulRulesScreen
import com.yogistudio.molkkyboard.feature.setup.SetupScreen
import com.yogistudio.molkkyboard.feature.share.ShareScreen
import com.yogistudio.molkkyboard.feature.stats.HallOfFameScreen
import com.yogistudio.molkkyboard.feature.stats.StatsScreen
import com.yogistudio.molkkyboard.notification.NotificationPermissionPrompt
import com.yogistudio.molkkyboard.startup.StartupState
import com.yogistudio.molkkyboard.ui.AboutScreen
import com.yogistudio.molkkyboard.ui.EmptyStateScreen
import com.yogistudio.molkkyboard.ui.HomeScreen
import com.yogistudio.molkkyboard.ui.LicensesScreen
import com.yogistudio.molkkyboard.ui.SettingsScreen
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
fun MolkkyBoardNavHost(
    startup: StartupState,
    target: LaunchTarget? = null,
    onTargetOpened: () -> Unit = {},
    crashReport: String? = null,
    onCrashReportHandled: () -> Unit = {},
    settings: UserSettings? = null,
    onNotificationPrompted: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    LaunchTargetEffect(navController, target, destination, onTargetOpened)
    // L'écran de jeu est en plein écran : pas de barre du bas.
    val showBar = destination?.let(::showsBottomBar) ?: true

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
        SharedTransitionLayout {
            val shared = this
            NavHost(
                navController = navController,
                startDestination = IntroRoute,
                modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            ) {
                composable<IntroRoute> {
                    val logotype = with(shared) { sharedLogotype(this@composable) }
                    IntroScreen(
                        loadState = startup.load,
                        sounds = startup.sounds,
                        onFinished = {
                            navController.navigate(HomeRoute) { popUpTo<IntroRoute> { inclusive = true } }
                        },
                        logotype = { MolkkyBoardLogotype(it.then(logotype), fontSize = INTRO_LOGOTYPE_SIZE) },
                    )
                }
                composable<HomeRoute> {
                    if (crashReport != null) CrashReportDialog(crashReport, onCrashReportHandled)
                    HomeScreen(
                        onNewGame = { navController.navigate(SetupRoute) },
                        onJoin = { navController.navigate(JoinRoute) },
                        onResume = { navController.navigate(GameRoute(it)) },
                        onRules = { navController.navigate(RulesRoute) },
                        onPlacement = { navController.navigate(PlacementRoute) },
                        logotypeModifier = with(shared) { sharedLogotype(this@composable) },
                    )
                }
                composable<HistoryRoute> { HistoryScreen(onOpenGame = { navController.navigate(GameRoute(it)) }) }
                composable<HallOfFameRoute> {
                    HallOfFameScreen(
                        onOpenGame = { navController.navigate(GameRoute(it)) },
                        onOpenStats = { navController.navigate(StatsRoute) },
                    )
                }
                composable<StatsRoute> { StatsScreen(onBack = navController::navigateUp) }
                composable<ShareRoute> { ShareScreen(onBack = navController::navigateUp) }
                composable<SettingsRoute> {
                    SettingsScreen(
                        onAbout = { navController.navigate(AboutRoute) },
                        onPlayfulRules = { navController.navigate(PlayfulRulesRoute) },
                        onRules = { navController.navigate(RulesRoute) },
                        onReplayGuide = { navController.navigate(SetupRoute) },
                    )
                }
                composable<AboutRoute> {
                    AboutScreen(
                        onBack = navController::navigateUp,
                        onLicenses = { navController.navigate(LicensesRoute) },
                    )
                }
                composable<LicensesRoute> { LicensesScreen(onBack = navController::navigateUp) }
                composable<PlayfulRulesRoute> { PlayfulRulesScreen(onBack = navController::navigateUp) }
                composable<RulesRoute> { RulesScreen(onBack = navController::navigateUp) }
                composable<PlacementRoute> { PlacementScreen(onBack = navController::navigateUp) }
                composable<JoinRoute> {
                    EmptyStateScreen(R.string.join_title, R.string.join_empty, onBack = navController::navigateUp)
                }
                composable<SetupRoute> {
                    SetupScreen(
                        onBack = navController::navigateUp,
                        onGameCreated = { id ->
                            navController.navigate(GameRoute(id)) {
                                popUpTo<SetupRoute> {
                                    inclusive =
                                        true
                                }
                            }
                        },
                    )
                }
                composable<GameRoute> { entry ->
                    val gameId = entry.toRoute<GameRoute>().gameId
                    GameScreen(
                        onShare = { navController.navigate(ShareRoute(gameId)) },
                        onBack = navController::navigateUp,
                        onNewGame = { navController.navigate(SetupRoute) { popUpTo<HomeRoute>() } },
                        onHome = { navController.popBackStack<HomeRoute>(inclusive = false) },
                        // Revanche : la nouvelle partie remplace l'ancienne dans la pile.
                        onOpenGame = { id ->
                            navController.navigate(GameRoute(id)) { popUpTo<GameRoute> { inclusive = true } }
                        },
                    )
                    NotificationPermissionPrompt(
                        prompted = settings?.notificationPrompted,
                        enabled = settings?.ongoingNotification == true,
                        onPrompted = onNotificationPrompted,
                    )
                }
            }
        }
    }
}

private const val LOGOTYPE_KEY = "logotype"
private val INTRO_LOGOTYPE_SIZE = 52.sp

/** Le logotype de l'intro glisse et se réduit jusqu'à l'en-tête de l'accueil (même élément). */
@Composable
private fun SharedTransitionScope.sharedLogotype(scope: AnimatedVisibilityScope): Modifier =
    Modifier.sharedBounds(
        sharedContentState = rememberSharedContentState(LOGOTYPE_KEY),
        animatedVisibilityScope = scope,
        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
    )

/** Notification « Saisir le lancer » ou raccourci de l'icône : l'écran s'ouvre une fois l'intro passée. */
@Composable
private fun LaunchTargetEffect(
    navController: NavHostController,
    target: LaunchTarget?,
    destination: NavDestination?,
    onTargetOpened: () -> Unit,
) {
    LaunchedEffect(target, destination) {
        val wanted = target ?: return@LaunchedEffect
        if (destination == null || destination.hasRoute(IntroRoute::class)) return@LaunchedEffect
        val route: Any =
            when (wanted) {
                is LaunchTarget.Game -> GameRoute(wanted.id)
                LaunchTarget.NewGame -> SetupRoute
                LaunchTarget.Placement -> PlacementRoute
                LaunchTarget.Rules -> RulesRoute
            }
        navController.navigate(route) {
            popUpTo<HomeRoute>()
            launchSingleTop = true
        }
        onTargetOpened()
    }
}

private fun showsBottomBar(d: NavDestination): Boolean =
    TopLevel.entries.any { d.hasRoute(it.routeClass) } ||
        listOf(
            AboutRoute::class,
            LicensesRoute::class,
            PlayfulRulesRoute::class,
            RulesRoute::class,
            PlacementRoute::class,
            StatsRoute::class,
        ).any { d.hasRoute(it) }
