package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import com.yogistudio.molkkyboard.core.designsystem.theme.MBMotion
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/**
 * Nombre qui défile comme un compteur : chaque chiffre glisse vers le haut quand la valeur monte, vers le bas
 * quand elle descend (retour à 25). Chiffres tabulaires : la largeur ne varie pas.
 */
@Composable
fun RollingNumber(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    var previous by remember { mutableIntStateOf(value) }
    val goingUp = value >= previous
    previous = value
    val reduced = MolkkyBoardTheme.reducedMotion
    val digits = value.toString()
    Row(modifier) {
        digits.forEachIndexed { index, digit ->
            // Clé par position depuis la droite : les unités restent les unités quand on passe de 9 à 10.
            val position = digits.length - index
            AnimatedContent(
                targetState = digit,
                transitionSpec = {
                    if (reduced) {
                        (slideInVertically(snap()) { 0 }).togetherWith(slideOutVertically(snap()) { 0 })
                    } else {
                        val direction = if (goingUp) 1 else -1
                        slideInVertically(MBMotion.bouncy<IntOffset>()) { direction * it }
                            .togetherWith(slideOutVertically(MBMotion.gentle<IntOffset>()) { -direction * it })
                    }.using(SizeTransform(clip = true))
                },
                label = "digit$position",
            ) { char ->
                Text(text = char.toString(), style = style, color = color)
            }
        }
    }
}

/**
 * [RollingNumber] réduit si besoin pour tenir dans la place disponible : un score à deux chiffres n'est
 * jamais coupé, même avec une police système agrandie (facteur 2,0). La taille est calculée sur « 88 »
 * (au moins deux chiffres) pour ne pas changer quand le score passe de 9 à 10.
 */
@Composable
fun FitRollingNumber(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        val sample = "8".repeat(maxOf(2, value.toString().length))
        val measured = measurer.measure(sample, style)
        val maxWidth = constraints.maxWidth.toFloat()
        val maxHeight = constraints.maxHeight.toFloat()
        val factor = minOf(1f, maxWidth / measured.size.width, maxHeight / measured.size.height)
        val fitted =
            if (factor < 1f) {
                with(density) {
                    style.copy(
                        fontSize = (style.fontSize.toPx() * factor).toSp(),
                        lineHeight = (style.lineHeight.toPx() * factor).toSp(),
                    )
                }
            } else {
                style
            }
        RollingNumber(value, fitted, color = color)
    }
}
