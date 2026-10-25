package com.yogistudio.molkkyboard.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalMBColors = staticCompositionLocalOf { LightMBColors }
private val LocalMBTypography = staticCompositionLocalOf { MBTypography() }
private val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Thème « Nordic Bold ». Pas de couleurs dynamiques : l'identité visuelle est imposée.
 *
 * @param sunlight option « Plein soleil » : contraste renforcé (prioritaire sur le mode sombre).
 * @param reducedMotion réglage « Réduire les animations » ; par défaut, celui du système.
 */
@Composable
fun MolkkyBoardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    sunlight: Boolean = false,
    reducedMotion: Boolean = rememberSystemReducedMotion(),
    content: @Composable () -> Unit,
) {
    val (scheme, colors) =
        when {
            sunlight -> SunlightColorScheme to SunlightMBColors
            darkTheme -> DarkColorScheme to DarkMBColors
            else -> LightColorScheme to LightMBColors
        }
    CompositionLocalProvider(
        LocalMBColors provides colors,
        LocalMBTypography provides MBTypography(),
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MBMaterialTypography,
            shapes = MBShapes,
            content = content,
        )
    }
}

/** Accès aux tokens propres à l'application, à côté de `MaterialTheme`. */
object MolkkyBoardTheme {
    val colors: MBColors
        @Composable @ReadOnlyComposable
        get() = LocalMBColors.current

    val typography: MBTypography
        @Composable @ReadOnlyComposable
        get() = LocalMBTypography.current

    /** Réglage « Réduire les animations » : les mouvements sont remplacés par des fondus, ou supprimés. */
    val reducedMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReducedMotion.current
}
