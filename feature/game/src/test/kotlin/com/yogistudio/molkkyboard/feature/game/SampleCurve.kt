package com.yogistudio.molkkyboard.feature.game

import com.yogistudio.molkkyboard.core.scoring.SeriesMark
import com.yogistudio.molkkyboard.core.scoring.SeriesPoint

/** Courbe d'exemple des captures : le premier gagne au tour 7, le deuxième retombe à 25 au tour 6. */
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
                    points(0, 6, 12, 18, 24, 40, 25, 27, marks = mapOf(6 to SeriesMark.RESET)),
                    winner = false,
                ),
                CurveLineUi(ranked[2], points(0, 2, 2, 5, 8, 10, 12), winner = false),
            ),
        rounds = 7,
        target = 50,
        highlight = 1 to 6,
    )
}
