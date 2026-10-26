package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.R
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme

/**
 * Pastille discrète « Tour 3 ». Au changement de tour, le numéro bascule avec un ressort (le nouveau monte,
 * l'ancien s'en va par le haut) ; simple fondu si « Réduire les animations » est actif.
 */
@Composable
fun RoundBadge(
    round: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.ds_round, round)
    val reduced = MolkkyBoardTheme.reducedMotion
    Surface(
        shape = CircleShape,
        color = MolkkyBoardTheme.colors.saffron.copy(alpha = BADGE_ALPHA),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        AnimatedContent(
            targetState = round,
            transitionSpec = {
                val slide = spring<IntOffset>(dampingRatio = Spring.DampingRatioMediumBouncy)
                if (reduced) {
                    fadeIn() togetherWith fadeOut()
                } else {
                    (slideInVertically(slide) { it } + fadeIn()) togetherWith
                        (slideOutVertically(slide) { -it } + fadeOut())
                }.using(SizeTransform(clip = true))
            },
            label = "round",
        ) { value ->
            Text(
                stringResource(R.string.ds_round, value),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

private const val BADGE_ALPHA = 0.28f
