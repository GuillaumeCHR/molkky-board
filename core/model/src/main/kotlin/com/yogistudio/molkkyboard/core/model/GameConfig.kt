package com.yogistudio.molkkyboard.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Options de règles d'une manche (voir `docs/RULES.md` § 8 et `docs/SCORING_SPEC.md` § 1.1).
 * Les valeurs par défaut correspondent au préréglage « Classique ».
 */
@Serializable
data class GameConfig(
    val targetScore: Int = STANDARD_TARGET,
    val overshoot: Overshoot = Overshoot.ResetTo(STANDARD_RESET),
    val missLimit: Int = 3,
    val onMissLimit: MissPenalty = MissPenalty.Eliminate,
    /** Règle des 37 (IMO § 5.7) : à 37 points ou plus, certaines fautes ramènent à 25. */
    val rule37: Boolean = false,
    val faultsCountAsMiss: Boolean = true,
    val endCondition: EndCondition = EndCondition.FirstToTarget,
    val eliminationSetScore: EliminationSetScore = EliminationSetScore.Keep,
    /** Rejoindre exactement le score d'une autre équipe la fait redescendre à la moitié de la cible. */
    val catchUpReset: Boolean = false,
    /** Surcharges par équipe (mode famille / handicap). */
    val teamOverrides: Map<TeamId, TeamOverride> = emptyMap(),
) {
    companion object {
        const val STANDARD_TARGET = 50
        const val STANDARD_RESET = 25

        val Classic = GameConfig()

        val ImoTournament =
            GameConfig(
                rule37 = true,
                eliminationSetScore = EliminationSetScore.WinnerTargetLoserZero,
            )

        /** Mode Ludique par défaut : pas d'élimination (les ratés sont comptés) et mordu ignoré. */
        val Playful =
            GameConfig(
                onMissLimit = MissPenalty.Nothing,
                faultsCountAsMiss = false,
            )
    }
}

/** Mode de règles (`RULES.md` § 8.1), déduit de la configuration et jamais stocké. */
enum class RulesMode {
    /** Règles officielles verrouillées, éventuellement avec le règlement de tournoi IMO. */
    OFFICIAL,

    /** Règles officielles assorties d'exceptions (variantes, mode famille). */
    PLAYFUL,
}

@Serializable
data class TeamOverride(
    val startScore: Int? = null,
    val targetScore: Int? = null,
    val exemptFromMissLimit: Boolean = false,
)

/** Que faire quand un lancer ferait dépasser la cible. */
@Serializable
sealed interface Overshoot {
    /** Retour à un score fixe (25 dans les règles officielles). */
    @Serializable
    @SerialName("reset_to")
    data class ResetTo(
        val score: Int,
    ) : Overshoot

    /** Le lancer est annulé : on garde le score d'avant. */
    @Serializable
    @SerialName("stay")
    data object Stay : Overshoot

    /** Rebond : cible − excédent. */
    @Serializable
    @SerialName("bounce")
    data object Bounce : Overshoot
}

/** Sanction quand une équipe atteint la limite de ratés consécutifs. */
@Serializable
sealed interface MissPenalty {
    @Serializable
    @SerialName("eliminate")
    data object Eliminate : MissPenalty

    @Serializable
    @SerialName("reset_score")
    data class ResetScore(
        val score: Int,
    ) : MissPenalty

    /** Aucune sanction : le compteur continue (utile aux statistiques). */
    @Serializable
    @SerialName("nothing")
    data object Nothing : MissPenalty
}

@Serializable
enum class EndCondition {
    /** La manche s'arrête dès qu'une équipe atteint la cible. */
    FirstToTarget,

    /** On continue pour les places suivantes. */
    PlayForPlaces,
}

@Serializable
enum class EliminationSetScore {
    /** Les scores réels sont conservés. */
    Keep,

    /** IMO : le vainqueur est crédité de la cible, les équipes éliminées de 0. */
    WinnerTargetLoserZero,
}
