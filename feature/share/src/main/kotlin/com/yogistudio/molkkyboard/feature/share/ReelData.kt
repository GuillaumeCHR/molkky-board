package com.yogistudio.molkkyboard.feature.share

import androidx.compose.ui.graphics.toArgb
import com.yogistudio.molkkyboard.core.data.GameSnapshot
import com.yogistudio.molkkyboard.core.designsystem.theme.TeamPalette
import com.yogistudio.molkkyboard.core.scoring.SeriesMark
import com.yogistudio.molkkyboard.core.scoring.SeriesStep
import com.yogistudio.molkkyboard.core.scoring.scoreSeries

/** Une équipe dans le clip : nom et couleur ARGB (le clip, dessiné en `Canvas`, n'a pas d'emblème). */
data class ReelTeam(
    val name: String,
    val color: Int,
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
    val teams = content.teams.map { ReelTeam(it.name, palette[it.colorIndex % palette.size].fill.toArgb()) }
    // Même série que la courbe de l'écran de fin (`SCORING_SPEC.md` § 18), passage par passage.
    val steps =
        scoreSeries(config, currentSet.lineup, currentSet.replay.effectiveLog)
            ?.steps
            .orEmpty()
            .filter { it.mark != SeriesMark.LEFT }
            .mapNotNull { step -> index[step.team]?.let { ReelStep(it, step.score, step.round, step.kind()) } }
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

/** Nature d'un passage pour la mise en scène : victoire, élimination, retour en arrière, raté ou points. */
private fun SeriesStep.kind(): StepKind =
    when {
        mark == SeriesMark.WIN -> StepKind.WIN
        mark == SeriesMark.ELIMINATED -> StepKind.ELIMINATED
        mark == SeriesMark.RESET -> StepKind.RESET
        miss -> StepKind.MISS
        else -> StepKind.POINTS
    }
