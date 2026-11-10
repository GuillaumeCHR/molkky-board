package com.yogistudio.molkkyboard.feature.game

import com.yogistudio.molkkyboard.core.scoring.SeriesMark
import com.yogistudio.molkkyboard.core.scoring.SeriesPoint

/**
 * Courbe d'exemple des captures : le premier gagne au tour 7, le deuxième retombe à 25 au tour 6 (39 + 12 = 51).
 * Les écarts entre deux tours restent ceux d'un vrai lancer (0 à 12) et collent aux statistiques des captures.
 */
internal fun sampleCurve(ranked: List<TeamUi>): CurveUi {
    fun points(vararg scores: Int, marks: Map<Int, SeriesMark> = emptyMap()) =
        scores.mapIndexed { round, score -> SeriesPoint(round, score, marks[round] ?: SeriesMark.NONE) }
    return CurveUi(
        lines =
            listOf(
                CurveLineUi(
                    ranked[0],
                    points(0, 8, 15, 24, 31, 38, 45, 50, marks = mapOf(7 to SeriesMark.WIN)),
                    winner = true,
                ),
                CurveLineUi(
                    ranked[1],
                    points(0, 8, 17, 17, 28, 39, 25, 27, marks = mapOf(6 to SeriesMark.RESET)),
                    winner = false,
                ),
                CurveLineUi(ranked[2], points(0, 3, 3, 7, 7, 10, 12), winner = false),
            ),
        rounds = 7,
        target = 50,
        highlight = 1 to 6,
    )
}
