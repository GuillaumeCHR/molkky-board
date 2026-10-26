package com.yogistudio.molkkyboard.feature.setup

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yogistudio.molkkyboard.core.designsystem.theme.MBDimens

/**
 * Colonne réordonnable par glisser-déposer : on fait glisser la poignée ([DragHandle]) d'une ligne. Pour
 * TalkBack, chaque ligne propose aussi les actions « monter » et « descendre ».
 *
 * @param content contenu d'une ligne ; `handle` est le modificateur à poser sur la poignée.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    label: (T) -> String,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = MBDimens.SmallGap,
    content: @Composable (index: Int, item: T, handle: Modifier) -> Unit,
) {
    var dragged by remember { mutableStateOf<Any?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<Any, Int>() }
    val currentItems by rememberUpdatedState(items)
    val currentMove by rememberUpdatedState(onMove)
    val gap = with(LocalDensity.current) { spacing.toPx() }
    val up = stringResource(R.string.setup_move_up)
    val down = stringResource(R.string.setup_move_down)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        items.forEachIndexed { index, item ->
            val itemKey = key(item)
            key(itemKey) {
                val isDragged = dragged == itemKey
                val handle =
                    Modifier.pointerInput(itemKey) {
                        detectDragGestures(
                            onDragStart = {
                                dragged = itemKey
                                offset = 0f
                            },
                            onDragEnd = {
                                dragged = null
                                offset = 0f
                            },
                            onDragCancel = {
                                dragged = null
                                offset = 0f
                            },
                        ) { change, amount ->
                            change.consume()
                            offset += amount.y
                            val list = currentItems
                            val i = list.indexOfFirst { key(it) == itemKey }
                            // On échange avec la voisine dès que la ligne a franchi la moitié de sa hauteur.
                            if (offset > 0 && i < list.lastIndex) {
                                val next = heights[key(list[i + 1])] ?: return@detectDragGestures
                                if (offset > next / 2f) {
                                    currentMove(i, i + 1)
                                    offset -= next + gap
                                }
                            } else if (offset < 0 && i > 0) {
                                val previous = heights[key(list[i - 1])] ?: return@detectDragGestures
                                if (-offset > previous / 2f) {
                                    currentMove(i, i - 1)
                                    offset += previous + gap
                                }
                            }
                        }
                    }
                val name = label(item)
                Box(
                    Modifier
                        .onSizeChanged { heights[itemKey] = it.height }
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragged) offset else 0f
                            val lift = if (isDragged) DRAG_SCALE else 1f
                            scaleX = lift
                            scaleY = lift
                        }.semantics {
                            customActions =
                                listOfNotNull(
                                    CustomAccessibilityAction("$up : $name") {
                                        currentMove(index, index - 1)
                                        true
                                    }.takeIf { index > 0 },
                                    CustomAccessibilityAction("$down : $name") {
                                        currentMove(index, index + 1)
                                        true
                                    }.takeIf { index < items.lastIndex },
                                )
                        },
                ) {
                    content(index, item, handle)
                }
            }
        }
    }
}

/** Poignée de glisser-déposer « ≡ », assez grande pour le pouce. */
@Composable
fun DragHandle(modifier: Modifier) {
    Box(modifier.size(MBDimens.MinTouch, 48.dp), contentAlignment = Alignment.Center) {
        Text("≡", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private const val DRAG_SCALE = 1.03f
