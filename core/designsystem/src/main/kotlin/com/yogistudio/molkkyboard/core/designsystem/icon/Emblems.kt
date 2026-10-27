package com.yogistudio.molkkyboard.core.designsystem.icon

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
