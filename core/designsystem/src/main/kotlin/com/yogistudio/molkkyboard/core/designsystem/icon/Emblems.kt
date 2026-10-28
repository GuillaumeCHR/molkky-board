package com.yogistudio.molkkyboard.core.designsystem.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.yogistudio.molkkyboard.core.designsystem.R

/**
 * Avatars par défaut des joueurs : les 16 emblèmes au trait (C6, 2026-10-28), aucune photo nécessaire.
 * La clé est enregistrée dans le profil ; l'icône s'affiche.
 */
val PLAYER_AVATARS: List<String> = LineEmblems.all.keys.toList()

/**
 * Emblèmes d'équipe : une petite banque d'icônes simples, qui doublent toujours la couleur (daltonisme).
 * Les clés sont celles déjà enregistrées dans les équipes connues ; leur dessin vient de [LineEmblems].
 */
val TEAM_EMBLEMS: List<String> =
    listOf("log", "leaf", "bolt", "wave", "star", "sun", "mountain", "fire", "tree", "crown", "anchor", "heart")

/**
 * Icône d'un avatar ou d'un emblème enregistré : une clé de [LineEmblems], une ancienne clé d'emblème d'équipe, ou
 * un emoji enregistré avant le passage aux emblèmes au trait (profils et sauvegardes existants, sans migration).
 * `null` si rien ne correspond : on affiche alors l'initiale.
 */
fun emblemIcon(key: String?): ImageVector? {
    if (key == null) return null
    val resolved = LEGACY[key] ?: key
    return LineEmblems.all[resolved]
}

/** Nom d'un emblème, lu par TalkBack dans les sélecteurs (« Quille », « Feuille »…) ; vide si la clé est inconnue. */
@Composable
fun emblemLabel(key: String?): String = LABELS[LEGACY[key] ?: key]?.let { stringResource(it) }.orEmpty()

private val LABELS: Map<String, Int> =
    mapOf(
        "pin" to R.string.ds_emblem_pin,
        "leaf" to R.string.ds_emblem_leaf,
        "bolt" to R.string.ds_emblem_bolt,
        "wave" to R.string.ds_emblem_wave,
        "star" to R.string.ds_emblem_star,
        "sun" to R.string.ds_emblem_sun,
        "mountain" to R.string.ds_emblem_mountain,
        "flame" to R.string.ds_emblem_flame,
        "tree" to R.string.ds_emblem_tree,
        "crown" to R.string.ds_emblem_crown,
        "anchor" to R.string.ds_emblem_anchor,
        "heart" to R.string.ds_emblem_heart,
        "moon" to R.string.ds_emblem_moon,
        "snowflake" to R.string.ds_emblem_snowflake,
        "fish" to R.string.ds_emblem_fish,
        "bird" to R.string.ds_emblem_bird,
    )

private val LEGACY =
    mapOf(
        // Anciennes clés d'emblème d'équipe.
        "log" to "pin",
        "fire" to "flame",
        // Anciens emoji des joueurs, dans l'ordre de l'ancienne banque.
        "🦊" to "pin",
        "🐻" to "leaf",
        "🐼" to "bolt",
        "🐸" to "wave",
        "🦉" to "star",
        "🐙" to "sun",
        "🦄" to "mountain",
        "🐢" to "flame",
        "🦁" to "tree",
        "🐧" to "crown",
        "🐝" to "anchor",
        "🦔" to "heart",
        "🐨" to "moon",
        "🐯" to "snowflake",
        "🐳" to "fish",
        "🦜" to "bird",
        // Anciens emoji d'emblème d'équipe (affichés en jeu avant le 2026-10-28).
        "🪵" to "pin",
        "🍃" to "leaf",
        "⚡" to "bolt",
        "🌊" to "wave",
        "⭐" to "star",
        "☀️" to "sun",
        "⛰️" to "mountain",
        "🔥" to "flame",
        "🌲" to "tree",
        "👑" to "crown",
        "⚓" to "anchor",
        "❤️" to "heart",
    )
