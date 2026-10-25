package com.yogistudio.molkkyboard.core.scoring

/** Valeurs des quilles d'une manche. */
val ALL_PINS: IntRange = 1..12

/** Une quille seule = sa valeur ; plusieurs quilles = leur nombre ; aucune = 0. */
fun points(pins: Collection<Int>): Int = if (pins.size == 1) pins.first() else pins.size

/** Points d'un lancer, quel que soit le mode de saisie. */
val ThrowResult.points: Int
    get() =
        when (this) {
            is ThrowResult.Pins -> points(pins)
            is ThrowResult.Points -> value
        }

internal fun validatePins(pins: List<Int>, allowed: (Int) -> Boolean): ScoringError? =
    when {
        pins.any { !allowed(it) } -> ScoringError.INVALID_PIN
        pins.size != pins.toSet().size -> ScoringError.DUPLICATE_PIN
        else -> null
    }

/** Saisie rapide : 0 à 12 ; en « nombre de quilles », 1 est refusé (une quille seule vaut sa valeur). */
internal fun ThrowResult.validate(): ScoringError? =
    when (this) {
        is ThrowResult.Pins -> {
            validatePins(pins) { it in ALL_PINS }
        }

        is ThrowResult.Points -> {
            when {
                value == 0 -> null
                kind == PointsKind.SINGLE_PIN && value in ALL_PINS -> null
                kind == PointsKind.PIN_COUNT && value in 2..ALL_PINS.last -> null
                else -> ScoringError.INVALID_POINTS
            }
        }
    }
