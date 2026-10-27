package com.yogistudio.molkkyboard.core.stats

import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.scoring.LogEntry
import com.yogistudio.molkkyboard.core.scoring.PointsKind
import com.yogistudio.molkkyboard.core.scoring.SetState
import com.yogistudio.molkkyboard.core.scoring.SetStatus
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import com.yogistudio.molkkyboard.core.scoring.describe
import com.yogistudio.molkkyboard.core.scoring.getOrNull
import com.yogistudio.molkkyboard.core.scoring.getOrThrow
import com.yogistudio.molkkyboard.core.scoring.points
import com.yogistudio.molkkyboard.core.scoring.reduce
import com.yogistudio.molkkyboard.core.scoring.start

/** Un moment partageable d'une manche, détecté à partir de son journal. */
sealed interface Moment {
    /** Force du moment : le plus fort est proposé en premier sur l'écran de fin. */
    val strength: Int

    data class Victory(
        val team: TeamId,
        val throws: Int,
        val round: Int,
    ) : Moment {
        override val strength: Int get() = VICTORY
    }

    /** Victoire en 5 lancers (4 × 12 + 2), aux règles officielles. */
    data class Perfect(
        val team: TeamId,
    ) : Moment {
        override val strength: Int get() = PERFECT
    }

    /** Record battu (victoire la plus rapide de son onglet). */
    data class Record(
        val team: TeamId,
        val throws: Int,
    ) : Moment {
        override val strength: Int get() = RECORD
    }

    /** Victoire après avoir été mené de [deficit] points ou plus. */
    data class Comeback(
        val team: TeamId,
        val deficit: Int,
    ) : Moment {
        override val strength: Int get() = COMEBACK
    }

    /** Un lancer à 12 : la quille 12 seule, ou les 12 quilles d'un coup ([strike]). */
    data class Twelve(
        val team: TeamId,
        val player: PlayerId?,
        val strike: Boolean,
    ) : Moment {
        override val strength: Int get() = if (strike) STRIKE else TWELVE
    }

    /** Moment malheureux : retour à 25 alors que l'équipe était à 49 (à un point de la cible). */
    data class ResetFrom49(
        val team: TeamId,
        val player: PlayerId?,
    ) : Moment {
        override val strength: Int get() = RESET_49
    }

    /** Moment malheureux : élimination après la série de ratés. */
    data class Eliminated(
        val team: TeamId,
    ) : Moment {
        override val strength: Int get() = ELIMINATED
    }

    companion object {
        const val RECORD = 100
        const val PERFECT = 90
        const val COMEBACK = 70
        const val STRIKE = 60
        const val VICTORY = 50
        const val TWELVE = 40
        const val RESET_49 = 30
        const val ELIMINATED = 20

        /** Écart minimal pour parler de remontée. */
        const val COMEBACK_DEFICIT = 15
    }
}

/**
 * Moments partageables de la manche [setIndex] de [game], du plus fort au plus faible. [newRecord] vient de
 * [isNewRecord] (il dépend de l'historique). Une manche non terminée n'a ni victoire ni remontée.
 */
fun moments(game: StatsGame, setIndex: Int, newRecord: Boolean = false): List<Moment> {
    val set = game.sets.firstOrNull { it.index == setIndex } ?: return emptyList()
    val initial = start(game.config, set.lineup).getOrNull() ?: return emptyList()
    val scan = MomentScanner(game, initial).apply { describe(initial, set.replay.effectiveLog).forEach(::add) }
    val final = set.replay.state
    val winner = final.winner?.takeIf { final.status == SetStatus.FINISHED } ?: return scan.sorted()
    val summary = summarize(game).firstOrNull { it.setIndex == setIndex }
    val throws = summary?.winnerTeam?.throws ?: 0
    scan.found += Moment.Victory(winner, throws, final.round)
    if (summary?.standard == true && throws in 1..PERFECT_GAME_THROWS) scan.found += Moment.Perfect(winner)
    if (newRecord) scan.found += Moment.Record(winner, throws)
    val deficit = scan.worstDeficit[winner] ?: 0
    if (deficit >= Moment.COMEBACK_DEFICIT) scan.found += Moment.Comeback(winner, deficit)
    return scan.sorted()
}

/** Parcourt le journal effectif, événement après événement, et relève les moments en cours de manche. */
private class MomentScanner(
    private val game: StatsGame,
    initial: SetState,
) {
    val found = mutableListOf<Moment>()

    /** Plus grand retard de chaque équipe sur la meilleure (pour la remontée). */
    val worstDeficit = mutableMapOf<TeamId, Int>()
    private var state = initial

    fun add(entry: LogEntry) {
        val before = state
        state = reduce(state, entry.event).getOrThrow()
        val event = entry.event
        val team = entry.team
        if (event is Throw && team != null) throwMoments(event, team, entry, before.team(team)?.score ?: 0)
        state.teams
            .filter { it.eliminated && before.team(it.id)?.eliminated == false }
            .forEach { found += Moment.Eliminated(it.id) }
        val best = state.teams.maxOf { it.score }
        state.teams.forEach { t -> worstDeficit[t.id] = maxOf(worstDeficit[t.id] ?: 0, best - t.score) }
    }

    private fun throwMoments(event: Throw, team: TeamId, entry: LogEntry, beforeScore: Int) {
        val points = event.result.points
        val target = targetOf(game, team)
        // Un 12 qui fait dépasser la cible ne se célèbre pas.
        if (points == TWELVE_POINTS && beforeScore + points <= target) {
            found += Moment.Twelve(team, entry.player, strike = event.result.isStrike())
        }
        // À un point de la cible, un lancer de plus d'un point fait dépasser : retour en arrière.
        if (beforeScore == target - 1 && points > 1 && (entry.scoreAfter ?: 0) < beforeScore) {
            found += Moment.ResetFrom49(team, entry.player)
        }
    }

    fun sorted(): List<Moment> = found.sortedByDescending { it.strength }
}

private fun ThrowResult.isStrike(): Boolean =
    when (this) {
        is ThrowResult.Pins -> pins.size == TWELVE_POINTS
        is ThrowResult.Points -> kind == PointsKind.PIN_COUNT
    }

private fun targetOf(game: StatsGame, team: TeamId): Int =
    game.config.teamOverrides[team]?.targetScore ?: game.config.targetScore

private const val TWELVE_POINTS = 12
