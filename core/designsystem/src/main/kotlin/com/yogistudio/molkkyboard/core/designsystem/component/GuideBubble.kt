package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yogistudio.molkkyboard.core.designsystem.R
import com.yogistudio.molkkyboard.core.designsystem.theme.MolkkyBoardTheme
import kotlin.math.roundToInt

/** Positions des éléments que la prise en main désigne, relevées pendant la mise en page. */
@Stable
class GuideAnchors {
    internal val bounds = mutableStateMapOf<String, Rect>()

    fun of(key: String): Rect? = bounds[key]
}

@Composable
fun rememberGuideAnchors(): GuideAnchors = remember { GuideAnchors() }

/** Marque cet élément comme cible possible d'une bulle de prise en main. */
fun Modifier.guideAnchor(anchors: GuideAnchors?, key: String): Modifier =
    if (anchors == null) this else onGloballyPositioned { anchors.bounds[key] = it.boundsInRoot() }

/**
 * Bulle de prise en main (première partie guidée), posée au-dessus ou au-dessous de [anchor] avec une pointe
 * vers lui. Elle ne capte que ses propres boutons : le reste de l'écran reste utilisable, la saisie n'est
 * jamais bloquée. [onDone] (« Compris ») est absent quand l'étape avance d'elle-même par l'action demandée.
 */
@Composable
fun GuideBubble(
    text: String,
    step: Int,
    steps: Int,
    anchor: Rect?,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null,
    /** Force la bulle au-dessus de la cible (par exemple pour ne pas masquer les quilles). */
    above: Boolean = false,
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    val gap = with(LocalDensity.current) { GAP.roundToPx() }
    val padding = with(LocalDensity.current) { PADDING.toPx() }
    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        val target = anchor?.translate(-origin)
        val screenHeight = constraints.maxHeight
        // Au-dessous de la cible si elle est dans la moitié haute, au-dessus sinon ; sans cible, au tiers.
        val below = !above && (target == null || target.center.y < screenHeight / 2f)
        Layout(
            content = {
                AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(spring(), initialScale = INITIAL_SCALE)) {
                    Column(Modifier.padding(horizontal = PADDING)) {
                        val x = target?.center?.x?.minus(padding)
                        if (target != null && below) Pointer(x, up = true)
                        Bubble(text, step, steps, onSkip, onDone)
                        if (target != null && !below) Pointer(x, up = false)
                    }
                }
            },
        ) { measurables, constraints ->
            val placeable = measurables.first().measure(constraints.copy(minHeight = 0))
            layout(constraints.maxWidth, constraints.maxHeight) {
                val y =
                    when {
                        target == null -> constraints.maxHeight / THIRD
                        below -> target.bottom.roundToInt() + gap
                        else -> target.top.roundToInt() - gap - placeable.height
                    }
                placeable.place(0, y.coerceIn(0, (constraints.maxHeight - placeable.height).coerceAtLeast(0)))
            }
        }
    }
}

@Composable
private fun Bubble(text: String, step: Int, steps: Int, onSkip: () -> Unit, onDone: (() -> Unit)?) {
    // Pas de Surface : elle intercepterait les taps. Un simple fond laisse passer les touches vers ce qui est
    // dessous (quilles, boutons) ; seuls « Passer » et « Compris » réagissent.
    val shape = MaterialTheme.shapes.medium
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape)
            .background(MaterialTheme.colorScheme.primary, shape)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
            Column(Modifier.padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 4.dp)) {
                Text(stringResource(R.string.ds_guide_step, step, steps), style = MaterialTheme.typography.labelMedium)
                Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.ds_guide_skip), color = MaterialTheme.colorScheme.onPrimary)
                    }
                    if (onDone != null) {
                        TextButton(onClick = onDone) {
                            Text(stringResource(R.string.ds_guide_done), color = MolkkyBoardTheme.colors.saffron)
                        }
                    }
                }
            }
        }
    }
}

/** Pointe de la bulle, dirigée vers le centre horizontal [x] de la cible (relatif à la colonne). */
@Composable
private fun Pointer(x: Float?, up: Boolean) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(POINTER)) {
        val half = size.height
        val cx = (x ?: (size.width / 2)).coerceIn(half * 2, size.width - half * 2)
        val path =
            Path().apply {
                if (up) {
                    moveTo(cx - half, size.height)
                    lineTo(cx, 0f)
                    lineTo(cx + half, size.height)
                } else {
                    moveTo(cx - half, 0f)
                    lineTo(cx, size.height)
                    lineTo(cx + half, 0f)
                }
                close()
            }
        drawPath(path, color)
    }
}

private val GAP = 4.dp
private val POINTER = 12.dp
private val PADDING = 20.dp
private const val THIRD = 3
private const val INITIAL_SCALE = 0.85f
