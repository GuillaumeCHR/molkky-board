package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

/**
 * Affiche [content] sans défilement : s'il est plus haut que la place disponible, il est réduit de façon
 * homothétique (même facteur en largeur et en hauteur), centré horizontalement et calé en haut.
 * La réduction passe par un calque de rendu : les zones tactiles et l'accessibilité suivent la mise à l'échelle.
 */
@Composable
fun FitToScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val child = measurables.single().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else child.height
        val scale = if (child.height > maxHeight) maxHeight.toFloat() / child.height else 1f
        val width = constraints.constrainWidth(child.width)
        val height = constraints.constrainHeight((child.height * scale).toInt())
        layout(width, height) {
            child.placeWithLayer(x = (width - child.width) / 2, y = 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
        }
    }
}
