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

/** « Rejoindre » ; [link] n'est rempli qu'en debug (invitation passée par adb à un émulateur sans caméra). */
@Serializable
data class JoinRoute(
    val link: String? = null,
)

/** « Inviter des téléphones » pour la partie [gameId] (partie à côté). */
@Serializable
data class InviteRoute(
    val gameId: String,
)

@Serializable
data object SetupRoute

/** Écran de jeu ; le nom du paramètre est lu par `GameViewModel` (`GAME_ID`). */
@Serializable
data class GameRoute(
    val gameId: String,
)

/** Partage d'une partie (vignette, vidéo) ; le nom du paramètre est lu par `ShareViewModel` (`GAME_ID`). */
@Serializable
data class ShareRoute(
    val gameId: String,
)

/** Hall of Fame › Statistiques des joueurs. */
@Serializable
data object StatsRoute

/** Placement des quilles pas à pas (accueil, raccourci de l'icône). */
@Serializable
data object PlacementRoute

/** Licences des bibliothèques open source (À propos › Crédits). */
@Serializable
data object LicensesRoute
