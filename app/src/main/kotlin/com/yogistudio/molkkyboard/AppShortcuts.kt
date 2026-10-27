package com.yogistudio.molkkyboard

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Raccourcis de l'icône (appui long) : nouvelle partie, placement des quilles, règles. Publiés depuis l'activité, dans
 * la langue de l'app, avec une intention explicite vers [MainActivity] (aucun lien profond, aucun composant exporté
 * de plus). « Rejoindre une partie » viendra avec les parties partagées.
 */
object AppShortcuts {
    const val EXTRA_SHORTCUT = "com.yogistudio.molkkyboard.SHORTCUT"
    const val NEW_GAME = "new_game"
    const val PLACEMENT = "placement"
    const val RULES = "rules"

    private class Shortcut(
        val id: String,
        @param:StringRes val label: Int,
        @param:DrawableRes val icon: Int,
    )

    private val ALL =
        listOf(
            Shortcut(NEW_GAME, R.string.shortcut_new_game, R.drawable.ic_shortcut_new_game),
            Shortcut(PLACEMENT, R.string.shortcut_placement, R.drawable.ic_shortcut_pins),
            Shortcut(RULES, R.string.shortcut_rules, R.drawable.ic_shortcut_rules),
        )

    fun publish(context: Context) {
        val shortcuts =
            ALL.map { shortcut ->
                ShortcutInfoCompat
                    .Builder(context, shortcut.id)
                    .setShortLabel(context.getString(shortcut.label))
                    .setIcon(IconCompat.createWithResource(context, shortcut.icon))
                    .setIntent(
                        Intent(context, MainActivity::class.java)
                            .setAction(Intent.ACTION_VIEW)
                            .putExtra(EXTRA_SHORTCUT, shortcut.id),
                    ).build()
            }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }
}
