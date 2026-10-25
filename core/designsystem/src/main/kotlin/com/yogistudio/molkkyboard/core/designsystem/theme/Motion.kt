package com.yogistudio.molkkyboard.core.designsystem.theme

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Ressorts partagés : aucune animation linéaire dans l'application. */
object MBMotion {
    /** Retour généreux : quilles, boutons, compteurs. */
    fun <T> bouncy(): SpringSpec<T> =
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )

    /** Mouvement posé : jauges, transitions. */
    fun <T> gentle(): SpringSpec<T> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        )

    /** Réaction immédiate : appui. */
    fun <T> snappy(): SpringSpec<T> =
        spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        )
}

/** Réglage système « Supprimer les animations » (échelle de durée des animations à 0). */
@Composable
internal fun rememberSystemReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
