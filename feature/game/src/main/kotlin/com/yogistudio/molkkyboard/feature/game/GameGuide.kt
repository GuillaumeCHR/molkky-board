package com.yogistudio.molkkyboard.feature.game

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.res.stringResource
import com.yogistudio.molkkyboard.core.designsystem.component.GuideAnchors
import com.yogistudio.molkkyboard.core.designsystem.component.GuideBubble
import com.yogistudio.molkkyboard.core.model.InputMode
import kotlin.math.abs

// Prise en main et gestes de l'écran de jeu.

/** Bulles de la première partie guidée ; elles ne bloquent jamais la saisie. */
@Composable
internal fun GameGuide(state: GameUiState, anchors: GuideAnchors, onDone: () -> Unit) {
    val step = state.guide ?: return
    if (state.finished || state.abandoned) return
    val (text, anchor) =
        when (step) {
            GuideStep.PINS -> {
                stringResource(if (state.inputMode == InputMode.PINS) R.string.guide_pins else R.string.guide_quick) to
                    GUIDE_INPUT
            }

            GuideStep.VALIDATE -> {
                stringResource(R.string.guide_validate) to GUIDE_VALIDATE
            }

            GuideStep.UNDO -> {
                stringResource(R.string.guide_undo) to GUIDE_UNDO
            }
        }
    GuideBubble(
        text = text,
        step = step.number,
        steps = GuideStep.COUNT,
        // Les quilles occupent le centre : la bulle se pose au-dessus de la grille, sans en cacher aucune.
        anchor = anchors.of(anchor)?.let { if (step == GuideStep.PINS) it.copy(bottom = it.top) else it },
        onSkip = onDone,
        onDone = if (step == GuideStep.UNDO) onDone else null,
        above = step == GuideStep.PINS,
    )
}

internal const val GUIDE_INPUT = "input"
internal const val GUIDE_VALIDATE = "validate"
internal const val GUIDE_UNDO = "undo"

/** Balise de test du bandeau des scores. */
internal const val SCORES_STRIP_TAG = "scores_strip"

/**
 * Appelle [onSwipe] dès qu'un doigt se déplace verticalement de plus de [threshold] (plus que
 * horizontalement). Le geste est alors consommé : la puce touchée ne reçoit pas de clic en plus.
 */
internal suspend fun PointerInputScope.detectVerticalSwipe(threshold: Float, onSwipe: () -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var tracking = true
        while (tracking) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
            val dy = (change?.position?.y ?: down.position.y) - down.position.y
            val dx = (change?.position?.x ?: down.position.x) - down.position.x
            val swiped = change != null && abs(dy) > threshold && abs(dy) > abs(dx)
            if (swiped) {
                change.consume()
                onSwipe()
            }
            tracking = change != null && change.pressed && !swiped
        }
    }
}
