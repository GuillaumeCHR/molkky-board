package com.yogistudio.molkkyboard.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Formes très arrondies : 24–32 dp sur les cartes, pilules sur les boutons. */
internal val MBShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

/** Dimensions communes. */
object MBDimens {
    /** Zone tactile minimale : utilisable à une main, en extérieur. */
    val MinTouch = 56.dp
    val ActionButtonHeight = 72.dp
    val ScreenPadding = 20.dp
    val Gap = 12.dp
    val SmallGap = 8.dp
}
