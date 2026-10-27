package com.yogistudio.molkkyboard.feature.setup

import com.yogistudio.molkkyboard.core.model.EndCondition
import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.MissPenalty
import com.yogistudio.molkkyboard.core.model.Overshoot

// Exceptions du mode Ludique (`RULES.md` § 8.1), exprimées en choix d'interface au-dessus de GameConfig.
// « Moitié de la cible » vaut 25 pour une cible de 50 : c'est la règle officielle, qui suit la cible si on la change.

/** Sanction quand une équipe atteint la limite de ratés consécutifs. */
enum class MissChoice { ELIMINATE, NONE, RESET_ZERO, RESET_HALF }

/** Que faire quand un lancer ferait dépasser la cible. */
enum class OvershootChoice { RESET_HALF, RESET_ZERO, STAY, BOUNCE }

/** Une exception active, pour le résumé « Ludique · sans élimination · cible 40 ». */
sealed interface RuleException {
    data class Miss(
        val choice: MissChoice,
    ) : RuleException

    data class MissLimit(
        val limit: Int,
    ) : RuleException

    data class Overshoot(
        val choice: OvershootChoice,
    ) : RuleException

    data class Target(
        val target: Int,
    ) : RuleException

    data object PlayForPlaces : RuleException

    data object CatchUpReset : RuleException
}

object PlayfulRules {
    const val MIN_TARGET = 10
    const val MAX_TARGET = 100
    const val TARGET_STEP = 5
    const val MIN_MISS_LIMIT = 2
    const val MAX_MISS_LIMIT = 5
}

private val GameConfig.half: Int get() = targetScore / 2

val GameConfig.missChoice: MissChoice
    get() =
        when (val penalty = onMissLimit) {
            MissPenalty.Eliminate -> MissChoice.ELIMINATE
            MissPenalty.Nothing -> MissChoice.NONE
            is MissPenalty.ResetScore -> if (penalty.score == 0) MissChoice.RESET_ZERO else MissChoice.RESET_HALF
        }

fun GameConfig.withMissChoice(choice: MissChoice): GameConfig =
    copy(
        onMissLimit =
            when (choice) {
                MissChoice.ELIMINATE -> MissPenalty.Eliminate
                MissChoice.NONE -> MissPenalty.Nothing
                MissChoice.RESET_ZERO -> MissPenalty.ResetScore(0)
                MissChoice.RESET_HALF -> MissPenalty.ResetScore(half)
            },
    )

val GameConfig.overshootChoice: OvershootChoice
    get() =
        when (val overshoot = overshoot) {
            Overshoot.Stay -> OvershootChoice.STAY
            Overshoot.Bounce -> OvershootChoice.BOUNCE
            is Overshoot.ResetTo -> if (overshoot.score == 0) OvershootChoice.RESET_ZERO else OvershootChoice.RESET_HALF
        }

fun GameConfig.withOvershootChoice(choice: OvershootChoice): GameConfig =
    copy(
        overshoot =
            when (choice) {
                OvershootChoice.RESET_HALF -> Overshoot.ResetTo(half)
                OvershootChoice.RESET_ZERO -> Overshoot.ResetTo(0)
                OvershootChoice.STAY -> Overshoot.Stay
                OvershootChoice.BOUNCE -> Overshoot.Bounce
            },
    )

/** Change la cible ; les retours « à la moitié » suivent la nouvelle cible. */
fun GameConfig.withTarget(target: Int): GameConfig {
    val bounded = target.coerceIn(PlayfulRules.MIN_TARGET, PlayfulRules.MAX_TARGET)
    val missChoice = missChoice
    val overshootChoice = overshootChoice
    return copy(targetScore = bounded).withMissChoice(missChoice).withOvershootChoice(overshootChoice)
}

fun GameConfig.withMissLimit(limit: Int): GameConfig =
    copy(missLimit = limit.coerceIn(PlayfulRules.MIN_MISS_LIMIT, PlayfulRules.MAX_MISS_LIMIT))

/**
 * Exceptions actives par rapport aux règles officielles, dans l'ordre de l'éditeur. La faute de pied n'en fait plus
 * partie (2026-10-27) : l'app compte les points, le mordu est seulement expliqué dans les règles.
 */
val GameConfig.exceptions: List<RuleException>
    get() =
        buildList {
            if (missChoice != MissChoice.ELIMINATE) add(RuleException.Miss(missChoice))
            if (missLimit != GameConfig.Classic.missLimit) add(RuleException.MissLimit(missLimit))
            if (overshootChoice != OvershootChoice.RESET_HALF) add(RuleException.Overshoot(overshootChoice))
            if (targetScore != GameConfig.STANDARD_TARGET) add(RuleException.Target(targetScore))
            if (endCondition == EndCondition.PlayForPlaces) add(RuleException.PlayForPlaces)
            if (catchUpReset) add(RuleException.CatchUpReset)
        }
