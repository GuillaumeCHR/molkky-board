package fr.molkky.core.model

// Règles dérivées de la configuration. Elles vivent hors de la classe @Serializable pour que la
// couverture de tests mesure ce code-là et non le code généré par kotlinx-serialization.

/** Une partie avec handicap compte dans les victoires mais pas dans les records « purs ». */
val GameConfig.handicap: Boolean get() = teamOverrides.isNotEmpty()

/** Règles standard : cible 50 et retour à 25 (condition du record de victoire la plus rapide). */
val GameConfig.isStandard: Boolean
    get() =
        targetScore == GameConfig.STANDARD_TARGET &&
            overshoot == Overshoot.ResetTo(GameConfig.STANDARD_RESET) &&
            !handicap

fun GameConfig.targetFor(team: TeamId): Int = teamOverrides[team]?.targetScore ?: targetScore

fun GameConfig.startScoreFor(team: TeamId): Int = teamOverrides[team]?.startScore ?: 0

fun GameConfig.isExemptFromMissLimit(team: TeamId): Boolean = teamOverrides[team]?.exemptFromMissLimit ?: false
