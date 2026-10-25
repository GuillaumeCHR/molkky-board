package com.yogistudio.molkkyboard.core.designsystem.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Formation de départ officielle, vue du lanceur (rangée du fond en haut). */
val PIN_FORMATION: List<List<Int>> = listOf(listOf(7, 9, 8), listOf(5, 11, 12, 6), listOf(3, 10, 4), listOf(1, 2))

/** Hauteur d'une quille par rapport à sa largeur, et décalage vertical entre deux rangées (en hauteur de quille). */
private const val PIN_RATIO = 1.3f
private const val ROW_STEP = 0.62f
private const val COLUMN_STEP = 1.08f
private val MIN_PIN = 44.dp
private val MAX_PIN = 92.dp

/**
 * Les 12 quilles dans la formation officielle. La taille des quilles s'adapte à la place disponible (largeur et
 * hauteur) : la grille tient toujours sans défilement, avec des cibles tactiles aussi grandes que possible.
 *
 * @param highlighted quilles mises en avant (coach de fin de partie).
 */
@Composable
fun PinGrid(
    knocked: Set<Int>,
    onToggle: (pin: Int, knocked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlighted: Set<Int> = emptySet(),
) {
    BoxWithConstraints(modifier) {
        val rows = PIN_FORMATION.size
        val widest = PIN_FORMATION.maxOf { it.size }
        val byWidth = maxWidth / (1 + (widest - 1) * COLUMN_STEP)
        val byHeight = maxHeight / (PIN_RATIO * (1 + (rows - 1) * ROW_STEP))
        val pin: Dp = minOf(byWidth, byHeight).coerceIn(MIN_PIN, MAX_PIN)
        Layout(
            content = {
                PIN_FORMATION.flatten().forEach { number ->
                    PinView(
                        number = number,
                        knocked = number in knocked,
                        onToggle = { onToggle(number, it) },
                        enabled = enabled,
                        highlighted = number in highlighted,
                        size = pin,
                    )
                }
            },
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
            val pinWidth = pin.roundToPx()
            val pinHeight = (pin * PIN_RATIO).roundToPx()
            val stepX = (pinWidth * COLUMN_STEP).toInt()
            val stepY = (pinHeight * ROW_STEP).toInt()
            val width = pinWidth + (widest - 1) * stepX
            val height = pinHeight + (rows - 1) * stepY
            layout(width, height) {
                var index = 0
                PIN_FORMATION.forEachIndexed { row, pins ->
                    val rowWidth = pinWidth + (pins.size - 1) * stepX
                    pins.indices.forEach { col ->
                        placeables[index++].place((width - rowWidth) / 2 + col * stepX, row * stepY)
                    }
                }
            }
        }
    }
}
