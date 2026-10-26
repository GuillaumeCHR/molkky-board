package com.yogistudio.molkkyboard.feature.setup

/** Avatars par défaut des joueurs : aucune photo n'est nécessaire. */
val PLAYER_AVATARS =
    listOf("🦊", "🐻", "🐼", "🐸", "🦉", "🐙", "🦄", "🐢", "🦁", "🐧", "🐝", "🦔", "🐨", "🐯", "🐳", "🦜")

/**
 * Emblèmes d'équipe : une petite banque d'icônes simples, qui doublent toujours la couleur (daltonisme).
 * La clé est enregistrée ; le symbole s'affiche.
 */
val TEAM_EMBLEMS =
    linkedMapOf(
        "log" to "🪵",
        "leaf" to "🍃",
        "bolt" to "⚡",
        "wave" to "🌊",
        "star" to "⭐",
        "sun" to "☀️",
        "mountain" to "⛰️",
        "fire" to "🔥",
        "tree" to "🌲",
        "crown" to "👑",
        "anchor" to "⚓",
        "heart" to "❤️",
    )

fun emblemSymbol(key: String?): String = TEAM_EMBLEMS[key] ?: TEAM_EMBLEMS.values.first()

/** Premier élément de [choices] qui n'est pas dans [used], sinon le n-ième en boucle. */
fun <T> firstFree(choices: List<T>, used: Collection<T>, fallbackIndex: Int): T =
    choices.firstOrNull { it !in used } ?: choices[fallbackIndex % choices.size]

/** Déplace l'élément [from] à la position [to] (glisser-déposer). */
internal fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
