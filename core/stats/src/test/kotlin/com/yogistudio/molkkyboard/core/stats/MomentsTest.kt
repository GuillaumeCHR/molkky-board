package com.yogistudio.molkkyboard.core.stats

import com.yogistudio.molkkyboard.core.model.GameConfig
import com.yogistudio.molkkyboard.core.model.PlayerId
import com.yogistudio.molkkyboard.core.model.TeamId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// Cas `M` de `docs/STATS_SPEC.md` § 8.
class MomentsTest {
    private val ab = listOf(T("A"), T("B"))
    private val a = TeamId("A")
    private val b = TeamId("B")

    @Test
    fun `M1 partie parfaite - record, parfaite, victoire et quatre 12, dans l'ordre de force`() {
        val found = moments(game("g", 1, ab, perfectLog()), 0, newRecord = true)
        assertEquals(Moment.Record(a, 5), found[0])
        assertEquals(Moment.Perfect(a), found[1])
        assertEquals(Moment.Victory(a, 5, 5), found[2])
        assertEquals(4, found.count { it is Moment.Twelve })
        assertEquals(Moment.Twelve(a, PlayerId("A"), strike = false), found.first { it is Moment.Twelve })
    }

    @Test
    fun `M2 strike des 12 quilles, plus fort qu'une victoire`() {
        val log = listOf(pins(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        val found = moments(game("g", 1, ab, log), 0)
        assertEquals(listOf(Moment.Twelve(a, PlayerId("A"), strike = true)), found)
        assertTrue(Moment.STRIKE > Moment.VICTORY)
    }

    @Test
    fun `M3 un 12 qui fait depasser la cible n'est pas un moment 12, mais un retour depuis 49 l'est`() {
        // A monte à 49 (4 × 12 + 1), puis fait 12 : retour à 25.
        val log = firstWins(12, 12, 12, 12, 1, 12)
        val found = moments(game("g", 1, ab, log), 0)
        assertEquals(4, found.count { it is Moment.Twelve })
        assertEquals(Moment.ResetFrom49(a, PlayerId("A")), found.single { it is Moment.ResetFrom49 })
    }

    @Test
    fun `M4 remontee - le vainqueur a ete mene de 15 points ou plus`() {
        // B mène 36 à 1 avant que A ne revienne : A gagne.
        val log =
            listOf(pins(1), pins(12), pins(1), pins(12), pins(1), pins(12)) +
                listOf(pins(12), pins(1), pins(12), pins(1), pins(12), pins(1), pins(11))
        val found = moments(game("g", 1, ab, log), 0)
        val comeback = found.single { it is Moment.Comeback } as Moment.Comeback
        assertEquals(a, comeback.team)
        assertTrue(comeback.deficit >= Moment.COMEBACK_DEFICIT)
    }

    @Test
    fun `M5 elimination apres trois rates`() {
        val log = listOf(pins(), pins(5), pins(), pins(5), pins())
        val found = moments(game("g", 1, ab, log), 0)
        assertEquals(Moment.Eliminated(a), found.first { it is Moment.Eliminated })
        // Il ne reste que B : il gagne.
        assertEquals(b, (found.first { it is Moment.Victory } as Moment.Victory).team)
    }

    @Test
    fun `M6 mode Ludique - pas de partie parfaite, la victoire reste`() {
        val found = moments(game("g", 1, ab, perfectLog(), config = GameConfig.Playful), 0)
        assertTrue(found.none { it is Moment.Perfect })
        assertTrue(found.any { it is Moment.Victory })
    }

    @Test
    fun `M7 manche inconnue - aucun moment`() {
        assertTrue(moments(game("g", 1, ab, perfectLog()), 3).isEmpty())
    }
}
