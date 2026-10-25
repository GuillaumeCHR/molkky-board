package fr.molkky.core.scoring

/** Valeurs des quilles d'une manche. */
val ALL_PINS: IntRange = 1..12

/** Une quille seule = sa valeur ; plusieurs quilles = leur nombre ; aucune = 0. */
fun points(pins: Collection<Int>): Int = if (pins.size == 1) pins.first() else pins.size

internal fun validatePins(pins: List<Int>, allowed: (Int) -> Boolean): ScoringError? =
    when {
        pins.any { !allowed(it) } -> ScoringError.INVALID_PIN
        pins.size != pins.toSet().size -> ScoringError.DUPLICATE_PIN
        else -> null
    }
