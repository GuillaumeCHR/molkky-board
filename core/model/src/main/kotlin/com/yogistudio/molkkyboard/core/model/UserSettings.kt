package com.yogistudio.molkkyboard.core.model

/** Mode de saisie de l'écran de jeu. */
enum class InputMode {
    /** Grille des 12 quilles (par défaut) : alimente les statistiques par quille. */
    PINS,

    /** Pavé 0 à 12 : saisie rapide, sans statistiques par quille. */
    QUICK,
}

/** Réglages de l'utilisateur (la langue est gérée à part, par la langue de l'application Android). */
data class UserSettings(
    val inputMode: InputMode = InputMode.PINS,
    val sunlight: Boolean = false,
    val voiceAnnouncements: Boolean = false,
    /** Débit des annonces vocales (1 = normal). */
    val voiceRate: Float = 1f,
    /** Volume des annonces vocales, de 0 à 1 (relatif au volume « média » du téléphone). */
    val voiceVolume: Float = 1f,
    /** Notification persistante de la partie en cours. */
    val ongoingNotification: Boolean = true,
    /** L'autorisation d'afficher les notifications a déjà été proposée (on ne la redemande pas à chaque partie). */
    val notificationPrompted: Boolean = false,
    val sounds: Boolean = false,
    val coach: Boolean = true,
    /** Intro courte (≈ 1,5 s), proposée à partir du deuxième lancement. */
    val shortIntro: Boolean = false,
    val launchCount: Int = 0,
    val onboardingDone: Boolean = false,
    /** Exceptions par défaut des nouvelles parties en mode Ludique (`RULES.md` § 8.1), sans surcharge par équipe. */
    val playfulRules: GameConfig = GameConfig.Playful,
) {
    /** La version courte de l'intro n'est proposée qu'à partir du deuxième lancement. */
    val canChooseShortIntro: Boolean get() = launchCount >= 2
}
