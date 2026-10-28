package com.yogistudio.molkkyboard.navigation

/** Écran demandé de l'extérieur : notification de partie en cours ou raccourci de l'icône de l'app. */
sealed interface LaunchTarget {
    data class Game(
        val id: String,
    ) : LaunchTarget

    data object NewGame : LaunchTarget

    /** Builds de debug seulement : invitation transmise par adb (émulateur sans caméra). */
    data class Join(
        val link: String,
    ) : LaunchTarget

    data object Placement : LaunchTarget

    data object Rules : LaunchTarget
}
