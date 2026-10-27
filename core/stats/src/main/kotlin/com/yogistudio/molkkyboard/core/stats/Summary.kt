package com.yogistudio.molkkyboard.core.stats

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup
import com.yogistudio.molkkyboard.core.model.isStandard
import com.yogistudio.molkkyboard.core.scoring.Fault
import com.yogistudio.molkkyboard.core.scoring.LogEntry
import com.yogistudio.molkkyboard.core.scoring.PlayEvent
import com.yogistudio.molkkyboard.core.scoring.ReplayResult
import com.yogistudio.molkkyboard.core.scoring.SetState
import com.yogistudio.molkkyboard.core.scoring.SetStatus
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import com.yogistudio.molkkyboard.core.scoring.WrongTeam
import com.yogistudio.molkkyboard.core.scoring.describe
import com.yogistudio.molkkyboard.core.scoring.getOrNull
import com.yogistudio.molkkyboard.core.scoring.points
import com.yogistudio.molkkyboard.core.scoring.start

/** Une équipe d'une partie et son identité durable ([memberKey]) : c'est elle qui accumule le palmarès. */
data class StatsTeam(
    val id: TeamId,
    val memberKey: String,
    val name: String,
)

/** Une manche telle que `core:stats` la reçoit : composition de départ et résultat du rejeu. */
data class StatsSet(
    val index: Int,
    val lineup: List<TeamLineup>,
    val replay: ReplayResult,
)

/** Une partie : rien n'y est précalculé, tout est déduit des journaux de ses manches. */
data class StatsGame(
    val id: String,
    /** Date de la partie (millisecondes depuis l'époque Unix). */
    val playedAt: Long,
    val config: GameConfig,
    val teams: List<StatsTeam>,
    val sets: List<StatsSet>,
    val abandoned: Boolean = false,
)

/** Lancers d'un joueur dans une manche. Les fautes ne sont pas des lancers ; les ratés en sont. */
data class PlayerLine(
    val throws: Int = 0,
    val misses: Int = 0,
    val points: Int = 0,
    /** Quilles touchées seules (mode « Quilles » uniquement) : alimentera la carte de précision. */
    val singlePins: Map<Int, Int> = emptyMap(),
    /** Plus longue série de lancers sans raté. */
    val bestStreak: Int = 0,
)

/** Une équipe dans le résumé d'une manche. */
data class SummaryTeam(
    val memberKey: String,
    val name: String,
    val players: List<PlayerId>,
    val score: Int,
    /** Lancers de l'équipe dans la manche, fautes comprises. */
    val throws: Int,
)

/** Résumé d'une manche, base de tous les classements et statistiques. */
data class SetSummary(
    val gameId: String,
    val setIndex: Int,
    val playedAt: Long,
    val teams: List<SummaryTeam>,
    /** [SummaryTeam.memberKey] du vainqueur ; `null` si la manche n'est pas terminée. */
    val winner: String?,
    /** Terminée normalement, ni abandonnée ni modifiée après la fin : compte pour les victoires. */
    val eligible: Boolean,
    /** Règles officielles (cible 50, retour à 25, sans handicap) : compte aussi pour les records « purs ». */
    val standard: Boolean,
    val players: Map<PlayerId, PlayerLine>,
) {
    /** Partie en solo : chaque équipe n'a qu'un joueur (onglet Joueurs du Hall of Fame). */
    val solo: Boolean get() = teams.all { it.players.size == 1 }

    val winnerTeam: SummaryTeam? get() = teams.firstOrNull { it.memberKey == winner }

    fun team(memberKey: String): SummaryTeam? = teams.firstOrNull { it.memberKey == memberKey }
}

/** Résume chaque manche de [game] à partir de son journal effectif. */
fun summarize(game: StatsGame): List<SetSummary> = game.sets.mapNotNull { summarize(game, it) }

/** Ajoute un lancer ; [streak] est la série sans raté en cours après ce lancer. */
private fun PlayerLine.plus(result: ThrowResult, streak: Int): PlayerLine {
    val points = result.points
    val single = (result as? ThrowResult.Pins)?.pins?.singleOrNull()
    return copy(
        throws = throws + 1,
        misses = misses + if (points == 0) 1 else 0,
        points = this.points + points,
        singlePins = if (single == null) singlePins else singlePins + (single to (singlePins[single] ?: 0) + 1),
        bestStreak = maxOf(bestStreak, streak),
    )
}

/** Lancers par équipe (fautes comprises) et lignes de chaque joueur, en rejouant le journal effectif. */
private class Tally(
    initial: SetState,
    log: List<PlayEvent>,
) {
    val throwsByTeam = mutableMapOf<TeamId, Int>()
    val lines = mutableMapOf<PlayerId, PlayerLine>()
    private val streaks = mutableMapOf<PlayerId, Int>()

    init {
        describe(initial, log).forEach(::add)
    }

    private fun add(entry: LogEntry) {
        val event = entry.event
        val team = entry.team ?: return
        if (event is Throw || event is Fault || event is WrongTeam) throwsByTeam.merge(team, 1, Int::plus)
        val player = entry.player ?: return
        if (event !is Throw) return
        val streak = if (event.result.points == 0) 0 else (streaks[player] ?: 0) + 1
        streaks[player] = streak
        lines[player] = (lines[player] ?: PlayerLine()).plus(event.result, streak)
    }
}

private fun summarize(game: StatsGame, set: StatsSet): SetSummary? {
    val initial = start(game.config, set.lineup).getOrNull() ?: return null
    val state = set.replay.state
    val keys = game.teams.associate { it.id to it }
    val tally = Tally(initial, set.replay.effectiveLog)
    val finished = state.status == SetStatus.FINISHED
    return SetSummary(
        gameId = game.id,
        setIndex = set.index,
        playedAt = game.playedAt,
        teams =
            state.teams.map { team ->
                val info = keys[team.id]
                SummaryTeam(
                    memberKey = info?.memberKey ?: team.id.value,
                    name = info?.name ?: team.id.value,
                    players = team.players,
                    score = team.score,
                    throws = tally.throwsByTeam[team.id] ?: 0,
                )
            },
        winner = state.winner?.takeIf { finished }?.let { keys[it]?.memberKey ?: it.value },
        eligible = set.replay.eligibleForRecords && !game.abandoned,
        standard = game.config.isStandard,
        players = tally.lines,
    )
}
