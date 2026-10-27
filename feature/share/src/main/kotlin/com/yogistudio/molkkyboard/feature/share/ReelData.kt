package com.yogistudio.molkkyboard.feature.share

import androidx.compose.ui.graphics.toArgb
import com.yogistudio.molkkyboard.core.data.GameSnapshot
import com.yogistudio.molkkyboard.core.designsystem.theme.TeamPalette
import com.yogistudio.molkkyboard.core.model.TeamId
import com.yogistudio.molkkyboard.core.scoring.Fault
import com.yogistudio.molkkyboard.core.scoring.SetState
import com.yogistudio.molkkyboard.core.scoring.Throw
import com.yogistudio.molkkyboard.core.scoring.WrongTeam
import com.yogistudio.molkkyboard.core.scoring.describe
import com.yogistudio.molkkyboard.core.scoring.getOrNull
import com.yogistudio.molkkyboard.core.scoring.getOrThrow
import com.yogistudio.molkkyboard.core.scoring.reduce
import com.yogistudio.molkkyboard.core.scoring.start

/** Une équipe dans le clip : nom affiché (masqué si demandé), couleur ARGB, emoji. */
data class ReelTeam(
    val name: String,
    val color: Int,
    val avatar: String?,
)

enum class StepKind { POINTS, MISS, RESET, ELIMINATED, WIN }

/** Un lancer de la course vers 50 : l'équipe [team] (index dans [ReelData.teams]) passe à [score]. */
data class ReelStep(
    val team: Int,
    val score: Int,
    val round: Int,
    val kind: StepKind,
)

/** Tout ce que le clip met en scène, calculé une fois depuis le journal (le rendu ne fait que lire). */
data class ReelData(
    val teams: List<ReelTeam>,
    val steps: List<ReelStep>,
    val winner: Int?,
    val target: Int,
    val title: String,
    val keyFigure: String,
    val labels: ReelLabels,
)

/** Textes du clip, traduits par l'appelant (chaînes externalisées). */
data class ReelLabels(
    val round: (Int) -> String,
    val reset: String,
    val eliminated: String,
    val scan: String,
    val hashtag: String,
)

/** Chronologie de la manche en cours, avec les noms et emoji de la vignette ([content], masquage compris). */
fun GameSnapshot.reelData(content: ShareContent, title: String, keyFigure: String, labels: ReelLabels): ReelData {
    val palette = TeamPalette.colors(dark = false)
    val index = content.teams.withIndex().associate { (i, team) -> team.id to i }
    val teams = content.teams.map { ReelTeam(it.name, palette[it.colorIndex % palette.size].fill.toArgb(), it.avatar) }
    val initial = start(config, currentSet.lineup).getOrNull()
    val steps = mutableListOf<ReelStep>()
    if (initial != null) {
        var state: SetState = initial
        describe(initial, currentSet.replay.effectiveLog).forEach { entry ->
            val before = state
            state = reduce(state, entry.event).getOrThrow()
            val team = entry.team?.takeIf { entry.event is Throw || entry.event is Fault || entry.event is WrongTeam }
            val i = team?.let(index::get) ?: return@forEach
            val after = state.team(team) ?: return@forEach
            val kind = stepKind(before, state, team, after.score)
            steps += ReelStep(i, after.score, entry.round, kind)
        }
    }
    return ReelData(
        teams = teams,
        steps = steps,
        winner = content.teams.indexOfFirst { it.id == currentSet.replay.state.winner }.takeIf { it >= 0 },
        target = content.target,
        title = title,
        keyFigure = keyFigure,
        labels = labels,
    )
}

/** Nature d'un lancer pour la mise en scène : victoire, élimination, retour en arrière, raté ou points. */
private fun stepKind(before: SetState, after: SetState, team: TeamId, score: Int): StepKind {
    val previous = before.team(team)
    return when {
        after.winner == team -> StepKind.WIN
        after.team(team)?.eliminated == true && previous?.eliminated == false -> StepKind.ELIMINATED
        score < (previous?.score ?: 0) -> StepKind.RESET
        score == (previous?.score ?: 0) -> StepKind.MISS
        else -> StepKind.POINTS
    }
}
