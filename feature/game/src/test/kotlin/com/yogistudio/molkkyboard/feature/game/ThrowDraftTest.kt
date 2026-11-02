package com.yogistudio.molkkyboard.feature.game

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
        assertEquals(ThrowResult.Points(12, PointsKind.SCORE), quick.withQuick(12).result)
        assertEquals(ThrowResult.Pins(emptyList()), quick.withQuick(0).result)
    }

    @Test
    fun `saisie rapide, les points sont saisis directement, 1 compris`() {
        assertEquals(ThrowResult.Points(1, PointsKind.SCORE), ThrowDraft(InputMode.QUICK).withQuick(1).result)
        assertEquals(ThrowResult.Points(5, PointsKind.SCORE), ThrowDraft(InputMode.QUICK).withQuick(5).result)
    }

    @Test
    fun `un ancien lancer en nombre de quilles se corrige en saisie directe`() {
        val draft = ThrowDraft.of(ThrowResult.Points(5, PointsKind.PIN_COUNT))
        assertEquals(InputMode.QUICK, draft.mode)
        assertEquals(5, draft.quick)
        assertEquals(ThrowResult.Points(7, PointsKind.SCORE), draft.withQuick(7).result)
    }
}
