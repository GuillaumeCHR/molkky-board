package com.yogistudio.molkkyboard.feature.setup

/** Premier élément de [choices] qui n'est pas dans [used], sinon le n-ième en boucle. */
fun <T> firstFree(choices: List<T>, used: Collection<T>, fallbackIndex: Int): T =
    choices.firstOrNull { it !in used } ?: choices[fallbackIndex % choices.size]

/** Déplace l'élément [from] à la position [to] (glisser-déposer). */
internal fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
