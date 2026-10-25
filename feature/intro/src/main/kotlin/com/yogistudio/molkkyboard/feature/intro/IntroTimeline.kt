package com.yogistudio.molkkyboard.feature.intro

import kotlin.math.cos
import kotlin.math.exp

/** Découpage de l'intro (millisecondes), d'après le storyboard de `CLAUDE.md`. */
internal object IntroTimeline {
    /** 1. Mise en place : les quilles apparaissent avec un rebond. */
    const val SETUP_END = 600L

    /** 2. Le lancer : le mölkky arrive du bas en tournoyant. */
    const val IMPACT = 1_200L

    /** 3. L'impact : les quilles volent en éclats et sortent de l'écran. */
    const val SHATTER_END = 2_300L

    /** 4. La signature Yogi Studio ; on y reste tant que le chargement n'est pas fini. */
    const val SIGNATURE_END = 3_600L

    /** 5. Fondu du logotype Molkky Board, puis le menu apparaît. */
    const val LOGOTYPE_IN = 3_700L
    const val LOGOTYPE_SHOWN = 4_200L
    const val END = 4_500L

    /** Fin des trajectoires précalculées des éclats. */
    const val SIMULATION_END = 2_600L

    /** Version « Réduire les animations » : simple fondu du logo Yogi Studio. */
    const val REDUCED_END = 1_000L
}

internal fun progress(time: Long, from: Long, to: Long): Float =
    ((time - from).toFloat() / (to - from)).coerceIn(
        0f,
        1f,
    )

/** Courbe de ressort amorti (0 → 1 avec léger dépassement), calculée sans allocation. */
internal fun springCurve(p: Float): Float = if (p <= 0f) 0f else 1f - exp(-6f * p) * cos(10f * p)

internal fun easeOutCubic(p: Float): Float = 1f - (1f - p) * (1f - p) * (1f - p)

internal fun easeInOutCubic(p: Float): Float =
    if (p <
        0.5f
    ) {
        4f * p * p * p
    } else {
        1f - (-2f * p + 2f).let { it * it * it } / 2f
    }
