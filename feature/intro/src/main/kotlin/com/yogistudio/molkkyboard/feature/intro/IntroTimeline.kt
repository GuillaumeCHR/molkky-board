package com.yogistudio.molkkyboard.feature.intro

import kotlin.math.cos
import kotlin.math.exp

/** Découpage de l'intro « strike » (millisecondes), d'après le storyboard de `CLAUDE.md` (spec du 2026-10-26). */
internal object IntroTimeline {
    /** 1. Mise en place : les 12 quilles apparaissent dans leur formation avec un rebond. */
    const val SETUP_END = 600L

    /** 2. Le lancer : le mölkky arrive du bas en tournoyant, les lignes de vitesse s'accélèrent. */
    const val IMPACT = 1_100L

    /** 3a. Image d'impact figée (≈ 100 ms) : aplat safran, éclat radial, quilles en silhouettes. */
    const val FREEZE_END = 1_200L

    /** 3b. L'envol : quilles intactes projetées hors champ, bulle « 12 ! », le mölkky retombe. */
    const val STRIKE_END = 2_400L

    /** 4. La signature Yogi Studio ; on y reste tant que le chargement n'est pas fini. */
    const val SIGNATURE_END = 3_500L

    /** 5. Le logotype Molkky Board claque au centre, puis le menu apparaît. */
    const val LOGOTYPE_IN = 3_500L
    const val LOGOTYPE_SHOWN = 3_900L
    const val END = 4_500L

    /** Fin des trajectoires précalculées des quilles. */
    const val SIMULATION_END = STRIKE_END

    /** Version courte (réglage, à partir du 2e lancement) : on part juste avant l'impact… */
    const val SHORT_START = 950L

    /** … et, l'envol passé, on saute directement au logotype (≈ 1,7 s au total). */
    const val SHORT_JUMP = 1_700L

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
