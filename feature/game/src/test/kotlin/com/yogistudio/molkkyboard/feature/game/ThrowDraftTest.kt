package com.yogistudio.molkkyboard.feature.game

import com.yogistudio.molkkyboard.core.designsystem.component.QuickPadMode
import com.yogistudio.molkkyboard.core.model.InputMode
import com.yogistudio.molkkyboard.core.scoring.PointsKind
import com.yogistudio.molkkyboard.core.scoring.ThrowResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Saisie de l'éditeur du déroulé : mêmes règles que l'écran de jeu. */
class ThrowDraftTest {
    @Test
    fun `l'editeur part du lancer enregistre`() {
        assertEquals(ThrowResult.Pins(listOf(4, 12)), ThrowDraft.of(ThrowResult.Pins(listOf(12, 4))).result)
        assertEquals(ThrowResult.Pins(emptyList()), ThrowDraft.of(ThrowResult.Pins(emptyList())).result)
        val quick = ThrowDraft.of(ThrowResult.Points(7, PointsKind.PIN_COUNT))
        assertEquals(InputMode.QUICK, quick.mode)
        assertEquals(ThrowResult.Points(7, PointsKind.PIN_COUNT), quick.result)
        assertNull(ThrowDraft.of(null).result)
    }

    @Test
    fun `rate, quilles et pave rapide`() {
        val draft = ThrowDraft.of(ThrowResult.Pins(listOf(3)))
        assertEquals(ThrowResult.Pins(emptyList()), draft.toggleMiss().result)
        assertEquals(ThrowResult.Pins(listOf(3, 9)), draft.toggleMiss().withPin(9, true).result)
        assertNull(draft.withPin(3, false).result)
        val quick = draft.withMode(InputMode.QUICK)
        assertNull(quick.result)
        assertEquals(ThrowResult.Points(12, PointsKind.SINGLE_PIN), quick.withQuick(12).result)
        assertEquals(ThrowResult.Pins(emptyList()), quick.withQuick(0).result)
    }

    @Test
    fun `une seule quille ne se saisit pas en nombre de quilles`() {
        val draft = ThrowDraft(InputMode.QUICK).withQuick(1)
        assertNull(draft.withQuickMode(QuickPadMode.PinCount).result)
        assertEquals(
            ThrowResult.Points(5, PointsKind.PIN_COUNT),
            draft.withQuickMode(QuickPadMode.PinCount).withQuick(5).result,
        )
    }
}
