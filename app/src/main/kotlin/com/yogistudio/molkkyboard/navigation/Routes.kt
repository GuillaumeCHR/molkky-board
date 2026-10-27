package com.yogistudio.molkkyboard.navigation

import kotlinx.serialization.Serializable

// Routes typées de Navigation Compose.

/** Intro animée (destination de départ, retirée de la pile une fois finie). */
@Serializable
data object IntroRoute

@Serializable
data object HomeRoute

@Serializable
data object HistoryRoute

@Serializable
data object HallOfFameRoute

@Serializable
data object SettingsRoute

@Serializable
data object AboutRoute

/** Réglages › Règles du jeu. */
@Serializable
data object RulesRoute

/** Réglages › Règles du mode Ludique. */
@Serializable
data object PlayfulRulesRoute

@Serializable
data object JoinRoute

@Serializable
data object SetupRoute

/** Écran de jeu ; le nom du paramètre est lu par `GameViewModel` (`GAME_ID`). */
@Serializable
data class GameRoute(
    val gameId: String,
)

/** Hall of Fame › Statistiques des joueurs. */
@Serializable
data object StatsRoute

/** Vitrine du design system (build debug uniquement). */
@Serializable
data object ShowcaseRoute
