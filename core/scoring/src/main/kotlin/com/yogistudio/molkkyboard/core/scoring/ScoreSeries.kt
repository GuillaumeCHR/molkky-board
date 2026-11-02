package com.yogistudio.molkkyboard.core.scoring

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.model.TeamLineup

/** Ce que raconte un point de la série (`SCORING_SPEC.md` § 18). */
enum class SeriesMark { NONE, RESET, ELIMINATED, LEFT, WIN }

/** Passage d'une équipe, dans l'ordre du journal : sert à la « course vers 50 » du mini-reel. */
data class SeriesStep(
    val team: TeamId,
    val round: Int,
    val score: Int,
    val mark: SeriesMark,
    /** Lancer sans point (raté, faute) : le score n'a pas bougé. */
    val miss: Boolean = false,
)

/** Score de l'équipe à la fin du tour [round] (tour 0, ou tour d'arrivée moins un : score de départ). */
data class SeriesPoint(
    val round: Int,
    val score: Int,
    val mark: SeriesMark = SeriesMark.NONE,
)

data class TeamSeries(
    val team: TeamId,
    val points: List<SeriesPoint>,
)

enum class HighlightKind { COMEBACK, RESET, ELIMINATED }

/** Moment fort de la manche, sur lequel la courbe zoome brièvement. */
data class SeriesHighlight(
    val team: TeamId,
    val round: Int,
    val kind: HighlightKind,
)

/**
 * Série de scores par tour d'une manche, déduite du journal effectif (jamais stockée) : [steps] dans l'ordre du
 * journal, [teams] avec un point par tour joué, [rounds] tours, axe de 0 à [target] (la plus haute cible, mode famille
 * compris), [winner] et [highlight] (remontée, retour en arrière ou élimination la plus marquante).
 */
data class ScoreSeries(
    val steps: List<SeriesStep>,
    val teams: List<TeamSeries>,
    val rounds: Int,
    val target: Int,
    val winner: TeamId?,
    val highlight: SeriesHighlight?,
)

/**
 * Rejoue [effectiveLog] et relève, après chaque passage (lancer, faute, lancer de la mauvaise équipe), le score de
 * l'équipe concernée. Une équipe ajoutée en cours de manche part de son score de départ à la fin du tour précédant
 * son arrivée ; une équipe qui part s'arrête au tour de son départ. Une victoire acquise par l'élimination des autres
 * est marquée sur le dernier point du vainqueur. `null` si la manche ne démarre pas.
 */
fun scoreSeries(config: GameConfig, lineup: List<TeamLineup>, effectiveLog: List<PlayEvent>): ScoreSeries? {
    val initial = start(config, lineup).getOrNull() ?: return null
    val starts = linkedMapOf<TeamId, SeriesPoint>()
    initial.teams.forEach { starts[it.id] = SeriesPoint(0, it.score) }
    val steps = mutableListOf<SeriesStep>()
    var state = initial
    for (event in effectiveLog) {
        val before = state
        val round = before.round
        state = reduce(before, event).getOrNull() ?: continue
        state.teams.forEach { team ->
            starts.getOrPut(team.id) { SeriesPoint((round - 1).coerceAtLeast(0), team.score) }
        }
        val team =
            when (event) {
                is WrongTeam -> event.team
                is Throw, is Fault -> before.turn
                else -> null
            }
        if (team != null) {
            steps += step(before, state, team, round)
        }
        // Départ d'une équipe : sa ligne s'arrête là.
        state.teams.filter { it.left && before.team(it.id)?.left == false }.forEach {
            steps += SeriesStep(it.id, round, it.score, SeriesMark.LEFT)
        }
        val winner = state.winner
        if (winner != null && winner != before.winner && winner != team) {
            val last = steps.indexOfLast { it.team == winner }
            if (last >= 0) {
                steps[last] = steps[last].copy(mark = SeriesMark.WIN)
            } else {
                steps += SeriesStep(winner, round, state.team(winner)?.score ?: 0, SeriesMark.WIN)
            }
        }
    }
    val teams = starts.map { (id, first) -> TeamSeries(id, listOf(first) + perRound(steps.filter { it.team == id })) }
    val target = (listOf(config.targetScore) + config.teamOverrides.values.mapNotNull { it.targetScore }).max()
    return ScoreSeries(
        steps = steps,
        teams = teams,
        rounds = teams.maxOf { series -> series.points.maxOf { it.round } },
        target = target,
        winner = state.winner,
        highlight = highlight(teams, state.winner),
    )
}

private fun step(before: SetState, after: SetState, team: TeamId, round: Int): SeriesStep {
    val previous = before.team(team)
    val now = after.team(team)
    val score = now?.score ?: previous?.score ?: 0
    val mark =
        when {
            after.winner == team && before.winner != team -> SeriesMark.WIN
            now?.eliminated == true && previous?.eliminated == false -> SeriesMark.ELIMINATED
            score < (previous?.score ?: 0) -> SeriesMark.RESET
            else -> SeriesMark.NONE
        }
    return SeriesStep(team, round, score, mark, miss = score == (previous?.score ?: 0))
}

/** Un point par tour : le score à la fin du tour, avec la marque la plus forte du tour. */
private fun perRound(steps: List<SeriesStep>): List<SeriesPoint> =
    steps.groupBy { it.round }.map { (round, inRound) ->
        SeriesPoint(round, inRound.last().score, inRound.maxOf { it.mark })
    }

/**
 * Moment fort : une remontée (le vainqueur a été mené d'au moins [COMEBACK_GAP] points), sinon la plus forte chute
 * (retour en arrière), sinon la première élimination. Le point gagnant lui-même n'en est jamais un.
 */
private fun highlight(teams: List<TeamSeries>, winner: TeamId?): SeriesHighlight? {
    comeback(teams, winner)?.let { return it }
    val drops =
        teams.flatMap { series ->
            series.points.zipWithNext().filter { (_, b) -> b.mark == SeriesMark.RESET }.map { (a, b) ->
                Triple(series.team, b.round, a.score - b.score)
            }
        }
    drops.maxByOrNull { it.third }?.let { (team, round, _) -> return SeriesHighlight(team, round, HighlightKind.RESET) }
    return teams
        .flatMap { series -> series.points.filter { it.mark == SeriesMark.ELIMINATED }.map { series.team to it.round } }
        .minByOrNull { it.second }
        ?.let { (team, round) -> SeriesHighlight(team, round, HighlightKind.ELIMINATED) }
}

/** Tour où le vainqueur était le plus mené, si l'écart a atteint [COMEBACK_GAP]. */
private fun comeback(teams: List<TeamSeries>, winner: TeamId?): SeriesHighlight? {
    val mine = teams.firstOrNull { it.team == winner } ?: return null
    val others = teams.filter { it.team != winner }
    var best: Pair<Int, Int>? = null
    for (point in mine.points) {
        val leader = others.maxOfOrNull { other -> other.scoreAt(point.round) } ?: continue
        val gap = leader - point.score
        if (gap >= COMEBACK_GAP && gap > (best?.second ?: 0)) best = point.round to gap
    }
    return best?.let { (round, _) -> SeriesHighlight(mine.team, round, HighlightKind.COMEBACK) }
}

/** Score de l'équipe à la fin du tour [round] (dernier point connu à ce moment). */
fun TeamSeries.scoreAt(round: Int): Int = points.lastOrNull { it.round <= round }?.score ?: points.first().score

/** Retard minimal d'une « remontée » (même seuil que les moments partageables, `STATS_SPEC.md` § 8). */
const val COMEBACK_GAP = 15
